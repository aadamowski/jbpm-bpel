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
package org.jbpm.bpel.integration.jms;

import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.wsdl.Operation;
import javax.wsdl.Port;
import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.JbpmContext;
import org.jbpm.bpel.BpelException;
import org.jbpm.bpel.deploy.DeploymentDescriptor;
import org.jbpm.bpel.deploy.ScopeMatcher;
import org.jbpm.bpel.deploy.PartnerRoleDescriptor.InitiateMode;
import org.jbpm.bpel.endpointref.EndpointReference;
import org.jbpm.bpel.endpointref.EndpointReferenceFactory;
import org.jbpm.bpel.graph.basic.Receive;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.exe.BpelFaultException;
import org.jbpm.bpel.graph.scope.OnEvent;
import org.jbpm.bpel.graph.scope.Scope;
import org.jbpm.bpel.graph.struct.Pick;
import org.jbpm.bpel.graph.struct.StructuredActivity.Begin;
import org.jbpm.bpel.integration.catalog.DefinitionCatalog;
import org.jbpm.bpel.integration.catalog.ServiceCatalog;
import org.jbpm.bpel.integration.client.Caller;
import org.jbpm.bpel.integration.client.SoapCaller;
import org.jbpm.bpel.integration.def.InvokeAction;
import org.jbpm.bpel.integration.def.PartnerLinkDefinition;
import org.jbpm.bpel.integration.def.ReceiveAction;
import org.jbpm.bpel.integration.def.ReplyAction;
import org.jbpm.bpel.integration.exe.PartnerLinkInstance;
import org.jbpm.bpel.persistence.db.IntegrationSession;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.graph.exe.Token;
import org.jbpm.svc.Services;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2008/01/30 07:18:22 $
 */
public class IntegrationControl {

  private final JmsIntegrationServiceFactory integrationServiceFactory;

  private Connection jmsConnection;
  private DeploymentDescriptor deploymentDescriptor;

  private List partnerLinkEntries = Collections.EMPTY_LIST;

  private final List startListeners = new Vector();
  private final Map requestListeners = new Hashtable();
  private final Map outstandingRequests = new Hashtable();
  private final DefinitionCatalog myCatalog = new DefinitionCatalog();

  private final Map callers = new HashMap();

  /**
   * JNDI subcontext that contains JMS objects, relative to the initial context.
   */
  public static final String JMS_CONTEXT = "java:comp/env/jms";

  /** JNDI name bound to the JMS connection factory, relative to {@link #JMS_CONTEXT}. */
  public static final String CONNECTION_FACTORY_NAME = "JbpmConnectionFactory";

  private static final Log log = LogFactory.getLog(IntegrationControl.class);
  private static final long serialVersionUID = 1L;

  IntegrationControl(JmsIntegrationServiceFactory integrationServiceFactory) {
    this.integrationServiceFactory = integrationServiceFactory;
  }

  public JmsIntegrationServiceFactory getIntegrationServiceFactory() {
    return integrationServiceFactory;
  }

  public Connection getJmsConnection() {
    return jmsConnection;
  }

  public DeploymentDescriptor getDeploymentDescriptor() {
    return deploymentDescriptor;
  }

  public void setDeploymentDescriptor(DeploymentDescriptor deploymentDescriptor) {
    this.deploymentDescriptor = deploymentDescriptor;
  }

  public PartnerLinkEntry getPartnerLinkEntry(PartnerLinkDefinition partnerLink) {
    final long partnerLinkId = partnerLink.getId();
    for (int i = 0, n = partnerLinkEntries.size(); i < n; i++) {
      PartnerLinkEntry entry = (PartnerLinkEntry) partnerLinkEntries.get(i);
      if (entry.getId() == partnerLinkId)
        return entry;
    }
    return null;
  }

