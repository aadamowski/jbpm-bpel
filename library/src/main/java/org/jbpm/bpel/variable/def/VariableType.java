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
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.w3c.dom.Node;

import org.jbpm.bpel.graph.exe.BpelFaultException;
import org.jbpm.bpel.wsdl.PropertyAlias;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.bpel.xml.util.DatatypeUtil;
import org.jbpm.util.EqualsUtil;

/**
 * Common base for metadata related to a kind of variable declaration.
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/09/04 06:42:27 $
 */
public abstract class VariableType implements Serializable {

  long id;
  private Map propertyAliases = new HashMap();

  protected VariableType() {
  }

  public abstract QName getName();

  public void addPropertyAlias(PropertyAlias alias) {
    propertyAliases.put(alias.getProperty().getQName(), alias);
  }

  public PropertyAlias getPropertyAlias(QName propertyName) {
    return (PropertyAlias) propertyAliases.get(propertyName);
  }

  public Map getPropertyAliases() {
    return propertyAliases;
  }

  public abstract Object createValue(VariableDefinition definition);

  public abstract boolean isInitialized(Object variableValue);

  public abstract void setValue(Object currentValue, Object newValue);

  public Object getPropertyValue(QName propertyName, Object variableValue) {
    PropertyAlias alias = getPropertyAlias(propertyName);
    if (alias == null)
      throw new BpelFaultException(BpelConstants.FAULT_SELECTION_FAILURE);

    Object result = evaluateProperty(alias, variableValue);
    return result instanceof Node ? DatatypeUtil.toString(((Node) result)) : result;
  }

  public void setPropertyValue(QName propertyName, Object variableValue, Object propertyValue) {
    assignProperty(getPropertyAlias(propertyName), variableValue, propertyValue);
  }

  public boolean isMessage() {
    return false;
  }

  public boolean isElement() {
    return false;
  }

  public boolean equals(Object obj) {
    // XXX why is this required?
    return EqualsUtil.equals(this, obj);
  }

  protected abstract Object evaluateProperty(PropertyAlias propertyAlias, Object variableValue);

  protected abstract void assignProperty(PropertyAlias propertyAlias, Object variableValue,
      Object propertyValue);

  public String toString() {
    return new ToStringBuilder(this).append("name", getName()).toString();
  }
}
