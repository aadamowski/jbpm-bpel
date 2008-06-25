/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the JBPM BPEL PUBLIC LICENSE AGREEMENT as
 * published by JBoss Inc.; either version 1.0 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package org.jbpm.bpel.tutorial.task;

import java.rmi.RemoteException;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.namespace.QName;
import javax.xml.rpc.Service;
import javax.xml.rpc.ServiceException;
import javax.xml.rpc.Stub;
import javax.xml.rpc.server.ServiceLifecycle;
import javax.xml.rpc.server.ServletEndpointContext;
import javax.xml.rpc.soap.SOAPFaultException;
import javax.xml.soap.SOAPConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.bpel.tutorial.wsa.EndpointReferenceType;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.exe.TaskInstance;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2008/06/12 08:30:54 $
 */
public class TaskManager_Impl implements TaskManager, ServiceLifecycle {

  private Service taskCallbackService;
  private JbpmConfiguration jbpmConfiguration;

  /** Initialization parameter for the jBPM configuration resource. */
  public static final String JBPM_CONFIG_RESOURCE_PARAM = "JbpmCfgResource";

  private static final String REPLY_ADDRESS_VARIABLE = "replyAddress";
  private static final QName SERVER_CODE = new QName(SOAPConstants.URI_NS_SOAP_ENVELOPE, "Server");

  private static final Log log = LogFactory.getLog(TaskManager_Impl.class);

  public long createTask(TaskInfo taskInfo, EndpointReferenceType replyTo) throws RemoteException {
    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
    try {
      /*
       * Loading the token, even for read only, leads Hibernate to update the version column at the
       * end of the transaction, causing a stale state exception on the process side. To cope with
       * this situation, this method can either (1) ignore a second invocation when the process
       * detects the stale state and retries the operation or (2) avoid loading the token and set
       * the token column with a plain SQL statement
       */
      TaskInstance taskInstance = createTaskInstance2(taskInfo, replyTo, jbpmContext);
      return taskInstance.getId();
    }
    catch (RuntimeException e) {
      jbpmContext.setRollbackOnly();
      throw e;
    }
    finally {
      jbpmContext.close();
    }
  }

  private TaskInstance createTaskInstance1(TaskInfo taskInfo, EndpointReferenceType replyTo,
      JbpmContext jbpmContext) {
    String name = taskInfo.getName();
    Long tokenId = taskInfo.getTokenId();
    String actorId = taskInfo.getActorId();

    log.debug("looking for existing task instances with name="
        + name
        + ", tokenId="
        + tokenId
        + " and actorId="
        + actorId);

    List duplicates = jbpmContext.getSession().createCriteria(TaskInstance.class).add(
        Restrictions.eq("name", name)).add(Restrictions.eq("actorId", actorId)).createAlias(
        "token", "t").add(Restrictions.eq("t.id", tokenId)).list();

    if (!duplicates.isEmpty()) {
      jbpmContext.setRollbackOnly();

      TaskInstance duplicate = (TaskInstance) duplicates.get(0);
      log.debug("found "
          + duplicates.size()
          + " task instances, selected instance with id="
          + duplicate.getId());
      return duplicate;
    }

    // materialize token
    Token token = jbpmContext.loadToken(tokenId.longValue());

    // create task instance for token
    TaskInstance taskInstance = token.getProcessInstance()
        .getTaskMgmtInstance()
        .createTaskInstance(token);

    // set task instance properties
    taskInstance.setName(name);
    taskInstance.setActorId(actorId);

    // save return address
    taskInstance.setVariable(REPLY_ADDRESS_VARIABLE, replyTo.getAddress().toString());
    jbpmContext.save(taskInstance);

    log.debug("found no task instances, created instance with id=" + taskInstance.getId());
    return taskInstance;
  }

