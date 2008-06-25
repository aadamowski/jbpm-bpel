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

import java.util.ArrayList;
import java.util.List;

import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.BpelVisitor;
import org.jbpm.graph.exe.ExecutionContext;

/**
 * Updates the values of variables with new data.
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/07/22 05:54:37 $
 */
public class Assign extends Activity {

  private List operations = new ArrayList();

  private static final long serialVersionUID = 1L;

  public Assign() {
  }

  public Assign(String name) {
    super(name);
  }

  public void execute(ExecutionContext exeContext) {
    for (int i = 0, n = operations.size(); i < n; i++) {
      AssignOperation operation = (AssignOperation) operations.get(i);
      operation.execute(exeContext);
    }
    leave(exeContext);
  }

  public void addOperation(AssignOperation operation) {
    operations.add(operation);
  }

  public List getOperations() {
    return operations;
  }

  public void accept(BpelVisitor visitor) {
    visitor.visit(this);
  }
}
