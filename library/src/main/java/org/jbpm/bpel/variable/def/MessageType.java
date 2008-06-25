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

import java.util.Iterator;
import java.util.Map.Entry;

import javax.wsdl.Message;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.jbpm.bpel.graph.exe.BpelFaultException;
import org.jbpm.bpel.sublang.def.PropertyQuery;
import org.jbpm.bpel.variable.exe.MessageValue;
import org.jbpm.bpel.wsdl.PropertyAlias;
import org.jbpm.bpel.xml.BpelConstants;

/**
 * Metadata related to a WSDL message type.
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/09/12 23:20:21 $
 */
public class MessageType extends VariableType {

  private Message message;

  private static final long serialVersionUID = 1L;

  MessageType() {
  }

  public MessageType(Message message) {
    setMessage(message);
  }

  public QName getName() {
    return getMessage().getQName();
  }

  public Message getMessage() {
    return message;
  }

  public void setMessage(Message message) {
    this.message = message;
  }

  public Object createValue(VariableDefinition definition) {
    return new MessageValue(this);
  }

  public boolean isInitialized(Object variableValue) {
    return ((MessageValue) variableValue).isInitialized();
  }

  public void setValue(Object currentValue, Object newValue) {
    // message variables are only assignable from message values
    if (!(newValue instanceof MessageValue)) {
      throw new BpelFaultException(BpelConstants.FAULT_MISMATCHED_ASSIGNMENT);
    }
    // further, the message value must be defined by the same wsdl definition
    MessageValue newMessageValue = (MessageValue) newValue;
    if (!getName().equals(newMessageValue.getType().getName())) {
      throw new BpelFaultException(BpelConstants.FAULT_MISMATCHED_ASSIGNMENT);
    }
    // perform partwise assignment
    MessageValue curMessageValue = (MessageValue) currentValue;
    // first drop the current parts
    curMessageValue.getParts().clear();
    // next copy the new parts
    Iterator partEntryIt = newMessageValue.getParts().entrySet().iterator();
    while (partEntryIt.hasNext()) {
      Entry partEntry = (Entry) partEntryIt.next();
      curMessageValue.setPart((String) partEntry.getKey(), partEntry.getValue());
    }
  }

  protected Object evaluateProperty(PropertyAlias propertyAlias,
      Object variableValue) {
    // get the part, fail if it does not exist
    Element part = ((MessageValue) variableValue).getPart(propertyAlias.getPart());
    PropertyQuery query = propertyAlias.getQuery();
    return query != null ?
    // evaluate the query on the given part
    query.getEvaluator().evaluate(part)
        // return the bare part
        : part;
  }

  protected void assignProperty(PropertyAlias propertyAlias,
      Object variableValue, Object propertyValue) {
    String partName = propertyAlias.getPart();
    PropertyQuery query = propertyAlias.getQuery();
    MessageValue messageValue = (MessageValue) variableValue;
    if (query == null) {
      // assign to the part
      messageValue.setPart(partName, propertyValue);
    }
    else {
      // retrieve the part
      Element partForAssign = messageValue.getPartForAssign(partName);
      // assign to the location identified by the query
      query.getEvaluator().assign(partForAssign, propertyValue);
    }
  }

  public boolean isMessage() {
    return true;
  }
}
