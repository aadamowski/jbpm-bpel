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
package org.jbpm.bpel.wsdl.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.wsdl.Message;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.ibm.wsdl.Constants;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/04/11 06:37:35 $
 */
public abstract class OperationMember implements Serializable {

  long id;
  protected String name;
  protected Message message;
  protected Element documentationElement;

  protected Map extensionAttributes = new HashMap();
  protected List extensibilityElements = new ArrayList();

  private static final List MEMBER_ATTR_NAMES = Arrays.asList(new String[] {
      Constants.ATTR_NAME, Constants.ATTR_MESSAGE
  });

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Message getMessage() {
    return message;
  }

  public void setMessage(Message message) {
    this.message = message;
  }

  public Element getDocumentationElement() {
    return documentationElement;
  }

  public void setDocumentationElement(Element documentationElement) {
    this.documentationElement = documentationElement;
  }

  public Object getExtensionAttribute(QName name) {
    return extensionAttributes.get(name);
  }

  public void setExtensionAttribute(QName name, Object value) {
    if (value != null)
      extensionAttributes.put(name, value);
    else
      extensionAttributes.remove(name);
  }

  public Map getExtensionAttributes() {
    return extensionAttributes;
  }

  public List getNativeAttributeNames() {
    return MEMBER_ATTR_NAMES;
  }

  public List getExtensibilityElements() {
    return extensibilityElements;
  }

  public void addExtensibilityElement(ExtensibilityElement extension) {
    extensibilityElements.add(extension);
  }

  public ExtensibilityElement removeExtensibilityElement(
      ExtensibilityElement extension) {
    return extensibilityElements.remove(extension) ? extension : null;
  }
}
