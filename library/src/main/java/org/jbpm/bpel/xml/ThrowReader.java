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

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.jbpm.bpel.graph.basic.Throw;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.CompositeActivity;
import org.jbpm.bpel.variable.def.VariableDefinition;
import org.jbpm.bpel.variable.def.VariableType;
import org.jbpm.bpel.xml.util.XmlUtil;

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/09/04 06:42:26 $
 */
public class ThrowReader extends ActivityReader {

  /**
   * Loads the activity properties from the given DOM element
   */
  public Activity read(Element activityElem, CompositeActivity parent) {
    Throw _throw = new Throw();
    readStandardProperties(activityElem, _throw, parent);
    readThrow(activityElem, _throw);
    return _throw;
  }

  public void readThrow(Element throwElem, Throw _throw) {
    validateNonInitial(throwElem, _throw);

    // fault name - required
    QName faultName = XmlUtil.getQNameValue(throwElem.getAttributeNode(BpelConstants.ATTR_FAULT_NAME));
    _throw.setFaultName(faultName);

    // fault variable - optional
    String variableName = XmlUtil.getAttribute(throwElem, BpelConstants.ATTR_FAULT_VARIABLE);
    if (variableName != null) {
      VariableDefinition faultVariable = _throw.getCompositeActivity().findVariable(variableName);

      if (faultVariable != null) {
        VariableType variableType = faultVariable.getType();
        if (variableType.isMessage() || variableType.isElement()) {
          _throw.setFaultVariable(faultVariable);
        }
        else {
          bpelReader.getProblemHandler().add(
              new ParseProblem("fault variable must be either wsdl message or element", throwElem));
        }
      }
      else
        bpelReader.getProblemHandler().add(new ParseProblem("variable not found", throwElem));
    }
  }
}
