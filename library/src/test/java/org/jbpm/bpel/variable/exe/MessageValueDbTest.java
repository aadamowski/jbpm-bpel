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
import javax.wsdl.Message;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.def.ImportDefinition;
import org.jbpm.bpel.persistence.db.AbstractDbTestCase;
import org.jbpm.bpel.variable.def.MessageType;
import org.jbpm.bpel.variable.def.VariableDefinition;
import org.jbpm.bpel.variable.exe.MessageValue;
import org.jbpm.bpel.wsdl.xml.WsdlUtil;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.bpel.xml.util.DatatypeUtil;
import org.jbpm.bpel.xml.util.XmlUtil;
import org.jbpm.graph.exe.ProcessInstance;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/10/13 02:53:26 $
 */
public class MessageValueDbTest extends AbstractDbTestCase {

  private ProcessInstance processInstance;

  private static final String WSDL_TEXT = "<definitions targetNamespace='http://jbpm.org/bpel/examples'"
      + " xmlns:tns='http://jbpm.org/bpel/examples'"
      + " xmlns:xsd='http://www.w3.org/2001/XMLSchema'"
      + " xmlns='http://schemas.xmlsoap.org/wsdl/'>"
      + "  <message name='request'>"
      + "    <part name='simplePart' type='xsd:string'/>"
      + "    <part name='elementPart' element='tns:surpriseElement'/>"
      + "  </message>"
      + "</definitions>";
  private static final QName MESSAGE_NAME = new QName(BpelConstants.NS_EXAMPLES, "request");
  private static final String VARIABLE_NAME = "msg";

  protected void setUp() throws Exception {
    super.setUp();
    // process, create after opening the jbpm context
    BpelProcessDefinition processDefinition = new BpelProcessDefinition("pd",
        BpelConstants.NS_EXAMPLES);
    // read wsdl
    Definition def = WsdlUtil.readText(WSDL_TEXT);
    // message
    Message message = def.getMessage(MESSAGE_NAME);
    ImportDefinition importDefinition = processDefinition.getImportDefinition();
    importDefinition.addMessage(message);
    MessageType type = importDefinition.getMessageType(MESSAGE_NAME);
    // variable
    VariableDefinition variable = new VariableDefinition();
    variable.setName(VARIABLE_NAME);
    variable.setType(type);
    processDefinition.getGlobalScope().addVariable(variable);
    // persist process
    graphSession.saveProcessDefinition(processDefinition);
    // process instance
    processInstance = new ProcessInstance(processDefinition);
    variable.createInstance(processInstance.getRootToken());
  }

  public void testDefinition() {
    // save the objects and load them back
    processInstance = saveAndReload(processInstance);
    MessageValue messageValue = (MessageValue) processInstance.getContextInstance().getVariable(
        VARIABLE_NAME);

    // verify the retrieved objects
    assertEquals(MESSAGE_NAME, messageValue.getType().getName());
  }

  public void testSchemaPart() {
    // prepare persistent objects
    MessageValue messageValue = (MessageValue) processInstance.getContextInstance().getVariable(
        VARIABLE_NAME);

    // simple part
    final String childCData = "toro";
    messageValue.setPart("simplePart", childCData);

    // save the objects and load them back
    processInstance = saveAndReload(processInstance);
    messageValue = (MessageValue) processInstance.getContextInstance().getVariable(VARIABLE_NAME);

    // verify the retrieved objects
    assertEquals(childCData, DatatypeUtil.toString(messageValue.getPart("simplePart")));
  }

  public void testElementPart() {
    // prepare persistent objects
    MessageValue messageValue = (MessageValue) processInstance.getContextInstance().getVariable(
        VARIABLE_NAME);

    // element part
    Element elementValue = XmlUtil.createElement(BpelConstants.NS_EXAMPLES, "e");
    final String attrName = "a";
    final String attrValue = "bucks";
    elementValue.setAttribute(attrName, attrValue);
    messageValue.setPart("elementPart", elementValue);

    // save the objects and load them back
    processInstance = saveAndReload(processInstance);
    messageValue = (MessageValue) processInstance.getContextInstance().getVariable(VARIABLE_NAME);

    // verify the retrieved objects
    elementValue = messageValue.getPart("elementPart");
    assertEquals(BpelConstants.NS_EXAMPLES, elementValue.getNamespaceURI());
    assertEquals("surpriseElement", elementValue.getLocalName());
    assertEquals(attrValue, elementValue.getAttribute(attrName));
  }
}
