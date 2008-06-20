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

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2008/02/01 05:43:08 $
 */
public abstract class ActiveState extends ScopeState {

  public static final ScopeState PRIMARY = new ActiveState("AP") {

    private static final long serialVersionUID = 1L;

    public void completed(ScopeInstance scope) {
      scope.disableEvents();
      if (!scope.hasPendingEvents()) {
        // set completed if no events left
        EndState.enterCompleted(scope);
      }
      else {
        // wait for completion of pending events
        scope.setState(EVENTS);
      }
    }
  };

  public static final ScopeState EVENTS = new ActiveState("AE") {

    private static final long serialVersionUID = 1L;

    public void completed(ScopeInstance scope) {
      EndState.enterCompleted(scope);
    }
  };

  /**
   * Constructs an active state identified by the given name.
   * @param name
   */
  private ActiveState(String name) {
    super(name);
  }

  public void terminate(ScopeInstance scope) {
    TerminatingState.enterTerminating(scope);
  }

  public void faulted(ScopeInstance scope) {
    FaultingState.enterFaulting(scope);
  }

  public boolean isTerminable() {
    return true;
  }
}
