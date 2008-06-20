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
package org.jbpm.bpel.graph.basic.assign;

import java.util.Collections;
import java.util.Set;

import javax.wsdl.Definition;
import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import org.jbpm.bpel.graph.basic.Assign;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.def.ImportDefinition;
import org.jbpm.bpel.graph.def.Namespace;
import org.jbpm.bpel.graph.scope.Scope;
import org.jbpm.bpel.sublang.def.VariableQuery;
import org.jbpm.bpel.variable.def.SchemaType;
import org.jbpm.bpel.variable.def.VariableDefinition;
import org.jbpm.bpel.variable.exe.MessageValue;
import org.jbpm.bpel.wsdl.xml.WsdlUtil;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.bpel.xml.util.DatatypeUtil;
import org.jbpm.bpel.xml.util.XmlUtil;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/10/13 02:53:27 $
 */
public class FromVariableTest extends TestCase {

  private Token token;
  private MessageValue messageValue;
  private FromVariable from = new FromVariable();

  private VariableDefinition simpleVariable = new VariableDefinition();
  private VariableDefinition messageVariable = new VariableDefinition();

  private static final String WSDL_TEXT = "<definitions targetNamespace='http://jbpm.org/bpel/examples'"
      + " xmlns:tns='http://jbpm.org/bpel/examples'"
      + " xmlns:xsd='http://www.w3.org/2001/XMLSchema'"
      + " xmlns='http://schemas.xmlsoap.org/wsdl/'>"
      + "  <message name='request'>"
      + "    <part name='simplePart' type='xsd:string'/>"
      + "    <part name='elementPart' element='tns:surpriseElement'/>"
      + "  </message>"
      + "</definitions>";
  private static final String ELEM_PART_TEXT = "<tns:surpriseElement xmlns:tns='http://jbpm.org/bpel/examples'>"
      + "  <b on=\"true\">true</b>"
      + "  <c name=\"venus\"/>"
      + "  <d amount=\"20\"/>"
      + "  <e>30</e>"
      + "</tns:surpriseElement>";
  private static final String SIMPLE_VAR_TEXT = "4";

  protected void setUp() throws Exception {
    // process and global scope
    BpelProcessDefinition pd = new BpelProcessDefinition("pd", BpelConstants.NS_EXAMPLES);
    Scope scope = pd.getGlobalScope();
    // primary activity
    Activity primary = new Assign();
    scope.setActivity(primary);
    // wsdl description
    Definition def = WsdlUtil.readText(WSDL_TEXT);
    ImportDefinition importDefinition = pd.getImportDefinition();
    importDefinition.addImport(WsdlUtil.createImport(def));
    // message variable
    messageVariable.setName("msg");
    messageVariable.setType(importDefinition.getMessageType(new QName(BpelConstants.NS_EXAMPLES,
        "request")));
    scope.addVariable(messageVariable);
    // simple variable
    simpleVariable.setName("index");
    simpleVariable.setType(new SchemaType(new QName(BpelConstants.NS_XML_SCHEMA, "int")));
    scope.addVariable(simpleVariable);
    // instantiate process
    token = new ProcessInstance(pd).getRootToken();
    token.setNode(primary);
    // initialize global scope
    scope.createInstance(token).initializeData();
    simpleVariable.setValue(token, SIMPLE_VAR_TEXT);
    messageValue = (MessageValue) messageVariable.getValueForAssign(token);
  }

  public void testExtractVariable_simple() throws SAXException {
    // variable
    from.setVariable(simpleVariable);
    // assertion
    Element extractedValue = (Element) from.extract(token);
    assertEquals(SIMPLE_VAR_TEXT, DatatypeUtil.toString(extractedValue));
  }

  public void testExtractVariable_message() throws SAXException {
    // from
    from.setVariable(messageVariable);
    // instance data
    Element partValue = XmlUtil.parseText(ELEM_PART_TEXT);
    messageValue.setPart("elementPart", partValue);
    // assertion
    MessageValue extractedValue = (MessageValue) from.extract(token);
    assertSame(messageValue, extractedValue);
  }

  public void testExtractPart() throws SAXException {
    // from
    from.setVariable(messageVariable);
    from.setPart("elementPart");
    // instance data
    Element partValue = XmlUtil.parseText(ELEM_PART_TEXT);
    messageValue.setPart("elementPart", partValue);
    Element elementPart = messageValue.getPart("elementPart");
    // assertion
    assertSame(elementPart, from.extract(token));
  }

  public void testExtractQuery_noContextAccess() throws SAXException {
    // namespace declarations
    Set namespaces = Collections.singleton(new Namespace("tns", BpelConstants.NS_EXAMPLES));
    // query
    VariableQuery query = new VariableQuery();
    query.setText("/tns:surpriseElement/c/@name");
    query.setNamespaces(namespaces);
    // from
    from.setVariable(messageVariable);
    from.setPart("elementPart");
    from.setQuery(query);
    // instance data
    Element partValue = XmlUtil.parseText(ELEM_PART_TEXT);
    messageValue.setPart("elementPart", partValue);
    // assertion
    assertEquals("venus", DatatypeUtil.toString((Attr) from.extract(token)));
  }

  public void testExtractQuery_contextAccess() throws SAXException {
    // enclosing element
    Set namespaces = Collections.singleton(new Namespace("bpel", BpelConstants.NS_BPEL_1_1));
    // query
    VariableQuery query = new VariableQuery();
    query.setText("*[position() = bpel:getVariableData('index')]");
    query.setNamespaces(namespaces);
    // from
    from.setVariable(messageVariable);
    from.setPart("elementPart");
    from.setQuery(query);
    // instance data
    Element partValue = XmlUtil.parseText(ELEM_PART_TEXT);
    messageValue.setPart("elementPart", partValue);
    // assertion
    assertEquals("30", DatatypeUtil.toString((Element) from.extract(token)));
  }
}
