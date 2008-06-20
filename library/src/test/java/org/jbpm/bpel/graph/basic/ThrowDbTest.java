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
package org.jbpm.bpel.graph.basic;

import javax.xml.namespace.QName;

import org.jbpm.bpel.graph.def.AbstractActivityDbTestCase;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.variable.def.VariableDefinition;
import org.jbpm.bpel.xml.BpelConstants;

/**
 * @author Juan Cantu
 * @version $Revision$ $Date: 2007/03/05 13:10:21 $
 */
public class ThrowDbTest extends AbstractActivityDbTestCase {

  public void testFaultVariable() {
    String varName = "v";
    VariableDefinition var = new VariableDefinition();
    var.setName(varName);
    processDefinition.getGlobalScope().addVariable(var);

    Throw _throw = createThrow();
    _throw.setFaultVariable(var);

    putThrow(processDefinition, _throw);

    processDefinition = saveAndReload(processDefinition);
    _throw = getThrow(processDefinition);

    assertEquals(varName, _throw.getFaultVariable().getName());
  }

  public void testFaultName() {
    QName faultName = new QName(BpelConstants.NS_EXAMPLES,
        "catastrophicException");
    Throw _throw = createThrow();
    _throw.setFaultName(faultName);

    putThrow(processDefinition, _throw);

    processDefinition = saveAndReload(processDefinition);
    _throw = getThrow(processDefinition);

    assertEquals(faultName, _throw.getFaultName());
  }

  protected Activity createActivity() {
    return createThrow();
  }

  private Throw createThrow() {
    return new Throw("throw");
  }

  private void putThrow(BpelProcessDefinition processDefinition, Throw _throw) {
    processDefinition.getGlobalScope().setActivity(_throw);
  }

  private Throw getThrow(BpelProcessDefinition processDefinition) {
    return (Throw) session.load(Throw.class, new Long(
        processDefinition.getGlobalScope().getActivity().getId()));
  }
}
