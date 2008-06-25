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

import org.w3c.dom.Element;

import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.def.ImportDefinition;
import org.jbpm.bpel.persistence.db.AbstractDbTestCase;
import org.jbpm.bpel.variable.def.VariableDefinition;
import org.jbpm.bpel.variable.def.VariableType;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.graph.exe.ProcessInstance;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/10/13 02:53:26 $
 */
public abstract class XmlValueDbTestCase extends AbstractDbTestCase {

  private ProcessInstance processInstance;

  private static final String VARIABLE_NAME = "vehicle";

  protected XmlValueDbTestCase() {
  }

  protected void setUp() throws Exception {
    // set up db stuff
    super.setUp();
    // process, create after opening the jbpm context
    BpelProcessDefinition processDefinition = new BpelProcessDefinition("pd",
        BpelConstants.NS_EXAMPLES);
    // schema type
    VariableType type = getVariableType(processDefinition.getImportDefinition());
    // variable
    VariableDefinition variable = new VariableDefinition();
    variable.setName(VARIABLE_NAME);
    variable.setType(type);
    processDefinition.getGlobalScope().addVariable(variable);
    // persist process
    graphSession.saveProcessDefinition(processDefinition);
    // instantiate process
    processInstance = new ProcessInstance(processDefinition);
    variable.createInstance(processInstance.getRootToken());
  }

  public void testCreateValue() {
    // process instance
    processInstance = saveAndReload(processInstance);

    // get the variable value
    Element elementValue = (Element) processInstance.getContextInstance()
        .getVariable(VARIABLE_NAME);
    // check the placeholder has the initialization mark
    assertEquals("false", elementValue.getAttributeNS(BpelConstants.NS_VENDOR,
        BpelConstants.ATTR_INITIALIZED));
  }

  public void testSetValue() {
    // process instance
    processInstance = saveAndReload(processInstance);

    // get the variable value
    Element variableValue = (Element) processInstance.getContextInstance().getVariable(
        VARIABLE_NAME);
    // assign a value to the variable
    update(variableValue);
    // check the value has been updated in memory
    assertUpdate(variableValue);
    // now save the process instance
    processInstance = saveAndReload(processInstance);
    // check the value has been updated in the database
    variableValue = (Element) processInstance.getContextInstance().getVariable(VARIABLE_NAME);
    assertUpdate(variableValue);
  }

  protected abstract VariableType getVariableType(ImportDefinition importDefinition);

  protected abstract void update(Element variableValue);

  protected abstract void assertUpdate(Element variableValue);
}
