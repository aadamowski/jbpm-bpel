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
package org.jbpm.bpel.integration.exe;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.commons.lang.builder.ToStringBuilder;

import org.jbpm.bpel.graph.exe.BpelFaultException;
import org.jbpm.bpel.integration.def.CorrelationSetDefinition;
import org.jbpm.bpel.variable.def.VariableType;
import org.jbpm.bpel.variable.exe.MessageValue;
import org.jbpm.bpel.wsdl.Property;
import org.jbpm.bpel.xml.BpelConstants;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2006/09/27 03:53:01 $
 */
public class CorrelationSetInstance {

  long id;
  private CorrelationSetDefinition definition;
  private Map properties;

  private static final long serialVersionUID = 1L;

  public CorrelationSetInstance() {
  }

  public CorrelationSetDefinition getDefinition() {
    return definition;
  }

  public void setDefinition(CorrelationSetDefinition definition) {
    this.definition = definition;
  }

  public boolean isInitialized() {
    return properties != null && properties.size() > 0;
  }

  public void initialize(Map propertyValues) {
    checkNotInitialized();
    properties = new HashMap();
    Iterator propertyIter = definition.getProperties().iterator();
    while (propertyIter.hasNext()) {
      Property property = (Property) propertyIter.next();
      QName name = property.getQName();
      properties.put(name, propertyValues.get(name));
    }
  }

  public void initialize(MessageValue messageValue) {
    // make sure the correlation set is not initialized more than once
    checkNotInitialized();
    properties = new HashMap();
    // extract the property values
    VariableType messageType = messageValue.getType();
    Iterator propertyIter = definition.getProperties().iterator();
    while (propertyIter.hasNext()) {
      Property property = (Property) propertyIter.next();
      QName propertyName = property.getQName();
      Object propertyValue = messageType.getPropertyValue(propertyName,
          messageValue);
      properties.put(propertyName, propertyValue);
    }
  }

  public Object getProperty(QName propertyName) {
    checkInitialized();
    return properties.get(propertyName);
  }

  public Map getProperties() {
    checkInitialized();
    return properties;
  }

  public void validateConstraint(MessageValue messageValue) {
    checkInitialized();
    // compare the property values
    VariableType messageType = messageValue.getType();
    Iterator entryIt = properties.entrySet().iterator();
    while (entryIt.hasNext()) {
      Map.Entry nameValueEntry = (Map.Entry) entryIt.next();
      QName propertyName = (QName) nameValueEntry.getKey();
      Object propertyValue = nameValueEntry.getValue();
      // compare the value in the correlation set with the value in the message
      if (!propertyValue.equals(messageType.getPropertyValue(propertyName,
          messageValue))) {
        // property value mismatch
        throw new BpelFaultException(BpelConstants.FAULT_CORRELATION_VIOLATION);
      }
    }
  }

  private void checkInitialized() {
    if (!isInitialized()) {
      // correlation set not initiated
      throw new BpelFaultException(BpelConstants.FAULT_CORRELATION_VIOLATION);
    }
  }

  private void checkNotInitialized() {
    if (isInitialized()) {
      // correlation set already initiated
      throw new BpelFaultException(BpelConstants.FAULT_CORRELATION_VIOLATION);
    }
  }

  public String toString() {
    return new ToStringBuilder(this).append("name", definition.getName())
        .append("id", id)
        .toString();
  }
}
