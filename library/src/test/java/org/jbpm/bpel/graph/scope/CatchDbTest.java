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
package org.jbpm.bpel.graph.scope;

import javax.wsdl.Message;
import javax.xml.namespace.QName;

import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.def.ImportDefinition;
import org.jbpm.bpel.variable.def.VariableDefinition;
import org.jbpm.bpel.wsdl.xml.WsdlUtil;
import org.jbpm.bpel.xml.BpelConstants;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/06/09 23:36:38 $
 */
public class CatchDbTest extends AbstractHandlerDbTestCase {

  protected Handler getHandler(BpelProcessDefinition process) {
    return (Catch) process.getGlobalScope().getFaultHandlers().get(0);
  }

  protected Handler createHandler(BpelProcessDefinition process) {
    Catch faultHandler = new Catch();
    process.getGlobalScope().addCatch(faultHandler);
    return faultHandler;
  }

  public void testFaultName() {
    // prepare persistent objects
    // fault name
    QName faultName = new QName(BpelConstants.NS_EXAMPLES, "fault");
    // handler
    Catch catcher = (Catch) handler;
    catcher.setFaultName(faultName);

    // save objects and load them back
    BpelProcessDefinition process = saveAndReload(handler.getBpelProcessDefinition());
    catcher = (Catch) getHandler(process);

    // verify retrieved objects
    assertEquals(faultName, catcher.getFaultName());
  }

  public void testFaultVariable() {
    // prepare persistent objects
    BpelProcessDefinition process = handler.getBpelProcessDefinition();
    ImportDefinition importDefinition = process.getImportDefinition();
    // message
    QName messageName = new QName(BpelConstants.NS_EXAMPLES, "message");
    Message message = WsdlUtil.getSharedDefinition().createMessage();
    message.setQName(messageName);
    importDefinition.addMessage(message);
    // fault variable
    String variableName = "var";
    VariableDefinition faultVariable = new VariableDefinition();
    faultVariable.setName(variableName);
    faultVariable.setType(importDefinition.getMessageType(messageName));
    // handler
    Catch catcher = (Catch) handler;
    catcher.setFaultVariable(faultVariable);

    // save objects and load them back
    process = saveAndReload(process);
    catcher = (Catch) getHandler(process);
    faultVariable = catcher.getFaultVariable();

    // verify retrieved objects
    assertEquals(variableName, faultVariable.getName());
    assertEquals(messageName, faultVariable.getType().getName());
  }
}
