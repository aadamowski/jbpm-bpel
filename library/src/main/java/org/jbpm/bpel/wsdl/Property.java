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

import javax.wsdl.extensions.ExtensibilityElement;
import javax.xml.namespace.QName;

/**
 * Creates a unique name and associates it with an XML Schema type. The intent
 * is to introduce a name that has semantic significance beyond the type itself.
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/01/22 00:24:55 $
 */
public interface Property extends ExtensibilityElement, Serializable {

  /**
   * Gets the name of this property.
   */
  public QName getQName();

  /**
   * Sets the name of this property.
   */
  public void setQName(QName name);

  /**
   * Gets the type of this property.
   */
  public QName getType();

  /**
   * Sets the type of this property.
   */
  public void setType(QName type);

  /**
   * Tells whether this property is completely defined or merely a placeholder.
   * The value of the <tt>undefined</tt> flag is relevant during
   * serialization:
   * <ul>
   * <li>The deserializer sets the flag to <code>false</code> when it reads a
   * <tt>bpel:property</tt> element from the input document.</li>
   * <li>The serializer checks the flag is set to <code>false</code> before
   * writing the element to the output stream.</li>
   * </ul>
   * @return <code>false</code> if this is completely defined,
   *         <code>true</code> if this is a placeholder
   */
  public boolean isUndefined();

  /**
   * Sets whether this property is completely defined or merely a placeholder.
   * @param undefined <code>false</code> marks this completely defined,
   *        <code>true</code> marks this a placeholder
   */
  public void setUndefined(boolean undefined);
}