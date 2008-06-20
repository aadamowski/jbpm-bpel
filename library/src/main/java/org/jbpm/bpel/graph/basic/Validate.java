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

import java.util.Collection;
import java.util.HashSet;

import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.BpelVisitor;
import org.jbpm.bpel.variable.def.VariableDefinition;

/**
 * Validates the values of variables against their associated XML and WSDL data
 * definition.
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/01/22 00:24:55 $
 */
public class Validate extends Activity {

  private static final long serialVersionUID = 1L;

  public Collection variables;

  public void accept(BpelVisitor visitor) {
    visitor.visit(this);
  }

  public void addVariable(VariableDefinition variable) {
    if (variables == null) {
      variables = new HashSet();
    }
    variables.add(variable);
  }

  public Collection getVariables() {
    return variables;
  }

  public void setVariables(Collection variables) {
    this.variables = variables;
  }
}
