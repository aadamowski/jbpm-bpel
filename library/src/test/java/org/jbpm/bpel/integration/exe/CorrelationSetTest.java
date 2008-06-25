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
package org.jbpm.bpel.integration.exe;

import java.util.HashMap;
import java.util.Set;

import javax.wsdl.Definition;
import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.def.ImportDefinition;
import org.jbpm.bpel.integration.def.CorrelationSetDefinition;
import org.jbpm.bpel.variable.def.MessageType;
import org.jbpm.bpel.variable.exe.MessageValue;
import org.jbpm.bpel.wsdl.xml.WsdlConstants;
import org.jbpm.bpel.wsdl.xml.WsdlUtil;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.bpel.xml.BpelReader;
import org.jbpm.bpel.xml.util.XmlUtil;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/10/13 02:53:29 $
 */
public class CorrelationSetTest extends TestCase {

  private CorrelationSetInstance correlationInstance;
  private MessageValue messageValue;

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
  private static final String ELEM_PART_TEXT = "<tns:surpriseElement xmlns:tns='http://jbpm.org/bpel/examples'>"
      + "  <b on=\"true\">true</b>"
      + "  <c name=\"venus\"/>"
      + "  <d amount=\"20\"/>"
      + "  <e>30</e>"
      + "</tns:surpriseElement>";
  private static final QName Q_NAME_PROP = new QName(BpelConstants.NS_EXAMPLES, "nameProperty");
  private static final QName Q_ID_PROP = new QName(BpelConstants.NS_EXAMPLES, "idProperty");

  public CorrelationSetTest(String name) {
    super(name);
  }

  protected void setUp() throws Exception {
    // read wsdl
    Definition def = WsdlUtil.readText(WSDL_TEXT);
    BpelProcessDefinition processDefinition = new BpelProcessDefinition("pd",
        BpelConstants.NS_EXAMPLES);
    ImportDefinition importDefinition = processDefinition.getImportDefinition();
    importDefinition.addImport(WsdlUtil.createImport(def));
    new BpelReader().registerPropertyAliases(importDefinition);
    // message type
    MessageType type = importDefinition.getMessageType(new QName(BpelConstants.NS_EXAMPLES,
        "request"));
    // message value
    messageValue = new MessageValue(type);
    // correlation set
    CorrelationSetDefinition correlationDefinition = new CorrelationSetDefinition();
    correlationDefinition.setName("cset");
    correlationDefinition.addProperty(WsdlUtil.getProperty(def, Q_NAME_PROP));
    correlationDefinition.addProperty(WsdlUtil.getProperty(def, Q_ID_PROP));
    // correlation set
    correlationInstance = new CorrelationSetInstance();
    correlationInstance.setDefinition(correlationDefinition);
  }

  public void testGetProperty() {
    HashMap propertyValues = new HashMap();
    propertyValues.put(Q_NAME_PROP, "elle");
    propertyValues.put(Q_ID_PROP, "1981");
    correlationInstance.initialize(propertyValues);
    assertEquals("elle", correlationInstance.getProperty(Q_NAME_PROP));
  }

  public void testGetProperties() {
    Set properties = correlationInstance.getDefinition().getProperties();
    assertEquals(2, properties.size());
  }

  public void testInitializeMap() {
    HashMap propertyValues = new HashMap();
    propertyValues.put(Q_NAME_PROP, "elle");
    propertyValues.put(Q_ID_PROP, "1981");
    correlationInstance.initialize(propertyValues);
    assertEquals(propertyValues, correlationInstance.getProperties());
  }

  public void testInitializeMessage() throws Exception {
    Element partValue = XmlUtil.parseText(ELEM_PART_TEXT);
    messageValue.setPart("elementPart", partValue);

    correlationInstance.initialize(messageValue);
    assertEquals("venus", correlationInstance.getProperty(Q_NAME_PROP));
    assertEquals("30", correlationInstance.getProperty(Q_ID_PROP));
  }

  public void testValidConstraint() throws SAXException {
    Element partValue = XmlUtil.parseText(ELEM_PART_TEXT);
    messageValue.setPart("elementPart", partValue);

    HashMap propertyValues = new HashMap();
    propertyValues.put(Q_NAME_PROP, "venus");
    propertyValues.put(Q_ID_PROP, "30");
    correlationInstance.initialize(propertyValues);
    try {
      correlationInstance.validateConstraint(messageValue);
    }
    catch (RuntimeException e) {
      fail(e.toString());
    }
  }

  public void testInvalidConstraint() throws SAXException {
    Element partValue = XmlUtil.parseText(ELEM_PART_TEXT);
    messageValue.setPart("elementPart", partValue);

    HashMap propertyValues = new HashMap();
    propertyValues.put(Q_NAME_PROP, "elle");
    propertyValues.put(Q_ID_PROP, "1981");
    correlationInstance.initialize(propertyValues);
    try {
      correlationInstance.validateConstraint(messageValue);
      fail("constraint validation should throw an exception");
    }
    catch (RuntimeException e) {
      // this exception is expected
    }
  }
}