  private TaskInstance createTaskInstance2(TaskInfo taskInfo, EndpointReferenceType replyTo,
      JbpmContext jbpmContext) {
    // TODO use task instance factory? requires execution context...
    TaskInstance taskInstance = new TaskInstance();

    // fire task creation event
    taskInstance.create();

    // set task instance properties
    taskInstance.setName(taskInfo.getName());
    taskInstance.setActorId(taskInfo.getActorId());

    // preserve return address
    taskInstance.setVariable(REPLY_ADDRESS_VARIABLE, replyTo.getAddress().toString());

    // save task instance
    Session hbSession = jbpmContext.getSession();
    hbSession.save(taskInstance);

    // associate task instance with token via SQL statement
    // FIXME isn't there a better way?
    hbSession.createSQLQuery("UPDATE JBPM_TASKINSTANCE SET TOKEN_ = :token WHERE ID_ = :id")
        .setLong("token", taskInfo.getTokenId().longValue())
        .setLong("id", taskInstance.getId())
        .executeUpdate();

    return taskInstance;
  }

  public void endTask(TaskInfo taskInfo) {
    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
    try {
      // end task instance
      TaskInstance taskInstance = jbpmContext.loadTaskInstance(taskInfo.getTaskId().longValue());
      taskInstance.end();

      // acquire endpoint proxy
      TaskCallback taskCallback = (TaskCallback) taskCallbackService.getPort(TaskCallback.class);

      // configure callback address
      String address = (String) taskInstance.getVariable(REPLY_ADDRESS_VARIABLE);
      Stub taskCallbackStub = (Stub) taskCallback;
      taskCallbackStub._setProperty(Stub.ENDPOINT_ADDRESS_PROPERTY, address);

      log.debug("calling back endpoint at: " + address);
      taskCallback.taskEnded(taskInfo);
    }
    catch (ServiceException e) {
      log.error("could not get callback endpoint proxy", e);
      jbpmContext.setRollbackOnly();
      throw new SOAPFaultException(SERVER_CODE, "task callback failed", null, null);
    }
    catch (RemoteException e) {
      log.error("endpoint callback failed", e);
      jbpmContext.setRollbackOnly();
      throw new SOAPFaultException(SERVER_CODE, "task callback failed", null, null);
    }
    catch (RuntimeException e) {
      jbpmContext.setRollbackOnly();
      throw e;
    }
    finally {
      jbpmContext.close();
    }

  }

  public TaskList getTaskList(String actorId) {
    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
    try {
      List taskInstances = jbpmContext.getTaskList(actorId);
      int taskCount = taskInstances.size();
      TaskInfo[] taskInfos = new TaskInfo[taskCount];

      for (int i = 0; i < taskCount; i++) {
        TaskInstance taskInstance = (TaskInstance) taskInstances.get(i);

        TaskInfo taskInfo = new TaskInfo();
        taskInfo.setTaskId(new Long(taskInstance.getId()));
        taskInfo.setName(taskInstance.getName());
        taskInfo.setActorId(taskInstance.getActorId());
        taskInfo.setTokenId(new Long(taskInstance.getToken().getId()));

        taskInfos[i] = taskInfo;
      }

      return new TaskList(taskInfos);
    }
    finally {
      jbpmContext.close();
    }
  }

  public void init(Object context) throws ServiceException {
    // jbpm configuration
    ServletEndpointContext endpointContext = (ServletEndpointContext) context;
    String configResource = endpointContext.getServletContext().getInitParameter(
        JBPM_CONFIG_RESOURCE_PARAM);
    jbpmConfiguration = JbpmConfiguration.getInstance(configResource);

    // task callback service
    try {
      Context initialContext = new InitialContext();
      taskCallbackService = (Service) initialContext.lookup("java:comp/env/service/TaskCallback");
      initialContext.close();
    }
    catch (NamingException e) {
      throw new ServiceException("could not retrieve task callback service", e);
    }
  }

  public void destroy() {
    taskCallbackService = null;
  }
}
