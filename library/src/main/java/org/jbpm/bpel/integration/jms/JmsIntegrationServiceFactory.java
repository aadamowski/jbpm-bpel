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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.JbpmConfiguration;
import org.jbpm.bpel.BpelException;
import org.jbpm.bpel.integration.IntegrationService;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.svc.Service;
import org.jbpm.svc.ServiceFactory;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2008/06/12 08:18:54 $
 */
public class JmsIntegrationServiceFactory implements ServiceFactory {

  // injected objects, see jbpm.cfg.xml
  private JbpmConfiguration jbpmConfiguration;
  private String connectionFactoryName = "java:comp/env/jms/JbpmConnectionFactory";
  private String requestDestinationName = "java:comp/env/jms/JbpmRequestQueue";
  private String responseDestinationName = "java:comp/env/jms/JbpmResponseQueue";

  private ConnectionFactory connectionFactory;
  private Destination requestDestination;
  private Destination responseDestination;

  private Map integrationControls = new HashMap();

  private static final long serialVersionUID = 1L;
  private static final Log log = LogFactory.getLog(JmsIntegrationServiceFactory.class);

  public Service openService() {
    return new JmsIntegrationService(this);
  }

  public void close() {
    for (Iterator i = integrationControls.values().iterator(); i.hasNext();) {
      IntegrationControl integrationControl = (IntegrationControl) i.next();
      try {
        integrationControl.disableInboundMessageActivities();
      }
      catch (JMSException e) {
        log.warn("could not disable inbound message activities", e);
      }
    }
  }

  public JbpmConfiguration getJbpmConfiguration() {
    return jbpmConfiguration;
  }

  public ConnectionFactory getConnectionFactory() {
    if (connectionFactory == null && connectionFactoryName != null) {
      try {
        connectionFactory = (ConnectionFactory) lookup(connectionFactoryName);
      }
      catch (NamingException e) {
        log.warn("could not retrieve connection factory", e);
      }
    }
    return connectionFactory;
  }

  public Destination getRequestDestination() {
    if (requestDestination == null && requestDestinationName != null) {
      try {
        requestDestination = (Destination) lookup(requestDestinationName);
      }
      catch (NamingException e) {
        log.warn("could not retrieve request destination", e);
      }
    }
    return requestDestination;
  }

  public Destination getResponseDestination() {
    if (responseDestination == null) {
      if (responseDestinationName == null) {
        throw new IllegalStateException(
            "response destination name not specified in jbpm configuration");
      }
      try {
        responseDestination = (Destination) lookup(responseDestinationName);
      }
      catch (NamingException e) {
        throw new BpelException("could not retrieve response destination", e);
      }
    }
    return responseDestination;
  }

  private static Object lookup(String name) throws NamingException {
    InitialContext initialContext = new InitialContext();
    try {
      return initialContext.lookup(name);
    }
    finally {
      initialContext.close();
    }
  }

  public IntegrationControl getIntegrationControl(ProcessDefinition processDefinition) {
    Long processId = new Long(processDefinition.getId());
    synchronized (integrationControls) {
      IntegrationControl integrationControl = (IntegrationControl) integrationControls.get(processId);
      if (integrationControl == null) {
        log.debug("creating integration control: processDefinition=" + processDefinition);
        integrationControl = new IntegrationControl(this);
        integrationControls.put(processId, integrationControl);
      }
      return integrationControl;
    }
  }

  public static JmsIntegrationServiceFactory getConfigurationInstance(
      JbpmConfiguration jbpmConfiguration) {
    return (JmsIntegrationServiceFactory) jbpmConfiguration.getServiceFactory(IntegrationService.SERVICE_NAME);
  }
}
