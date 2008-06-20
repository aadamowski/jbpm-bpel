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

import org.w3c.dom.Element;

import org.jbpm.bpel.graph.basic.Validate;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.CompositeActivity;
import org.jbpm.bpel.variable.def.VariableDefinition;

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/05/31 12:55:12 $
 */
public class ValidateReader extends ActivityReader {

  /**
   * Loads the activity properties from the given DOM element
   */
  public Activity read(Element activityElem, CompositeActivity parent) {
    Validate validate = new Validate();
    readStandardProperties(activityElem, validate, parent);
    readValidate(activityElem, validate);
    return validate;
  }

  public void readValidate(Element validateElem, Validate validate) {
    validateNonInitial(validateElem, validate);

    // variables
    CompositeActivity parent = validate.getCompositeActivity();
    String[] variableNames = validateElem.getAttribute(BpelConstants.ATTR_VARIABLES).split("\\s");

    for (int v = 0; v < variableNames.length; v++) {
      String variableName = variableNames[v];
      VariableDefinition variable = parent.findVariable(variableName);

      if (variable == null) {
        bpelReader.getProblemHandler().add(new ParseProblem("variable not found", validateElem));
        // patch missing variable
        variable = new VariableDefinition();
        variable.setName(variableName);
      }

      validate.addVariable(variable);
    }
  }
}
