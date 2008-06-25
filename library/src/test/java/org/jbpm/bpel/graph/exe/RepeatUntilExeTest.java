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
package org.jbpm.bpel.graph.exe;

import javax.xml.namespace.QName;

import org.jbpm.bpel.graph.basic.Receive;
import org.jbpm.bpel.graph.struct.RepeatUntil;
import org.jbpm.bpel.variable.def.SchemaType;
import org.jbpm.bpel.variable.def.VariableDefinition;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2008/01/30 08:15:34 $
 */
public class RepeatUntilExeTest extends AbstractExeTestCase {

  private VariableDefinition variable = new VariableDefinition();
  private RepeatUntil repeatUntil;
  private Receive receive;

  protected void setUp() throws Exception {
    super.setUp();

    variable.setName("condition");
    variable.setType(new SchemaType(new QName(BpelConstants.NS_XML_SCHEMA, "boolean")));
    scope.addVariable(variable);

    final String xml = "<repeatUntil name='repeat'>"
        + " <receive name='receive' partnerLink='aPartner' operation='o'/>"
        + " <condition>$condition</condition>"
        + "</repeatUntil>";

    repeatUntil = (RepeatUntil) readActivity(xml, false);
    receive = (Receive) repeatUntil.getNode("receive");

    plugInner(repeatUntil);
  }

  public void testTrueCondition() {
    Token normalPath = prepareInner();
    variable.setValue(normalPath, Boolean.TRUE);
    firstActivity.leave(new ExecutionContext(normalPath));

    // token completes after reception
    assertReceiveAndComplete(normalPath, receive);
  }

  public void testFalseCondition() {
    Token normalPath = prepareInner();
    variable.setValue(normalPath, Boolean.FALSE);
    firstActivity.leave(new ExecutionContext(normalPath));

    // token loops back to receive activity after reception
    assertReceiveAndAdvance(normalPath, receive, receive);
    // set condition result to true
    variable.setValue(normalPath, Boolean.TRUE);
    // token completes after reception
    assertReceiveAndComplete(normalPath, receive);
  }
}
