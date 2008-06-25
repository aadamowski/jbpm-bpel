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
package org.jbpm.bpel.sublang.exe;

import java.util.HashSet;
import java.util.Set;

import javax.wsdl.Definition;
import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.bpel.graph.basic.Assign;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.def.ImportDefinition;
import org.jbpm.bpel.graph.def.Namespace;
import org.jbpm.bpel.graph.scope.Scope;
import org.jbpm.bpel.sublang.def.Expression;
import org.jbpm.bpel.variable.def.VariableDefinition;
import org.jbpm.bpel.variable.exe.MessageValue;
import org.jbpm.bpel.wsdl.xml.WsdlConstants;
import org.jbpm.bpel.wsdl.xml.WsdlUtil;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.bpel.xml.BpelReader;
import org.jbpm.bpel.xml.util.DatatypeUtil;
import org.jbpm.bpel.xml.util.XmlUtil;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/10/13 02:53:26 $
 */
public class VariableAccessTest extends TestCase {

  private Token token;
  private MessageValue messageValue;
  private Set namespaces;
  private Element partValue;

  private JbpmContext jbpmContext;

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

  protected void setUp() throws Exception {
    /*
     * the process definition accesses the jbpm configuration, so create a context before creating a
     * process definition to avoid loading another configuration from the default resource
     */
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.getInstance("org/jbpm/bpel/graph/exe/test.jbpm.cfg.xml");
    jbpmContext = jbpmConfiguration.createJbpmContext();

    // process and global scope
    BpelProcessDefinition processDefinition = new BpelProcessDefinition("pd",
        BpelConstants.NS_EXAMPLES);
    Scope scope = processDefinition.getGlobalScope();
    // primary activity
    Activity primary = new Assign();
    scope.setActivity(primary);
    // wsdl description
    Definition def = WsdlUtil.readText(WSDL_TEXT);
    ImportDefinition importDefinition = processDefinition.getImportDefinition();
    importDefinition.addImport(WsdlUtil.createImport(def));
    new BpelReader().registerPropertyAliases(importDefinition);
    // namespace declarations
    namespaces = new HashSet();
    namespaces.add(new Namespace("tns", BpelConstants.NS_EXAMPLES));
    // the bpel 1.1 namespace is required to access variables through the
    // function getVariableData
    namespaces.add(new Namespace("bpws11", BpelConstants.NS_BPEL_1_1));
    // variable definition
    VariableDefinition messageDefinition = new VariableDefinition();
    messageDefinition.setName("msg");
    messageDefinition.setType(importDefinition.getMessageType(new QName(BpelConstants.NS_EXAMPLES,
        "request")));
    scope.addVariable(messageDefinition);
    // instantiate process
    token = new ProcessInstance(processDefinition).getRootToken();
    token.setNode(primary);
    // variable instance
    messageDefinition.createInstance(token);
    messageValue = (MessageValue) messageDefinition.getValueForAssign(token);
    // initialize the element part
    partValue = XmlUtil.parseText(ELEM_PART_TEXT);
    messageValue.setPart("elementPart", partValue);
  }

  protected void tearDown() throws Exception {
    jbpmContext.close();
  }

  public void testMessageSimplePart() {
    messageValue.setPart("simplePart", "hola");
    Element simplePart = (Element) eval("$msg.simplePart");
    assertNull(simplePart.getNamespaceURI());
    assertEquals("simplePart", simplePart.getLocalName());
    assertEquals("hola", DatatypeUtil.toString(simplePart));
  }

  public void testMessageElementPart() {
    Element elementPart = (Element) eval("$msg.elementPart");
    assertEquals(BpelConstants.NS_EXAMPLES, elementPart.getNamespaceURI());
    assertEquals("surpriseElement", elementPart.getLocalName());
  }

  public void testMessageAttributeLocation() throws Exception {
    Attr nameAttr = (Attr) eval("$msg.elementPart/c/@name");
    assertEquals("venus", nameAttr.getValue());
  }

  public void testMessageElementLocation() throws Exception {
    Element e = (Element) eval("$msg.elementPart/e");
    assertEquals("30", DatatypeUtil.toString(e));
  }

  public void testFunctionSimplePart() {
    messageValue.setPart("simplePart", "hola");
    Element simplePart = (Element) eval("bpws11:getVariableData('msg', 'simplePart')");
    assertNull(simplePart.getNamespaceURI());
    assertEquals("simplePart", simplePart.getLocalName());
    assertEquals("hola", DatatypeUtil.toString(simplePart));
  }

  public void testFunctionElementPart() {
    Element elementPart = (Element) eval("bpws11:getVariableData('msg', 'elementPart')");
    assertEquals(BpelConstants.NS_EXAMPLES, elementPart.getNamespaceURI());
    assertEquals("surpriseElement", elementPart.getLocalName());
  }

  public void testFunctionAttributeLocation() throws Exception {
    Attr nameAttr = (Attr) eval("bpws11:getVariableData('msg', 'elementPart', '/tns:surpriseElement/c/@name')");
    assertEquals("venus", nameAttr.getValue());
  }

  public void testFunctionElementLocation() throws Exception {
    Element e = (Element) eval("bpws11:getVariableData('msg', 'elementPart', '/tns:surpriseElement/e')");
    assertEquals("30", DatatypeUtil.toString(e));
  }

  public void testFunctionAttributeProperty() throws Exception {
    assertEquals("venus", eval("bpws11:getVariableProperty('msg', 'tns:nameProperty')"));
  }

  public void testFunctionElementProperty() throws Exception {
    assertEquals("30", eval("bpws11:getVariableProperty('msg', 'tns:idProperty')"));
  }

  private Object eval(String text) {
    Expression expr = new Expression();
    expr.setText(text);
    expr.setNamespaces(namespaces);
    return expr.getEvaluator().evaluate(token);
  }
}