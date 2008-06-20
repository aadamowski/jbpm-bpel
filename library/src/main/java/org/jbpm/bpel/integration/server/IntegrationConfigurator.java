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
package org.jbpm.bpel.integration.server;

import javax.jms.JMSException;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.bpel.BpelException;
import org.jbpm.bpel.deploy.DeploymentDescriptor;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.integration.jms.IntegrationControl;
import org.jbpm.bpel.integration.jms.JmsIntegrationServiceFactory;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/10/17 22:02:51 $
 */
public class IntegrationConfigurator implements ServletContextListener {

  /** Initialization parameter for the jBPM configuration resource. */
  public static final String JBPM_CONFIG_RESOURCE_PARAM = "JbpmCfgResource";

  private static final Log log = LogFactory.getLog(IntegrationConfigurator.class);

  public void contextInitialized(ServletContextEvent event) {
    ServletContext servletContext = event.getServletContext();

    String configResource = servletContext.getInitParameter(JBPM_CONFIG_RESOURCE_PARAM);
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.getInstance(configResource);

    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
    try {
      // read the app descriptor from a classpath resource
      DeploymentDescriptor deploymentDescriptor = DeploymentDescriptor.readDeploymentDescriptor(jbpmContext);
      // obtain the integration control for the process definition referenced in the descriptor
      BpelProcessDefinition processDefinition = deploymentDescriptor.findProcessDefinition(jbpmContext);
      JmsIntegrationServiceFactory integrationServiceFactory = JmsIntegrationServiceFactory.getConfigurationInstance(jbpmConfiguration);
      IntegrationControl integrationControl = integrationServiceFactory.getIntegrationControl(processDefinition);

      // make app descriptor available to message activities
      integrationControl.setDeploymentDescriptor(deploymentDescriptor);
      // start receiving requests
      integrationControl.enableInboundMessageActivities(jbpmContext);

      // make integration control available to jax-rpc handlers
      servletContext.setAttribute(SoapHandler.INTEGRATION_CONTROL_ATTR, integrationControl);

      log.info("message reception enabled for process: " + deploymentDescriptor.getName());
    }
    catch (RuntimeException e) {
      jbpmContext.setRollbackOnly();
      throw e;
    }
    catch (NamingException e) {
      jbpmContext.setRollbackOnly();
      throw new BpelException("could not start bpel application", e);
    }
    catch (JMSException e) {
      jbpmContext.setRollbackOnly();
      throw new BpelException("could not start bpel application", e);
    }
    finally {
      jbpmContext.close();
    }
  }

  public void contextDestroyed(ServletContextEvent event) {
    IntegrationControl integrationControl = (IntegrationControl) event.getServletContext()
        .getAttribute(SoapHandler.INTEGRATION_CONTROL_ATTR);
    // ensure integration control is bound to servlet context, as initialization may have failed
    if (integrationControl != null) {
      try {
        integrationControl.disableInboundMessageActivities();
        log.info("message reception disabled for process: "
            + integrationControl.getDeploymentDescriptor().getName());
      }
      catch (JMSException e) {
        log.error("could not stop bpel application", e);
      }
    }
  }
}
