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
import org.jbpm.bpel.graph.exe.Compensator;
import org.jbpm.bpel.graph.exe.ScopeInstance;
import org.jbpm.bpel.persistence.db.ScopeSession;
import org.jbpm.graph.exe.ExecutionContext;

/**
 * Causes one specified child scope to be compensated. The
 * {@link CompensateScope compensateScope} must only be used within
 * {@link Catch catch}, {@link Handler catchAll},
 * {@link Handler compensationHandler} and
 * {@link Handler terminationHandler}.
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/09/18 10:21:30 $
 */
public class CompensateScope extends Activity implements Compensator {

  private Scope target;

  private static final long serialVersionUID = 1L;

  public CompensateScope() {
  }

  public CompensateScope(String name) {
    super(name);
  }

  public Scope getTarget() {
    return target;
  }

  public void setTarget(Scope target) {
    this.target = target;
  }

  public void execute(ExecutionContext exeContext) {
    ScopeSession scopeSession = ScopeSession.getContextInstance(exeContext.getJbpmContext());
    ScopeInstance nestedScope = scopeSession.nextScopeToCompensate(
        exeContext.getProcessInstance(), target);

    if (nestedScope != null)
      nestedScope.compensate(this);
    else
      leave(exeContext);
  }

  public void scopeCompensated(ScopeInstance nestedInstance) {
    ScopeSession scopeSession = ScopeSession.getContextInstance(JbpmContext.getCurrentJbpmContext());
    ScopeInstance nextNestedInstance = scopeSession.nextScopeToCompensate(
        nestedInstance.getToken().getProcessInstance(), target);

    if (nextNestedInstance != null) {
      // If a completed scope instance is found, perform its compensation
      nextNestedInstance.compensate(this);
    }
    else {
      // continue the execution of the compensation handler
      ScopeInstance enclosingInstance = nestedInstance.getParent();
      while (!equals(enclosingInstance.getHandlerToken().getNode())) {
        enclosingInstance = enclosingInstance.getParent();
        if (enclosingInstance == null) {
          throw new IllegalStateException("could not find handler token: "
              + "compensate="
              + this
              + ", nestedScope="
              + nestedInstance);
        }
      }
      leave(new ExecutionContext(enclosingInstance.getHandlerToken()));
    }
  }
}
