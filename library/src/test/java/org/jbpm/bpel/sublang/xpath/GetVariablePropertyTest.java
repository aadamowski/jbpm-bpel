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
package org.jbpm.bpel.sublang.xpath;

import java.util.Map;

import javax.wsdl.Definition;
import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.jaxen.Context;
import org.jaxen.ContextSupport;
import org.jaxen.FunctionCallException;
import org.jaxen.SimpleNamespaceContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.bpel.graph.basic.Assign;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.def.ImportDefinition;
import org.jbpm.bpel.graph.scope.Scope;
import org.jbpm.bpel.variable.def.MessageType;
import org.jbpm.bpel.variable.def.VariableDefinition;
import org.jbpm.bpel.variable.exe.MessageValue;
import org.jbpm.bpel.wsdl.xml.WsdlConstants;
import org.jbpm.bpel.wsdl.xml.WsdlUtil;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.bpel.xml.BpelReader;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/10/13 02:53:25 $
 */
public class GetVariablePropertyTest extends TestCase {

  private MessageValue messageValue;
  private Context context;

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

  protected void setUp() throws Exception {
    /*
     * the process definition accesses the jbpm configuration, so create a context before creating a
     * process definition to avoid loading another configuration from the default resource
     */
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.getInstance("org/jbpm/bpel/graph/exe/test.jbpm.cfg.xml");
    jbpmContext = jbpmConfiguration.createJbpmContext();

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
    new BpelReader().registerPropertyAliases(importDefinition);
    // variable definition
    MessageType type = importDefinition.getMessageType(new QName(BpelConstants.NS_EXAMPLES,
        "request"));
    VariableDefinition messageVariable = new VariableDefinition();
    messageVariable.setName("msg");
    messageVariable.setType(type);
    scope.addVariable(messageVariable);
    // instantiate process
    Token token = new ProcessInstance(pd).getRootToken();
    token.setNode(primary);
    // initialize variables
    scope.createInstance(token).initializeData();
    messageValue = (MessageValue) messageVariable.getValueForAssign(token);
    // namespace declarations
    Map namespaceDeclarations = def.getNamespaces();
    namespaceDeclarations.remove("");
    // jaxen context
    ContextSupport sup = new ContextSupport();
    sup.setVariableContext(new TokenVariableContext(token));
    sup.setNamespaceContext(new SimpleNamespaceContext(namespaceDeclarations));
    context = new Context(sup);
  }

  protected void tearDown() throws Exception {
    jbpmContext.close();
  }

  public void testEvaluateAttribute() throws FunctionCallException {
    Element elementPart = messageValue.getPartForAssign("elementPart");
    Document doc = elementPart.getOwnerDocument();
    Element elemC = (Element) elementPart.appendChild(doc.createElementNS(null, "c"));
    elemC.setAttributeNS(null, "name", "wazabi");
    assertEquals("wazabi", GetVariablePropertyFunction.evaluate("msg", "tns:nameProperty", context));
  }

  public void testEvaluateElement() throws FunctionCallException {
    Element elementPart = messageValue.getPartForAssign("elementPart");
    Document doc = elementPart.getOwnerDocument();
    Element elemE = (Element) elementPart.appendChild(doc.createElementNS(null, "e"));
    elemE.appendChild(doc.createTextNode("30"));
    assertEquals("30", GetVariablePropertyFunction.evaluate("msg", "tns:idProperty", context));
  }
}
