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

import javax.xml.namespace.QName;

import org.apache.commons.lang.builder.ToStringBuilder;

import org.jbpm.bpel.integration.IntegrationService;
import org.jbpm.bpel.variable.def.VariableDefinition;
import org.jbpm.bpel.variable.exe.MessageValue;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/02/19 18:56:18 $
 */
public class ReplyAction extends MessageAction implements Serializable {

  private static final long serialVersionUID = 1L;

  private String messageExchange;

  private VariableDefinition variable;
  private Correlations correlations;

  private QName faultName;

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

  public QName getFaultName() {
    return faultName;
  }

  public void setFaultName(QName faultName) {
    this.faultName = faultName;
  }

  public void execute(ExecutionContext exeContext) {
    IntegrationService integrationService = ReceiveAction.getIntegrationService(exeContext.getJbpmContext());
    integrationService.reply(this, exeContext.getToken());
    exeContext.leaveNode();
  }

  public Map writeMessage(Token token) {
    MessageValue messageValue = (MessageValue) variable.getValue(token);
    // ensure the correlation constraint
    if (correlations != null)
      correlations.ensureConstraint(messageValue, token);
    // extract the message to reply with
    return messageValue.getParts();
  }

  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this).append("partnerLink",
        getPartnerLink().getName()).append("operation",
        getOperation().getName());

    if (messageExchange != null)
      builder.append("messageExchange", messageExchange);

    return builder.toString();
  }
}
