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

import javax.xml.namespace.QName;

import org.jbpm.bpel.wsdl.Property;
import org.jbpm.bpel.wsdl.xml.WsdlConstants;

/**
 * A property definition creates a globally unique name and associates it with
 * an XML Schema simple type. The intent is not to create a new type, but to
 * create a name that has greater significance than the type itself.
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2006/10/29 06:13:41 $
 */
public class PropertyImpl extends NamedExtension implements Property {

  private QName type;
  private boolean undefined = true;

  private static final long serialVersionUID = 1L;

  /**
   * Constructs a property and sets the element type.
   */
  public PropertyImpl() {
    setElementType(WsdlConstants.Q_PROPERTY);
  }

  public QName getType() {
    return type;
  }

  public void setType(QName type) {
    this.type = type;
  }

  public boolean isUndefined() {
    return undefined;
  }

  public void setUndefined(boolean undefined) {
    this.undefined = undefined;
  }
}
