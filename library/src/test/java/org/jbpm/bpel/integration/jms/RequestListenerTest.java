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

import javax.jms.JMSException;

import org.jbpm.bpel.graph.exe.ScopeInstance;
import org.jbpm.bpel.integration.exe.CorrelationSetInstance;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/08/08 11:20:32 $
 */
public class RequestListenerTest extends AbstractListenerTestCase {

  private Token token;

  protected void setUp() throws Exception {
    super.setUp();
    // create process instance
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    // initialize global data
    ScopeInstance scopeInstance = processDefinition.getGlobalScope().createInstance(
        processInstance.getRootToken());
    scopeInstance.initializeData();
    // commit changes
    jbpmContext.save(processInstance);
    newTransaction();
    // reassociate the process instance with the new session
    token = processInstance.getRootToken();
    token.setNode((Node) receiveAction.getInboundMessageActivity());
    // initiate correlation set
    CorrelationSetInstance csi = receiveAction.getCorrelations()
        .getCorrelation("csId")
        .getSet()
        .getInstance(token);
    csi.initialize(Collections.singletonMap(ID_PROP, ID_VALUE));
  }

  protected void openListener() throws JMSException {
    RequestListener requestListener = new RequestListener(receiveAction, token, integrationControl,
        jmsSession);
    newTransaction();
    requestListener.open();
  }

  protected void closeListener() throws JMSException {
    // we created a one-shot request listener, no need to close it
  }
}