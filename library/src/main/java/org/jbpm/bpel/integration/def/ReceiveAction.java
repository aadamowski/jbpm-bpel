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
package org.jbpm.bpel.integration.def;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;

import org.jbpm.JbpmContext;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.integration.IntegrationService;
import org.jbpm.bpel.variable.def.VariableDefinition;
import org.jbpm.bpel.variable.exe.MessageValue;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/07/26 00:39:10 $
 */
public class ReceiveAction extends MessageAction implements Serializable {

  private String messageExchange;

  private VariableDefinition variable;
  private Correlations correlations;

  private InboundMessageActivity inboundMessageActivity;

  private static final long serialVersionUID = 1L;

  public String getMessageExchange() {
    return messageExchange;
  }

  public void setMessageExchange(String messageExchange) {
    this.messageExchange = messageExchange;
  }

  public VariableDefinition getVariable() {
    return variable;
  }

  public void setVariable(VariableDefinition variable) {
    this.variable = variable;
  }

  public Correlations getCorrelations() {
    return correlations;
  }

  public void setCorrelations(Correlations correlations) {
    this.correlations = correlations;
  }

  public InboundMessageActivity getInboundMessageActivity() {
    return inboundMessageActivity;
  }

  public void setInboundMessageActivity(InboundMessageActivity inboundMessageListener) {
    this.inboundMessageActivity = inboundMessageListener;
  }

  public void execute(ExecutionContext exeContext) {
    IntegrationService integrationService = ReceiveAction.getIntegrationService(exeContext.getJbpmContext());
    integrationService.receive(this, exeContext.getToken(), true);
  }

  public Token initializeProcessInstance(ProcessInstance processInstance) {
    BpelProcessDefinition processDefinition = (BpelProcessDefinition) processInstance.getProcessDefinition();
    ProcessInstanceStarter starter = new ProcessInstanceStarter(this,
        processInstance.getRootToken());
    starter.visit(processDefinition);
    return starter.getReceivingToken();
  }

  public void deliverMessage(Token token, Map parts) {
    MessageValue messageValue = (MessageValue) variable.getValueForAssign(token);
    // save the received parts
    messageValue.setParts(parts);
    // ensure the correlation constraint
    if (correlations != null)
      correlations.ensureConstraint(messageValue, token);
    // pass control to inbound message activity
    inboundMessageActivity.messageReceived(this, token);
  }
  
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this).append("partnerLink",
        getPartnerLink().getName()).append("operation", getOperation().getName());

    if (messageExchange != null)
      builder.append("messageExchange", messageExchange);

    return builder.toString();
  }

  public static IntegrationService getIntegrationService(JbpmContext jbpmContext) {
    return (IntegrationService) jbpmContext.getServices().getService(
        IntegrationService.SERVICE_NAME);
  }
}
