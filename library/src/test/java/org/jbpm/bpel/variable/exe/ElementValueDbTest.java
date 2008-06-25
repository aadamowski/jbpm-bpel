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

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.jbpm.bpel.graph.def.ImportDefinition;
import org.jbpm.bpel.variable.def.VariableType;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.bpel.xml.util.XmlUtil;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/09/12 23:20:19 $
 */
public class ElementValueDbTest extends XmlValueDbTestCase {

  private static final QName ELEMENT_NAME = new QName(BpelConstants.NS_EXAMPLES, "bike");
  private static final String ATTR_NAME = "type";
  private static final String ATTR_VALUE = "mountain";
  private static final String CHILD_LOCALNAME = "model";

  protected VariableType getVariableType(ImportDefinition importDefinition) {
    return importDefinition.getElementType(ELEMENT_NAME);
  }

  protected void update(Element variableValue) {
    variableValue.setAttribute(ATTR_NAME, ATTR_VALUE);
    Element childElem = variableValue.getOwnerDocument().createElementNS(BpelConstants.NS_EXAMPLES,
        CHILD_LOCALNAME);
    variableValue.appendChild(childElem);
  }

  protected void assertUpdate(Element variableValue) {
    assertEquals(ATTR_VALUE, variableValue.getAttribute(ATTR_NAME));
    assertNotNull(XmlUtil.getElement(variableValue, BpelConstants.NS_EXAMPLES, CHILD_LOCALNAME));
  }
}
