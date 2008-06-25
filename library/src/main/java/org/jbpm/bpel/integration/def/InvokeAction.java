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

import org.jbpm.bpel.integration.IntegrationService;
import org.jbpm.bpel.variable.def.VariableDefinition;
import org.jbpm.bpel.variable.exe.MessageValue;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;

/**
 * @author Juan Cantu
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/07/26 00:36:13 $
 */
public class InvokeAction extends MessageAction implements Serializable {

  private VariableDefinition inputVariable;
  private VariableDefinition outputVariable;
  private Correlations requestCorrelations;
  private Correlations responseCorrelations;

  private static final long serialVersionUID = 1L;

  public VariableDefinition getInputVariable() {
    return inputVariable;
  }

  public void setInputVariable(VariableDefinition inputVariable) {
    this.inputVariable = inputVariable;
  }

  public VariableDefinition getOutputVariable() {
    return outputVariable;
  }

  public void setOutputVariable(VariableDefinition outputVariable) {
    this.outputVariable = outputVariable;
  }

  public Correlations getRequestCorrelations() {
    return requestCorrelations;
  }

  public void setRequestCorrelations(Correlations correlations) {
    this.requestCorrelations = correlations;
  }

  public Correlations getResponseCorrelations() {
    return responseCorrelations;
  }

  public void setResponseCorrelations(Correlations outCorrelations) {
    this.responseCorrelations = outCorrelations;
  }
  
  public void execute(ExecutionContext exeContext) {
    IntegrationService integrationService = ReceiveAction.getIntegrationService(exeContext.getJbpmContext());
    integrationService.invoke(this, exeContext.getToken());
    exeContext.leaveNode();
  }

  public Map writeMessage(Token token) {
    // get the input variable instance
    MessageValue messageValue = (MessageValue) inputVariable.getValue(token);
    // ensure the *outgoing* correlation constraint
    if (responseCorrelations != null)
      responseCorrelations.ensureConstraint(messageValue, token);
    // extract the outgoing data
    return messageValue.getParts();
  }

  public void readMessage(Token token, Map outputParts) {
    MessageValue messageValue = (MessageValue) outputVariable.getValueForAssign(token);
    // save the incoming data
    messageValue.setParts(outputParts);
    // ensure the *incoming* correlation constraint
    if (requestCorrelations != null)
      requestCorrelations.ensureConstraint(messageValue, token);
  }

  public String toString() {
    return new ToStringBuilder(this)
        .append("partnerLink", getPartnerLink().getName())
        .append("operation", getOperation().getName())
        .toString();
  }
}