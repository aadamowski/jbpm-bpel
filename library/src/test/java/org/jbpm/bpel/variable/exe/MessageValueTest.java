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

import javax.wsdl.Definition;
import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.w3c.dom.Element;

import org.jbpm.bpel.BpelException;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.def.ImportDefinition;
import org.jbpm.bpel.variable.def.MessageType;
import org.jbpm.bpel.wsdl.xml.WsdlConstants;
import org.jbpm.bpel.wsdl.xml.WsdlUtil;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.bpel.xml.BpelReader;
import org.jbpm.bpel.xml.util.DatatypeUtil;
import org.jbpm.bpel.xml.util.XmlUtil;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/10/13 02:53:26 $
 */
public class MessageValueTest extends TestCase {

  private MessageValue messageValue;
  private MessageType messageType;

  private static final String WSDL_TEXT = "<definitions targetNamespace='http://jbpm.org/bpel/examples'"
      + " xmlns:tns='http://jbpm.org/bpel/examples'"
      + " xmlns:xsd='http://www.w3.org/2001/XMLSchema'"
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
  private static final String ELEMENT_VALUE = "<tns:surpriseElement xmlns:tns='http://jbpm.org/bpel/examples'>"
      + " <b on=\"true\">true</b>"
      + " <c name=\"venus\"/>"
      + " <d amount=\"20\"/>"
      + " <e>30</e>"
      + "</tns:surpriseElement>";
  private static final QName Q_NAME_PROP = new QName(BpelConstants.NS_EXAMPLES, "nameProperty");
  private static final QName Q_ID_PROP = new QName(BpelConstants.NS_EXAMPLES, "idProperty");

  protected void setUp() throws Exception {
    BpelProcessDefinition processDefinition = new BpelProcessDefinition("pd",
        BpelConstants.NS_EXAMPLES);
    // read wsdl
    Definition def = WsdlUtil.readText(WSDL_TEXT);
    ImportDefinition importDefinition = processDefinition.getImportDefinition();
    importDefinition.addImport(WsdlUtil.createImport(def));
    new BpelReader().registerPropertyAliases(importDefinition);
    // message type
    messageType = importDefinition.getMessageType(new QName(BpelConstants.NS_EXAMPLES, "request"));
    // message value
    messageValue = new MessageValue(messageType);
  }

  public void testGetSimplePart() throws Exception {
    messageValue.setPart("simplePart", "wawa");
    Element part = messageValue.getPart("simplePart");
    assertEquals("simplePart", part.getLocalName());
    assertEquals(null, part.getNamespaceURI());
    assertEquals("wawa", DatatypeUtil.toString(part));
  }

  public void testGetElementPart() throws Exception {
    messageValue.setPart("elementPart", XmlUtil.parseText(ELEMENT_VALUE));
    Element part = messageValue.getPart("elementPart");
    assertEquals("surpriseElement", part.getLocalName());
    assertEquals(BpelConstants.NS_EXAMPLES, part.getNamespaceURI());
    assertNotNull(XmlUtil.getElement(part, "b"));
    assertNotNull(XmlUtil.getElement(part, "c"));
    assertNotNull(XmlUtil.getElement(part, "d"));
    assertNotNull(XmlUtil.getElement(part, "e"));
  }

  public void testGetInvalidPart() {
    try {
      messageValue.getPart("invalidPart");
      fail("set part should have failed");
    }
    catch (BpelException e) {
      // this exception is expected
    }
  }

  public void testGetAttributeProperty() throws Exception {
    messageValue.setPart("elementPart", XmlUtil.parseText(ELEMENT_VALUE));
    assertEquals("venus", messageType.getPropertyValue(Q_NAME_PROP, messageValue));
  }

  public void testGetElementProperty() throws Exception {
    messageValue.setPart("elementPart", XmlUtil.parseText(ELEMENT_VALUE));
    assertEquals("30", messageType.getPropertyValue(Q_ID_PROP, messageValue));
  }
}