  public PartnerLinkEntry getPartnerLinkEntry(QName portTypeName, QName serviceName, String portName) {
    PartnerLinkEntry selectedEntry = null;

    for (int i = 0, n = partnerLinkEntries.size(); i < n; i++) {
      PartnerLinkEntry entry = (PartnerLinkEntry) partnerLinkEntries.get(i);
      EndpointReference myReference = entry.getMyReference();

      if (myReference == null)
        continue; // this entry corresponds to a partner link without myRole

      QName myPortTypeName = myReference.getPortTypeName();
      if (!portTypeName.equals(myPortTypeName))
        continue;

      QName myServiceName = myReference.getServiceName();
      if (myServiceName != null && !serviceName.equals(myServiceName))
        continue;

      String myPortName = myReference.getPortName();
      if (myPortName != null && !portName.equals(myPortName))
        continue;

      if (selectedEntry != null) {
        throw new BpelException("multiple partner link entries match the given arguments: service="
            + serviceName
            + ", port="
            + portName);
      }
      selectedEntry = entry;
    }
    return selectedEntry;
  }

  public Map getRequestListeners() {
    return requestListeners;
  }

  public void addRequestListener(RequestListener requestListener) {
    Object key = createKey(requestListener.getReceiveActionId(), requestListener.getTokenId());
    requestListeners.put(key, requestListener);
  }

  public RequestListener removeRequestListener(ReceiveAction receiveAction, Token token) {
    Object key = createKey(receiveAction.getId(), token.getId());
    return (RequestListener) requestListeners.remove(key);
  }

  public void removeRequestListener(RequestListener requestListener) {
    Object key = createKey(requestListener.getReceiveActionId(), requestListener.getTokenId());
    synchronized (requestListeners) {
      Object currentListener = requestListeners.get(key);
      if (requestListener == currentListener)
        requestListeners.remove(key);
    }
  }

  private static Object createKey(long receiveActionId, long tokenId) {
    return new RequestListener.Key(receiveActionId, tokenId);
  }

  public Map getOutstandingRequests() {
    return outstandingRequests;
  }

  public void addOutstandingRequest(ReceiveAction receiveAction, Token token,
      OutstandingRequest request) {
    Object key = createKey(receiveAction.getPartnerLink().getInstance(token),
        receiveAction.getOperation(), receiveAction.getMessageExchange());

    if (outstandingRequests.put(key, request) != null)
      throw new BpelFaultException(BpelConstants.FAULT_CONFLICTING_REQUEST);

    log.debug("added outstanding request: receiveAction="
        + receiveAction
        + ", token="
        + token
        + ", request="
        + request);
  }

  public OutstandingRequest removeOutstandingRequest(ReplyAction replyAction, Token token) {
    Object key = createKey(replyAction.getPartnerLink().getInstance(token),
        replyAction.getOperation(), replyAction.getMessageExchange());
    OutstandingRequest request = (OutstandingRequest) outstandingRequests.remove(key);
    if (request == null)
      throw new BpelFaultException(BpelConstants.FAULT_MISSING_REQUEST);

    log.debug("removed outstanding request: replyAction="
        + replyAction
        + ", token="
        + token
        + ", request="
        + request);
    return request;
  }

  private static Object createKey(PartnerLinkInstance partnerLinkInstance, Operation operation,
      String messageExchange) {
    return new OutstandingRequest.Key(getOrAssignId(partnerLinkInstance), operation.getName(),
        messageExchange);
  }

  private static long getOrAssignId(PartnerLinkInstance partnerLinkInstance) {
    long id = partnerLinkInstance.getId();
    // in case instance is transient, assign an identifier to it
    if (id == 0L) {
      Services.assignId(partnerLinkInstance);
      id = partnerLinkInstance.getId();
    }
    return id;
  }

  public Map getCallers() {
    return callers;
  }

