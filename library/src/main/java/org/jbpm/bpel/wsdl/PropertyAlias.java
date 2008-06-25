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
package org.jbpm.bpel.wsdl;

import java.io.Serializable;

import javax.wsdl.Message;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.xml.namespace.QName;

import org.jbpm.bpel.graph.basic.Assign;
import org.jbpm.bpel.sublang.def.Expression;
import org.jbpm.bpel.sublang.def.PropertyQuery;

/**
 * Maps a {@linkplain Property property} to a field in a specific
 * {@linkplain Message message} part or variable value. The property name
 * becomes an alias for the message part and/or location, and can be used as
 * such in {@linkplain Expression expressions} and
 * {@linkplain Assign assignments}.
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/09/12 23:20:19 $
 */
public interface PropertyAlias extends ExtensibilityElement, Serializable {

  /**
   * Gets the aliasing property.
   */
  public Property getProperty();

  /**
   * Sets the aliasing property.
   */
  public void setProperty(Property property);

  /**
   * Gets the aliased WSDL message.
   */
  public Message getMessage();

  /**
   * Sets the aliased WSDL message
   */
  public void setMessage(Message message);

  /**
   * Gets the name of the aliased message part.
   */
  public String getPart();

  /**
   * Sets the name of the aliased message part.
   */
  public void setPart(String part);

  /**
   * Gets the name of the aliased XML Schema type.
   */
  public QName getType();

  /**
   * Sets the name of the aliased XML Schema type.
   */
  public void setType(QName type);

  /**
   * Gets the name of the aliased XML Schema element.
   */
  public QName getElement();

  /**
   * Sets the name of the aliased XML Schema element.
   */
  public void setElement(QName element);

  /**
   * Gets the aliased location.
   */
  public PropertyQuery getQuery();

  /**
   * Sets the aliased location.
   */
  public void setQuery(PropertyQuery query);
}