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

import javax.wsdl.Definition;
import javax.wsdl.Message;
import javax.wsdl.Part;
import javax.xml.namespace.QName;

import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.def.Import;
import org.jbpm.bpel.persistence.db.AbstractDbTestCase;
import org.jbpm.bpel.variable.def.MessageType;
import org.jbpm.bpel.wsdl.xml.WsdlUtil;
import org.jbpm.bpel.xml.BpelConstants;

/**
 * @author Juan Cantu
 * @version $Revision$ $Date: 2007/10/13 02:53:31 $
 */
public class MessageImplDbTest extends AbstractDbTestCase {

  BpelProcessDefinition processDefinition;

  protected void setUp() throws Exception {
    super.setUp();
    // process, create after opening a jbpm context
    processDefinition = new BpelProcessDefinition("pd", BpelConstants.NS_EXAMPLES);
  }

  public void testQName() {
    Definition definition = WsdlUtil.getFactory().newDefinition();

    QName messageName = new QName("namespace", "anyname");
    Message message = definition.createMessage();
    message.setQName(messageName);

    definition.addMessage(message);

    Import _import = new Import();
    _import.setType(Import.Type.WSDL);
    _import.setNamespace("namespace");
    _import.setDocument(definition);

    processDefinition.getImportDefinition().addImport(_import);

    // message type is created when queried the first time
    MessageType type = processDefinition.getImportDefinition().getMessageType(messageName);

    processDefinition = saveAndReload(processDefinition);
    type = processDefinition.getImportDefinition().getMessageType(messageName);

    assertEquals(messageName, type.getMessage().getQName());
  }

  public void testPart() {
    Definition definition = WsdlUtil.getFactory().newDefinition();

    String partName = "partName";
    QName elementName = new QName(null, "anElementName");
    Part part = definition.createPart();
    part.setName(partName);
    part.setElementName(elementName);

    QName messageName = new QName("namespace", "anyname");
    Message message = definition.createMessage();
    message.setQName(messageName);
    message.addPart(part);

    definition.addMessage(message);

    Import _import = new Import();
    _import.setType(Import.Type.WSDL);
    _import.setNamespace("namespace");
    _import.setDocument(definition);

    processDefinition.getImportDefinition().addImport(_import);

    // the message type is created when queried for the first time
    MessageType type = processDefinition.getImportDefinition().getMessageType(messageName);

    processDefinition = saveAndReload(processDefinition);
    type = processDefinition.getImportDefinition().getMessageType(messageName);
    part = type.getMessage().getPart(partName);

    assertEquals(partName, part.getName());
    assertEquals(elementName, part.getElementName());
  }
}
