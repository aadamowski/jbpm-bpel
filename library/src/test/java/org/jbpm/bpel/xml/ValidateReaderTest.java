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
package org.jbpm.bpel.xml;

import java.util.Collection;

import org.jbpm.bpel.graph.basic.Validate;
import org.jbpm.bpel.variable.def.VariableDefinition;

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2006/09/27 03:53:01 $
 */
public class ValidateReaderTest extends AbstractReaderTestCase {

  public void testVariables() throws Exception {
    VariableDefinition variableA = new VariableDefinition();
    variableA.setName("a");
    scope.addVariable(variableA);
    VariableDefinition variableB = new VariableDefinition();
    variableB.setName("b");
    scope.addVariable(variableB);
    String xml = "<validate variables='a b'/>";
    Validate validate = (Validate) readActivity(xml);
    Collection variables = validate.getVariables();
    assertEquals(variables.size(), 2);
    assertTrue(variables.contains(variableA));
  }
}
