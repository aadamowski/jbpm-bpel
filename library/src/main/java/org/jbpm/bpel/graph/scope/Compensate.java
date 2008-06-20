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

import org.jbpm.JbpmContext;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.BpelVisitor;
import org.jbpm.bpel.graph.exe.Compensator;
import org.jbpm.bpel.graph.exe.ScopeInstance;
import org.jbpm.bpel.persistence.db.ScopeSession;
import org.jbpm.graph.exe.ExecutionContext;

/**
 * Causes all immediately enclosed scopes to be compensated in default order. The
 * {@link CompensateScope compensateScope} must only be used within {@link Catch catch},
 * {@link Handler catchAll}, {@link Handler compensationHandler} and
 * {@link Handler terminationHandler}.
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/09/18 10:21:30 $
 */
public class Compensate extends Activity implements Compensator {

  private static final long serialVersionUID = 1L;

  public Compensate() {
  }

  public Compensate(String name) {
    super(name);
  }

  public void execute(ExecutionContext exeContext) {
    ScopeSession scopeSession = ScopeSession.getContextInstance(exeContext.getJbpmContext());
    ScopeInstance enclosingInstance = Scope.getInstance(exeContext.getToken());
    ScopeInstance nestedInstance = scopeSession.nextChildToCompensate(enclosingInstance);

    if (nestedInstance != null)
      nestedInstance.compensate(this);
    else
      leave(exeContext);
  }

  public void accept(BpelVisitor visitor) {
    visitor.visit(this);
  }

  public void scopeCompensated(ScopeInstance nestedInstance) {
    ScopeSession scopeSession = ScopeSession.getContextInstance(JbpmContext.getCurrentJbpmContext());
    ScopeInstance enclosingInstance = nestedInstance.getParent();
    ScopeInstance nextNestedInstance = scopeSession.nextChildToCompensate(enclosingInstance);

    if (nextNestedInstance != null)
      nextNestedInstance.compensate(this);
    else
      leave(new ExecutionContext(enclosingInstance.getHandlerToken()));
  }
}