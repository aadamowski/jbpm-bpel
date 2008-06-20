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
 * Metadata related to an XML Schema type, simple or complex.
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/01/22 00:24:55 $
 */
public class SchemaType extends XmlType {

  private static final long serialVersionUID = 1L;

  SchemaType() {
  }

  public SchemaType(QName name) {
    setName(name);
  }

  protected Element createElement(VariableDefinition definition) {
    // schema type: pick an arbitrary name (our choice is the variable name)
    return XmlUtil.createElement(definition.getName());
  }
}
