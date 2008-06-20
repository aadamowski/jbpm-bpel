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

import javax.wsdl.Message;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.def.ImportDefinition;
import org.jbpm.bpel.persistence.db.AbstractDbTestCase;
import org.jbpm.bpel.variable.exe.MessageValue;
import org.jbpm.bpel.wsdl.xml.WsdlUtil;
import org.jbpm.bpel.xml.BpelConstants;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/10/13 02:53:28 $
 */
public class VariableDefinitionDbTest extends AbstractDbTestCase {

  BpelProcessDefinition process;
  VariableDefinition variable = new VariableDefinition();

  static final String VARIABLE_NAME = "v";

  protected void setUp() throws Exception {
    super.setUp();
    // variable
    variable.setName(VARIABLE_NAME);
    // process, create after opening jbpm context
    process = new BpelProcessDefinition("pd", BpelConstants.NS_EXAMPLES);
    process.getGlobalScope().addVariable(variable);
  }

  public void testName() {
    // save objects and load them back
    process = saveAndReload(process);
    variable = process.getGlobalScope().getVariable(VARIABLE_NAME);

    // verify retrieved objects
    assertEquals(VARIABLE_NAME, variable.getName());
  }

  public void testSchemaType() {
    // prepare persistent objects
    // schema type
    QName typeName = new QName(BpelConstants.NS_EXAMPLES, "st");
    VariableType type = process.getImportDefinition().getSchemaType(typeName);
    // variable
    variable.setType(type);

    // save objects and load them back
    process = saveAndReload(process);
    variable = process.getGlobalScope().getVariable(VARIABLE_NAME);
    type = variable.getType();

    // verify retrieved objects
    assertEquals(typeName, type.getName());
    // polymorphism
    Element value = (Element) type.createValue(variable);
    assertEquals(VARIABLE_NAME, value.getLocalName());
    assertNull(value.getNamespaceURI());
  }

  public void testElement() {
    // prepare persistent objects
    // element
    QName elementName = new QName(BpelConstants.NS_EXAMPLES, "e");
    VariableType type = process.getImportDefinition().getElementType(elementName);
    // variable
    variable.setType(type);

    // save objects and load them back
    process = saveAndReload(process);
    variable = process.getGlobalScope().getVariable(VARIABLE_NAME);
    type = variable.getType();

    // verify retrieved objects
    assertEquals(elementName, type.getName());
    // polymorphism
    Element value = (Element) type.createValue(variable);
    assertEquals(elementName.getLocalPart(), value.getLocalName());
    assertEquals(elementName.getNamespaceURI(), value.getNamespaceURI());
  }

  public void testMessageType() {
    // message
    QName messageName = new QName("namespace", "aMessage");
    Message message = WsdlUtil.getSharedDefinition().createMessage();
    message.setQName(messageName);
    // process
    ImportDefinition importDefinition = process.getImportDefinition();
    importDefinition.addMessage(message);
    // message type
    VariableType type = process.getImportDefinition().getMessageType(messageName);
    // variable
    variable.setType(type);

    // save objects and load them back
    process = saveAndReload(process);
    variable = process.getGlobalScope().getVariable(VARIABLE_NAME);
    type = variable.getType();

    // verify retrieved objects
    assertEquals(messageName, type.getName());
    // polymorphism
    MessageValue value = (MessageValue) type.createValue(variable);
    assertEquals(messageName, value.getType().getName());
  }
}
