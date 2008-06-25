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

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.jbpm.bpel.xml.util.XmlUtil;

/**
 * Metadata related to an XML Schema element.
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/01/22 00:24:55 $
 */
public class ElementType extends XmlType {

  private static final long serialVersionUID = 1L;

  ElementType() {
  }

  public ElementType(QName name) {
    setName(name);
  }

  protected Element createElement(VariableDefinition definition) {
    // element type: use the element name
    return XmlUtil.createElement(getName());
  }

  public boolean isElement() {
    return true;
  }
}
