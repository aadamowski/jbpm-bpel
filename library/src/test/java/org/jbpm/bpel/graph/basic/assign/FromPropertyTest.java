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

import javax.wsdl.Definition;
import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.jbpm.bpel.graph.basic.Assign;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.def.ImportDefinition;
import org.jbpm.bpel.graph.scope.Scope;
import org.jbpm.bpel.variable.def.MessageType;
import org.jbpm.bpel.variable.def.VariableDefinition;
import org.jbpm.bpel.variable.exe.MessageValue;
import org.jbpm.bpel.wsdl.Property;
import org.jbpm.bpel.wsdl.xml.WsdlConstants;
import org.jbpm.bpel.wsdl.xml.WsdlUtil;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.bpel.xml.BpelReader;
import org.jbpm.bpel.xml.util.XmlUtil;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2008/02/04 14:35:48 $
 */
public class FromPropertyTest extends TestCase {

  private FromProperty from = new FromProperty();
  private Token token;

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
    // process definition
    BpelProcessDefinition processDefinition = new BpelProcessDefinition("pd",
        BpelConstants.NS_EXAMPLES);

    // wsdl
    Definition def = WsdlUtil.readText(WSDL_TEXT);
    ImportDefinition importDefinition = processDefinition.getImportDefinition();
    importDefinition.addImport(WsdlUtil.createImport(def));
    new BpelReader().registerPropertyAliases(importDefinition);

    // message
    MessageType messageType = importDefinition.getMessageType(new QName(BpelConstants.NS_EXAMPLES,
        "request"));

    // variable
    VariableDefinition variable = new VariableDefinition();
    variable.setName("msg");
    variable.setType(messageType);

    // property
    Property property = importDefinition.getProperty(new QName(BpelConstants.NS_EXAMPLES,
        "nameProperty"));

    // from
    from.setVariable(variable);
    from.setProperty(property);

    // copy
    Copy copy = new Copy();
    copy.setFrom(from);

    // assign
    Assign assign = new Assign("main");
    assign.addOperation(copy);

    // global scope
    Scope globalScope = processDefinition.getGlobalScope();
    globalScope.addVariable(variable);
    globalScope.setActivity(assign);

    // instantiate process
    token = new ProcessInstance(processDefinition).getRootToken();

    // initialize global scope
    globalScope.createInstance(token).initializeData();
  }

  public void testExtract() throws Exception {
    MessageValue messageValue = (MessageValue) from.getVariable().getValueForAssign(token);
    messageValue.setPart("elementPart", XmlUtil.parseText(ELEM_PART_TEXT));

    assertEquals("venus", from.extract(token));
  }
}
