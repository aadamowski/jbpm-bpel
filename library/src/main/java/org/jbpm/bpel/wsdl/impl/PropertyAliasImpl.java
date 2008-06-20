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

import javax.wsdl.Message;
import javax.xml.namespace.QName;

import org.jbpm.bpel.sublang.def.PropertyQuery;
import org.jbpm.bpel.wsdl.Property;
import org.jbpm.bpel.wsdl.PropertyAlias;
import org.jbpm.bpel.wsdl.xml.WsdlConstants;

/**
 * A property alias maps a global property to a location in a specific message
 * part. The property name becomes an alias for the message part and location.
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/09/12 23:20:20 $
 */
public class PropertyAliasImpl extends AbstractExtension implements
    PropertyAlias {

  private Property property;
  private Message message;
  private String part;
  private QName type;
  private QName element;
  private PropertyQuery query;

  private static final long serialVersionUID = 1L;

  /**
   * Constructs a property alias and sets its element type.
   */
  public PropertyAliasImpl() {
    setElementType(WsdlConstants.Q_PROPERTY_ALIAS);
  }

  public Property getProperty() {
    return property;
  }

  public void setProperty(Property property) {
    this.property = property;
  }

  public Message getMessage() {
    return message;
  }

  public void setMessage(Message message) {
    this.message = message;
  }

  public String getPart() {
    return part;
  }

  public void setPart(String part) {
    this.part = part;
  }

  public QName getType() {
    return type;
  }

  public void setType(QName type) {
    this.type = type;
  }

  public QName getElement() {
    return element;
  }

  public void setElement(QName element) {
    this.element = element;
  }

  public PropertyQuery getQuery() {
    return query;
  }

  public void setQuery(PropertyQuery query) {
    this.query = query;
  }
}
