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
package org.jbpm.bpel.graph.exe.state;

import org.jbpm.bpel.graph.exe.ScopeInstance;
import org.jbpm.bpel.graph.exe.ScopeState;
import org.jbpm.bpel.graph.scope.Handler;

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2008/02/01 05:43:08 $
 */
public class CompensatingState extends ScopeState {

  private static final long serialVersionUID = 1L;

  public static final ScopeState DEFAULT = new CompensatingState("CD") {

    private static final long serialVersionUID = 1L;

    public void faulted(ScopeInstance scope) {
      EndState.enterFaulted(scope);
    }

    public void childrenCompensated(ScopeInstance scope) {
      EndState.enterCompensated(scope);
    }
  };

  public static final ScopeState EXPLICIT = new CompensatingState("CE") {

    private static final long serialVersionUID = 1L;

    public void completed(ScopeInstance scope) {
      // end handler token (do not verify parent termination)
      scope.getHandlerToken().end(false);
      EndState.enterCompensated(scope);
    }

    public void faulted(ScopeInstance scope) {
      scope.setState(TERMINATING_HANDLER);
      scope.terminateChildren();
    }
  };

  public static final ScopeState TERMINATING_HANDLER = new CompensatingState("CT") {

    private static final long serialVersionUID = 1L;

    public void childrenTerminated(ScopeInstance scope) {
      EndState.enterFaulted(scope);
    }
  };

  private CompensatingState(String name) {
    super(name);
  }

  public static void enterCompensating(ScopeInstance scope) {
    Handler handler = scope.getDefinition().getCompensationHandler();

    if (handler != null) {
      scope.setState(EXPLICIT);
      StateUtil.executeHandler(scope, handler);
    }
    else {
      scope.setState(DEFAULT);
      StateUtil.invokeDefaultCompensation(scope);
    }
  }
}
