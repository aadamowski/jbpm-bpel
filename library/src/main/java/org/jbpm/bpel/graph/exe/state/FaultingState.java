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
public class FaultingState extends ScopeState {

  private static final long serialVersionUID = 1L;

  public static final ScopeState TERMINATING_PRIMARY = new FaultingState("FP") {

    private static final long serialVersionUID = 1L;

    public void childrenTerminated(ScopeInstance scope) {
      Handler handler = scope.getFaultHandler();
      if (handler != null) {
        scope.setState(EXPLICIT);
        StateUtil.executeHandler(scope, handler);
      }
      else {
        /*
         * TODO If the fault occurs in (or is rethrown to) the global process scope, and there is no
         * matching fault handler for the fault at the global level, the process terminates
         * abnormally, as though an exit activity had been performed.
         */
        scope.setState(DEFAULT);
        StateUtil.invokeDefaultCompensation(scope);
      }
    }
  };

  public static final ScopeState DEFAULT = new FaultingState("FD") {

    private static final long serialVersionUID = 1L;

    public void childrenCompensated(ScopeInstance scope) {
      EndState.enterFaulted(scope);
    }

    public void faulted(ScopeInstance scope) {
      EndState.enterFaulted(scope);
    }

    public boolean isTerminable() {
      return true;
    }
  };

  public static final ScopeState EXPLICIT = new FaultingState("FE") {

    private static final long serialVersionUID = 1L;

    public void completed(ScopeInstance scope) {
      EndState.enterExited(scope);
    }

    public void faulted(ScopeInstance scope) {
      scope.setState(TERMINATING_HANDLER);
      scope.terminateChildren();
    }

    public boolean isTerminable() {
      return true;
    }
  };

  public static final ScopeState TERMINATING_HANDLER = new FaultingState("FT") {

    private static final long serialVersionUID = 1L;

    public void childrenTerminated(ScopeInstance scope) {
      EndState.enterFaulted(scope);
    }
  };

  private FaultingState(String name) {
    super(name);
  }

  public void terminate(ScopeInstance scope) {
    /*
     * in case a Terminate signal is sent to a nested scope that has already faulted internally, the
     * Terminate signal is ignored; the scope will eventually send either a Faulted or an Exited
     * signal to the parent
     */
  }

  public static void enterFaulting(ScopeInstance scope) {
    scope.setState(FaultingState.TERMINATING_PRIMARY);
    scope.terminateChildren();
  }
}