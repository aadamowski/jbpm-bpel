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
import java.util.HashSet;
import java.util.Set;

import javax.wsdl.Definition;
import javax.xml.namespace.QName;

import junit.framework.TestCase;

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
public class ToVariableTest extends TestCase {

  private VariableDefinition simpleVariable;
  private VariableDefinition messageVariable;
  private ToVariable to = new ToVariable();

  private Token token;
  private MessageValue messageValue;

  private static final String WSDL_TEXT = "<definitions targetNamespace='http://jbpm.org/bpel/examples'"
      + " xmlns:tns='http://jbpm.org/bpel/examples'"
      + " xmlns:xsd='http://www.w3.org/2001/XMLSchema'"
      + " xmlns='http://schemas.xmlsoap.org/wsdl/'>"
      + "  <message name='request'>"
      + "    <part name='simplePart' type='xsd:string'/>"
      + "    <part name='elementPart' element='tns:surpriseElement'/>"
      + "  </message>"
      + "</definitions>";
  private static final String ELEM_SURPRISE = "surpriseElement";

  public ToVariableTest(String name) {
    super(name);
  }

  protected void setUp() throws Exception {
    // process and global scope
    BpelProcessDefinition pd = new BpelProcessDefinition("pd", BpelConstants.NS_EXAMPLES);
    Scope scope = pd.getGlobalScope();
    // primary activity
    Activity primary = new Assign();
    scope.setActivity(primary);
    // message type
    Definition def = WsdlUtil.readText(WSDL_TEXT);
    ImportDefinition importDefinition = pd.getImportDefinition();
    importDefinition.addImport(WsdlUtil.createImport(def));
    // message variable
    messageVariable = new VariableDefinition();
    messageVariable.setName("msg");
    messageVariable.setType(importDefinition.getMessageType(new QName(BpelConstants.NS_EXAMPLES,
        "request")));
    scope.addVariable(messageVariable);
    // simple variable
    simpleVariable = new VariableDefinition();
    simpleVariable.setName("index");
    simpleVariable.setType(new SchemaType(new QName(BpelConstants.NS_XML_SCHEMA, "int")));
    scope.addVariable(simpleVariable);
    // instantiate process
    token = new ProcessInstance(pd).getRootToken();
    token.setNode(primary);
    // initialize global scope
    scope.createInstance(token).initializeData();
    messageValue = (MessageValue) messageVariable.getValueForAssign(token);
  }

  public void testAssignVariable() {
    // to
    to.setVariable(simpleVariable);
    // instance data
    to.assign(token, "frack");
    // assertions
    Element simpleValue = (Element) simpleVariable.getValue(token);
    assertEquals("frack", DatatypeUtil.toString(simpleValue));
  }

  public void testAssignPart() {
    // to
    to.setVariable(messageVariable);
    to.setPart("elementPart");
    // instance data
    Element sourceElem = XmlUtil.createElement("urn:foo", "sourceElement");
    sourceElem.appendChild(sourceElem.getOwnerDocument().createElementNS(BpelConstants.NS_EXAMPLES,
        ELEM_SURPRISE));
    to.assign(token, sourceElem);
    // assertions
    Element part = messageValue.getPart("elementPart");
    assertEquals(BpelConstants.NS_EXAMPLES, part.getNamespaceURI());
    assertEquals(ELEM_SURPRISE, part.getLocalName());
  }

  public void testAssignQuery_noContextAccess() {
    // namespace declarations
    Set namespaces = Collections.singleton(new Namespace("tns", BpelConstants.NS_EXAMPLES));
    // query
    VariableQuery query = new VariableQuery();
    query.setText("/tns:surpriseElement/e");
    query.setNamespaces(namespaces);
    // to
    to.setVariable(messageVariable);
    to.setPart("elementPart");
    to.setQuery(query);
    // instance data
    to.assign(token, "1981");
    // assertions
    Element part = messageValue.getPart("elementPart");
    assertEquals("1981", DatatypeUtil.toString(XmlUtil.getElement(part, "e")));
  }

  public void testAssignQuery_contextAccess() throws SAXException {
    // namespace declarations
    Set namespaces = new HashSet();
    namespaces.add(new Namespace("tns", BpelConstants.NS_EXAMPLES));
    namespaces.add(new Namespace("bpel", BpelConstants.NS_BPEL_1_1));
    // query
    VariableQuery query = new VariableQuery();
    query.setText("/tns:surpriseElement/*[position() = bpel:getVariableData('index')]");
    query.setNamespaces(namespaces);
    // to
    to.setVariable(messageVariable);
    to.setPart("elementPart");
    to.setQuery(query);
    // instance data
    messageValue.setPart("elementPart", XmlUtil.parseText("<tns:surpriseElement xmlns:tns='"
        + BpelConstants.NS_EXAMPLES
        + "'>"
        + "  <b on=\"true\">true</b>"
        + "  <c name=\"venus\"/>"
        + "  <d amount=\"20\"/>"
        + "  <e>30</e>"
        + "</tns:surpriseElement>"));
    simpleVariable.setValue(token, new Integer(4));
    to.assign(token, "1981");
    // assertions
    Element part = messageValue.getPart("elementPart");
    assertEquals("1981", DatatypeUtil.toString(XmlUtil.getElement(part, "e")));
  }
}