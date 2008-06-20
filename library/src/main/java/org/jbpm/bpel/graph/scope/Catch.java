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
package org.jbpm.bpel.graph.scope;

import javax.xml.namespace.QName;

import org.apache.commons.lang.builder.ToStringBuilder;

import org.jbpm.bpel.graph.exe.FaultInstance;
import org.jbpm.bpel.variable.def.VariableDefinition;
import org.jbpm.bpel.variable.exe.MessageValue;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;

/**
 * Fault handlers attached to a scope provide a way to define custom error
 * recovery activities.
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/03/16 00:04:38 $
 */
public class Catch extends Handler {

  private QName faultName;
  private VariableDefinition faultVariable;

  private static final long serialVersionUID = 1L;

  public void execute(ExecutionContext exeContext) {
    if (faultVariable != null) {
      // retrieve variable definition from enclosing scope
      VariableDefinition scopeVariable = getCompositeActivity().findVariable(
          faultVariable.getName());

      if (faultVariable.equals(scopeVariable)) {
        // set variable in enclosing scope
        setFaultVariable(exeContext.getToken());
      }
      else {
        // initialize local variable
        initFaultVariable(exeContext.getToken());
      }
    }
    super.execute(exeContext);
  }

  // CompositeActivity override
  // //////////////////////////////////////////////////////////

  public VariableDefinition findVariable(String name) {
    return faultVariable != null && faultVariable.getName().equals(name) ? faultVariable
        : super.findVariable(name);
  }

  protected void initFaultVariable(Token token) {
    // retrieve thrown fault from scope instance
    FaultInstance faultInstance = Scope.getInstance(token).getFaultInstance();
    MessageValue messageData = faultInstance.getMessageValue();

    Object faultData;
    if (messageData != null) {
      if (messageData.getType().equals(faultVariable.getType())) {
        // message data / message variable
        faultData = messageData;
      }
      else {
        // message data / element variable
        faultData = messageData.getParts().values().iterator().next();
      }
    }
    else {
      // element data / element variable
      faultData = faultInstance.getElementValue();
    }
    
    // initialize variable with fault data
    faultVariable.createInstance(token, faultData);
  }

  protected void setFaultVariable(Token token) {
    FaultInstance faultInstance = Scope.getInstance(token).getFaultInstance();
    faultVariable.setValue(token, faultInstance.getMessageValue());
  }

  // fault handler properties
  // ////////////////////////////////////////////////////////////

  public QName getFaultName() {
    return faultName;
  }

  public void setFaultName(QName faultName) {
    this.faultName = faultName;
  }

  public VariableDefinition getFaultVariable() {
    return faultVariable;
  }

  public void setFaultVariable(VariableDefinition faultVariable) {
    this.faultVariable = faultVariable;
  }

  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);

    if (faultName != null)
      builder.append("name", faultName);

    if (faultVariable != null)
      builder.append("variable", faultVariable);

    return builder.toString();
  }
}