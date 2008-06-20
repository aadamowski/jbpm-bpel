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
package org.jbpm.bpel.graph.basic;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.BpelVisitor;
import org.jbpm.bpel.graph.exe.FaultInstance;
import org.jbpm.bpel.graph.exe.ScopeInstance;
import org.jbpm.bpel.graph.scope.Scope;
import org.jbpm.bpel.variable.def.VariableDefinition;
import org.jbpm.bpel.variable.exe.MessageValue;
import org.jbpm.graph.exe.ExecutionContext;

/**
 * Generates a fault from inside the business process.
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/02/19 22:25:44 $
 */
public class Throw extends Activity {

  private static final long serialVersionUID = 1L;

  private QName faultName;
  private VariableDefinition faultVariable;

  public Throw() {
  }

  public Throw(String name) {
    super(name);
  }

  public void execute(ExecutionContext exeContext) {
    FaultInstance faultInstance = new FaultInstance(faultName);

    if (faultVariable != null) {
      Object value = faultVariable.getValue(exeContext.getToken());
      if (value instanceof MessageValue) {
        faultInstance.setMessageValue((MessageValue) value);
      }
      else if (value instanceof Element) {
        faultInstance.setElementValue((Element) value);
      }
    }

    ScopeInstance scopeInstance = Scope.getInstance(exeContext.getToken());
    scopeInstance.faulted(faultInstance);
  }

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

  public void accept(BpelVisitor visitor) {
    visitor.visit(this);
  }
}