  public Caller createCaller(InvokeAction invokeAction, Token token) {
    PartnerLinkDefinition partnerLinkDefinition = invokeAction.getPartnerLink();
    PartnerLinkInstance partnerLinkInstance = partnerLinkDefinition.getInstance(token);

    // retrieve partner reference
    EndpointReference partnerReference = partnerLinkInstance.getPartnerReference();
    if (partnerReference == null) {
      // create reference whose sole selection criterion is the port type
      partnerReference = createPartnerReference(partnerLinkDefinition);
      partnerLinkInstance.setPartnerReference(partnerReference);
    }

    // select a port from the service catalog with the criteria known at this point
    Port port = partnerReference.selectPort(getPartnerCatalog());
    log.debug("selected port " + port.getName() + " on " + invokeAction + " for " + token);

    // create a client for that port
    Caller caller = new SoapCaller(port);
    callers.put(new Caller.Key(invokeAction.getId(), token.getId()), caller);
    return caller;
  }

  EndpointReference createPartnerReference(PartnerLinkDefinition partnerLink) {
    PartnerLinkEntry entry = getPartnerLinkEntry(partnerLink);
    InitiateMode initiateMode = entry.getInitiateMode();

    EndpointReference partnerReference;
    if (InitiateMode.PULL.equals(initiateMode)) {
      EndpointReferenceFactory referenceFactory = EndpointReferenceFactory.getInstance(
          IntegrationConstants.DEFAULT_REFERENCE_NAME, null);
      partnerReference = referenceFactory.createEndpointReference();
    }
    else if (InitiateMode.STATIC.equals(initiateMode))
      partnerReference = entry.getPartnerReference();
    else
      throw new BpelFaultException(BpelConstants.FAULT_UNINITIALIZED_PARTNER_ROLE);

    return partnerReference;
  }

  public Caller removeCaller(InvokeAction invokeAction, Token token) {
    return (Caller) callers.remove(new Caller.Key(invokeAction.getId(), token.getId()));
  }

  public ServiceCatalog getPartnerCatalog() {
    return getDeploymentDescriptor().getServiceCatalog();
  }

  public List getStartListeners() {
    return startListeners;
  }

  public void addStartListener(StartListener startListener) {
    startListeners.add(startListener);
  }

  public void removeStartListener(StartListener startListener) {
    startListeners.remove(startListener);
  }

  public DefinitionCatalog getMyCatalog() {
    return myCatalog;
  }

  /**
   * Prepares inbound message activities annotated to create a process instance for receiving
   * requests.
   */
  public void enableInboundMessageActivities(JbpmContext jbpmContext) throws NamingException,
      JMSException {
    InitialContext initialContext = new InitialContext();
    try {
      // publish partner link information to JNDI
      BpelProcessDefinition processDefinition = getDeploymentDescriptor().findProcessDefinition(
          jbpmContext);
      buildPartnerLinkEntries(initialContext, processDefinition);

      // open a jms connection
      openJmsConnection(initialContext);

      try {
        // enable start IMAs
        StartListenersBuilder builder = new StartListenersBuilder(this);
        builder.visit(processDefinition);

        // note: upon creation, start listeners add themselves to this control
        if (startListeners.isEmpty())
          throw new BpelException(processDefinition + " has no start activities");

        // enable outstanding IMAs
        IntegrationSession integrationSession = IntegrationSession.getContextInstance(jbpmContext);
        JmsIntegrationService integrationService = JmsIntegrationService.get(jbpmContext);

        // receive
        for (Iterator i = integrationSession.findReceiveTokens(processDefinition).iterator(); i.hasNext();) {
          Token token = (Token) i.next();
          Receive receive = (Receive) token.getNode();
          integrationService.jmsReceive(receive.getReceiveAction(), token, this, true);
        }

        // pick
        for (Iterator i = integrationSession.findPickTokens(processDefinition).iterator(); i.hasNext();) {
          Token token = (Token) i.next();
          // pick points activity token to begin mark
          Begin begin = (Begin) token.getNode();
          Pick pick = (Pick) begin.getCompositeActivity();
          integrationService.jmsReceive(pick.getOnMessages(), token, this);
        }

        // event
        for (Iterator t = integrationSession.findEventTokens(processDefinition).iterator(); t.hasNext();) {
          Token token = (Token) t.next();
          // scope points events token to itself
          Scope scope = (Scope) token.getNode();
          List onEvents = scope.getOnEvents();
          for (int i = 0, n = onEvents.size(); i < n; i++) {
            OnEvent onEvent = (OnEvent) onEvents.get(i);
            integrationService.jmsReceive(onEvent.getReceiveAction(), token, this, false);
          }
        }

        // start message delivery
        jmsConnection.start();
      }
      catch (JMSException e) {
        jmsConnection.close();
        throw e;
      }
    }
    finally {
      initialContext.close();
    }
  }

