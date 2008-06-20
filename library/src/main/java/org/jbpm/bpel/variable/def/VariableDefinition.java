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
package org.jbpm.bpel.variable.def;

import java.io.Serializable;

import javax.xml.namespace.QName;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.bpel.graph.exe.BpelFaultException;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.exe.Token;

/**
 * Variables hold messages that are exchanged, as well as intermediate data used in business logic
 * and in composing messages sent to partners.
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2008/02/01 05:43:09 $
 */
public class VariableDefinition implements Serializable {

  long id;
  private String name;
  private VariableType type;

  private static final long serialVersionUID = 1L;

  private static final Log log = LogFactory.getLog(VariableDefinition.class);

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public VariableType getType() {
    return type;
  }

  public void setType(VariableType type) {
    this.type = type;
  }

  /**
   * Creates an instance of this variable in the scope of the given token.
   * @param token the scope token
   */
  public void createInstance(Token token) {
    Object value = type.createValue(this);
    token.getProcessInstance().getContextInstance().createVariable(name, value, token);
  }

  /**
   * Creates an instance of this variable in the scope of the given token.
   * @param token the scope token
   * @param value the initial value
   */
  public void createInstance(Token token, Object value) {
    token.getProcessInstance().getContextInstance().createVariable(name, value, token);
  }

  /**
   * Gets the value of this variable in the scope of the given token.
   * @param token the scope token
   * @return the variable value
   */
  public Object getValue(Token token) {
    Object value = getValueForAssign(token);
    if (!type.isInitialized(value)) {
      log.error("uninitialized variable: " + name);
      throw new BpelFaultException(BpelConstants.FAULT_UNINITIALIZED_VARIABLE);
    }
    return value;
  }

  public Object getValueForAssign(Token token) {
    ContextInstance context = token.getProcessInstance().getContextInstance();
    return context.getVariable(name, token);
  }

  /**
   * Sets the value of this variable in the scope of the given token.
   * @param token the scope token
   * @param newValue the new value
   */
  public void setValue(Token token, Object newValue) {
    Object currentValue = getValueForAssign(token);
    type.setValue(currentValue, newValue);
  }

  public Object getPropertyValue(QName propertyName, Token token) {
    return type.getPropertyValue(propertyName, getValue(token));
  }

  public void setPropertyValue(QName propertyName, Token token, Object propertyValue) {
    type.setPropertyValue(propertyName, getValue(token), propertyValue);
  }

  public String toString() {
    return new ToStringBuilder(this).append("name", name).append("type", type).toString();
  }
}
