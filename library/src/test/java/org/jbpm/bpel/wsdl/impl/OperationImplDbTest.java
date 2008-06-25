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
import javax.wsdl.Fault;
import javax.wsdl.Input;
import javax.wsdl.Message;
import javax.wsdl.Operation;
import javax.wsdl.Output;
import javax.wsdl.PortType;
import javax.xml.namespace.QName;

import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.def.ImportDefinition;
import org.jbpm.bpel.persistence.db.AbstractDbTestCase;
import org.jbpm.bpel.wsdl.xml.WsdlUtil;
import org.jbpm.bpel.xml.BpelConstants;

/**
 * @author Juan Cantu
 * @version $Revision$ $Date: 2007/10/13 02:53:31 $
 */
public class OperationImplDbTest extends AbstractDbTestCase {

  private BpelProcessDefinition processDefinition;
  private Message message;
  private Operation operation;

  private static final QName MESSAGE_NAME = new QName("msg");
  private static final QName PORT_TYPE_NAME = new QName("pt");
  private static final String OPERATION_NAME = "op";

  protected void setUp() throws Exception {
    super.setUp();

    Definition def = WsdlUtil.getSharedDefinition();
    // message
    message = def.createMessage();
    message.setQName(MESSAGE_NAME);
    // operation
    operation = def.createOperation();
    operation.setName(OPERATION_NAME);
    // port type
    PortType portType = def.createPortType();
    portType.setQName(PORT_TYPE_NAME);
    portType.addOperation(operation);
    // process
    processDefinition = new BpelProcessDefinition("pd", BpelConstants.NS_EXAMPLES);
    ImportDefinition importDefinition = processDefinition.getImportDefinition();
    importDefinition.addMessage(message);
    importDefinition.addPortType(portType);
  }

  public void testName() {
    // save the objects and load them back
    processDefinition = saveAndReload(processDefinition);
    operation = getOperation(processDefinition);
    // verify the retrieved objects
    assertEquals(OPERATION_NAME, operation.getName());
  }

  public void testInput() {
    // set up the persistent objects
    Input input = new InputImpl();
    input.setName("userSession");
    input.setMessage(message);
    operation.setInput(input);
    // save the objects and load them back
    processDefinition = saveAndReload(processDefinition);
    operation = getOperation(processDefinition);
    input = operation.getInput();
    // verify the retrieved objects
    assertEquals("userSession", input.getName());
    assertEquals(MESSAGE_NAME, input.getMessage().getQName());
  }

  public void testOutput() {
    // set up the persistent objects
    Output output = new OutputImpl();
    output.setName("balance");
    output.setMessage(message);
    operation.setOutput(output);
    // save the objects and load them back
    processDefinition = saveAndReload(processDefinition);
    operation = getOperation(processDefinition);
    output = operation.getOutput();
    // verify the retrieved objects
    assertEquals("balance", output.getName());
    assertEquals(MESSAGE_NAME, output.getMessage().getQName());
  }

  public void testFault() {
    // set up the persistent objects
    Fault fault = new FaultImpl();
    fault.setName("errorNumber");
    fault.setMessage(message);
    operation.addFault(fault);
    // save the objects and load them back
    processDefinition = saveAndReload(processDefinition);
    operation = getOperation(processDefinition);
    fault = operation.getFault("errorNumber");
    // verify the retrieved objects
    assertEquals("errorNumber", fault.getName());
    assertEquals(MESSAGE_NAME, fault.getMessage().getQName());
  }

  static Operation getOperation(BpelProcessDefinition process) {
    return process.getImportDefinition().getPortType(PORT_TYPE_NAME).getOperation(OPERATION_NAME,
        null, null);
  }
}