  /**
   * Prevents inbound message activities annotated to create a process instance from further
   * receiving requests.
   */
  public void disableInboundMessageActivities() throws JMSException {
    // disable start IMAs
    synchronized (startListeners) {
      for (int i = 0, n = startListeners.size(); i < n; i++) {
        StartListener startListener = (StartListener) startListeners.get(i);
        startListener.close();
      }
      startListeners.clear();
    }

    // disable outstanding IMAs
    synchronized (requestListeners) {
      for (Iterator i = requestListeners.values().iterator(); i.hasNext();) {
        RequestListener requestListener = (RequestListener) i.next();
        requestListener.close();
      }
      requestListeners.clear();
    }

    // release jms connection
    closeJmsConnection();
  }

  void buildPartnerLinkEntries(InitialContext initialContext, BpelProcessDefinition process)
      throws NamingException {
    // match scopes with their descriptors
    ScopeMatcher scopeMatcher = new ScopeMatcher(process);
    scopeMatcher.visit(getDeploymentDescriptor());
    Map scopeDescriptors = scopeMatcher.getScopeDescriptors();
    // lookup destinations & bind port entries
    PartnerLinkEntriesBuilder builder = new PartnerLinkEntriesBuilder(scopeDescriptors,
        getJmsContext(initialContext), integrationServiceFactory.getRequestDestination());
    builder.visit(process);
    partnerLinkEntries = builder.getPartnerLinkEntries();
  }

  private static Context getJmsContext(InitialContext initialContext) {
    Context jmsContext;
    try {
      jmsContext = (Context) initialContext.lookup(JMS_CONTEXT);
      log.debug("retrieved jms context: " + JMS_CONTEXT);
    }
    catch (NamingException e) {
      log.debug("jms context not found: " + JMS_CONTEXT);
      jmsContext = initialContext;
      log.debug("fell back on initial context");
    }
    return jmsContext;
  }

  void openJmsConnection(InitialContext initialContext) throws NamingException, JMSException {
    ConnectionFactory jmsConnectionFactory = getConnectionFactory(initialContext);
    jmsConnection = jmsConnectionFactory.createConnection();
  }

  private ConnectionFactory getConnectionFactory(InitialContext initialContext)
      throws NamingException {
    Context jmsContext = getJmsContext(initialContext);
    ConnectionFactory jmsConnectionFactory;
    try {
      jmsConnectionFactory = (ConnectionFactory) jmsContext.lookup(CONNECTION_FACTORY_NAME);
      log.debug("retrieved jms connection factory: " + CONNECTION_FACTORY_NAME);
    }
    catch (NamingException e) {
      log.debug("jms connection factory not found: " + CONNECTION_FACTORY_NAME);
      jmsConnectionFactory = integrationServiceFactory.getConnectionFactory();
      if (jmsConnectionFactory == null)
        throw e;
      log.debug("fell back on default connection factory");
    }
    return jmsConnectionFactory;
  }

  void closeJmsConnection() throws JMSException {
    if (jmsConnection != null) {
      jmsConnection.close();
      jmsConnection = null;
    }
  }

  void reset() {
    deploymentDescriptor = null;

    partnerLinkEntries = Collections.EMPTY_LIST;

    startListeners.clear();
    requestListeners.clear();
    outstandingRequests.clear();

    callers.clear();
  }
}
