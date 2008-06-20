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

import javax.wsdl.extensions.ExtensibilityElement;
import javax.xml.namespace.QName;

/**
 * Basic implementation of a WSDL extensibility element.
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2006/08/21 01:05:59 $
 */
public abstract class AbstractExtension implements ExtensibilityElement,
    Serializable {

  long id;
  private Boolean required;
  private QName elementType;

  /**
   * Default constructor. Does nothing.
   */
  protected AbstractExtension() {
  }

  /**
   * Sets the type of this extensibility element.
   * @param elementType the qualified name for the type, possibly null
   */
  public void setElementType(QName elementType) {
    this.elementType = elementType;
  }

  /**
   * Gets the type of this extensibility element.
   * @return the qualified name for the type, possibly null
   */
  public QName getElementType() {
    return elementType;
  }

  /**
   * Sets whether the semantics of this extension are required. Relates to the
   * wsdl:required attribute.
   * @param required the required flag, possibly null.
   */
  public void setRequired(Boolean required) {
    this.required = required;
  }

  /**
   * Gets whether the semantics of this extension are required. Relates to the
   * wsdl:required attribute.
   * @return the required indicator, possibly null.
   */
  public Boolean getRequired() {
    return required;
  }
}
