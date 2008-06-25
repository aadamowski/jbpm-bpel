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

import javax.wsdl.Definition;
import javax.xml.namespace.QName;

import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.def.ImportDefinition;
import org.jbpm.bpel.persistence.db.AbstractDbTestCase;
import org.jbpm.bpel.variable.def.MessageType;
import org.jbpm.bpel.wsdl.xml.WsdlConstants;
import org.jbpm.bpel.wsdl.xml.WsdlUtil;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.bpel.xml.BpelReader;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/10/13 02:53:28 $
 */
public class MessageTypeDbTest extends AbstractDbTestCase {

  BpelProcessDefinition process;

  private static final String WSDL_TEXT = "<definitions targetNamespace='"
      + BpelConstants.NS_EXAMPLES
      + "' xmlns:tns='"
      + BpelConstants.NS_EXAMPLES
      + "' xmlns:xsd='http://www.w3.org/2001/XMLSchema'"
      + " xmlns:vprop='"
      + WsdlConstants.NS_VPROP
      + "' xmlns='http://schemas.xmlsoap.org/wsdl/'>"
      + "  <message name='request'>"
      + "    <part name='simplePart' type='xsd:string'/>"
      + "    <part name='elementPart' element='tns:surpriseElement'/>"
      + "  </message>"
      + "  <vprop:property name='nameProperty' type='xsd:string'/>"
      + "  <vprop:property name='idProperty' type='xsd:int'/>"
      + "  <vprop:propertyAlias propertyName='tns:nameProperty' messageType='tns:request' part='elementPart'>"
      + "    <vprop:query>c/@name</vprop:query>"
      + "  </vprop:propertyAlias>"
      + "  <vprop:propertyAlias propertyName='tns:idProperty' messageType='tns:request' part='elementPart'>"
      + "    <vprop:query>e</vprop:query>"
      + "  </vprop:propertyAlias>"
      + "</definitions>";
  private static final QName MESSAGE_NAME = new QName(BpelConstants.NS_EXAMPLES, "request");

  protected void setUp() throws Exception {
    super.setUp();
    // process, create after opening jbpm context
    process = new BpelProcessDefinition("pd", BpelConstants.NS_EXAMPLES);
    // read wsdl
    Definition def = WsdlUtil.readText(WSDL_TEXT);
    ImportDefinition importDefinition = process.getImportDefinition();
    importDefinition.addImport(WsdlUtil.createImport(def));
    new BpelReader().registerPropertyAliases(importDefinition);
  }

  public void testGetName() {
    // save objects and load them back
    process = saveAndReload(process);
    MessageType type = process.getImportDefinition().getMessageType(MESSAGE_NAME);

    // verify retrieved object
    assertEquals(MESSAGE_NAME, type.getName());
  }

  public void testGetMessage() {
    // save objects and load them back
    process = saveAndReload(process);
    MessageType type = process.getImportDefinition().getMessageType(MESSAGE_NAME);

    // verify retrieved object
    assertEquals(2, type.getMessage().getParts().size());
  }

  public void testGetPropertyAliases() {
    // save objects and load them back
    process = saveAndReload(process);
    MessageType type = process.getImportDefinition().getMessageType(MESSAGE_NAME);

    // verify retrieved object
    assertEquals(2, type.getPropertyAliases().size());
  }
}
