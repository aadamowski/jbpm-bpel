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
package org.jbpm.bpel.variable.exe;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.wsdl.Part;
import javax.xml.namespace.QName;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;

import org.jbpm.bpel.BpelException;
import org.jbpm.bpel.graph.exe.BpelFaultException;
import org.jbpm.bpel.variable.def.MessageType;
import org.jbpm.bpel.variable.def.VariableDefinition;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.bpel.xml.util.XmlUtil;

/**
 * Holds together the {@linkplain MessageType type metadata} and the part values of a WSDL message
 * {@linkplain VariableDefinition variable}.
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/07/22 05:57:25 $
 */
public class MessageValue {

  long id;
  private MessageType type;
  private Map parts = new HashMap();

  private static final Log log = LogFactory.getLog(MessageValue.class);

  MessageValue() {
  }

  public MessageValue(MessageType type) {
    this.type = type;
  }

  public MessageType getType() {
    return type;
  }

  public void setType(MessageType type) {
    this.type = type;
  }

  public Element getPart(String partName) {
    // get the part value
    Element part = (Element) getParts().get(partName);
    if (part == null) {
      // verify the wsdl message defines the part
      Part wsdlPart = type.getMessage().getPart(partName);
      if (wsdlPart == null)
        throw new BpelException("undefined part: " + partName);

      // uninitialized part
      log.error("uninitialized part: " + partName);
      throw new BpelFaultException(BpelConstants.FAULT_UNINITIALIZED_VARIABLE);
    }
    return part;
  }

  public Element getPartForAssign(String partName) {
    Map parts = getParts();
    Element part = (Element) parts.get(partName);
    if (part == null) {
      // verify the wsdl message defines the part
      Part wsdlPart = type.getMessage().getPart(partName);
      if (wsdlPart == null)
        throw new BpelException("undefined part: " + partName);

      // determine the qualified name of the part element
      QName elementName = wsdlPart.getElementName();
      if (elementName != null) {
        // element part: use the element name
        part = XmlUtil.createElement(elementName);
      }
      else {
        // typed part: pick an arbitrary name (our choice is the part name)
        part = XmlUtil.createElement(partName);
      }
      // add the new part
      parts.put(partName, part);
    }
    return part;
  }

  public void setPart(String partName, Object value) {
    Element part = getPartForAssign(partName);
    XmlUtil.setObjectValue(part, value);
  }

  public Map getParts() {
    return parts;
  }

  public void setParts(Map newParts) {
    // ensure the parts map is empty
    parts.clear();

    // make a shallow copy of the parts
    Map wsdlParts = type.getMessage().getParts();

    for (Iterator i = newParts.entrySet().iterator(); i.hasNext();) {
      Map.Entry partEntry = (Map.Entry) i.next();
      Object partName = partEntry.getKey();

      // ensure the part is defined
      if (!wsdlParts.containsKey(partName))
        throw new BpelException("undefined part: " + partName);

      parts.put(partName, partEntry.getValue());
    }
  }

  public boolean isInitialized() {
    // either one or more parts have a value, or the wsdl message has no parts
    return !parts.isEmpty() || type.getMessage().getParts().isEmpty();
  }

  public String toString() {
    return new ToStringBuilder(this).append("type", type).append("parts", parts).toString();
  }
}
