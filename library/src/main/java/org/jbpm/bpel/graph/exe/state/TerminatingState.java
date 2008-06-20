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
public class TerminatingState extends ScopeState {

  private static final long serialVersionUID = 1L;

  public static final ScopeState TERMINATING_PRIMARY = new TerminatingState("TP") {

    private static final long serialVersionUID = 1L;

    public void faulted(ScopeInstance scope) {
      /*
       * A fault at this point is ignored, it comes from a faulted notification send from a child
       * before it was canceled.
       */
    }

    public void childrenTerminated(ScopeInstance scope) {
      Handler handler = scope.getDefinition().getTerminationHandler();
      if (handler != null) {
        scope.setState(EXPLICIT);
        StateUtil.executeHandler(scope, handler);
      }
      else {
        scope.setState(DEFAULT);
        StateUtil.invokeDefaultCompensation(scope);
      }
    }
  };

  public static final ScopeState DEFAULT = new TerminatingState("TD") {

    private static final long serialVersionUID = 1L;

    public void faulted(ScopeInstance scope) {
      EndState.enterTerminated(scope);
    }

    public void childrenCompensated(ScopeInstance scope) {
      EndState.enterTerminated(scope);
    }

    public boolean isTerminable() {
      return true;
    }
  };

  public static final ScopeState EXPLICIT = new TerminatingState("TE") {

    private static final long serialVersionUID = 1L;

    public void faulted(ScopeInstance scope) {
      scope.setState(TERMINATING_HANDLER);
      scope.terminateChildren();
    }

    public void completed(ScopeInstance scope) {
      EndState.enterTerminated(scope);
    }

    public boolean isTerminable() {
      return true;
    }
  };

  public static final ScopeState TERMINATING_HANDLER = new TerminatingState("TT") {

    private static final long serialVersionUID = 1L;

    public void childrenTerminated(ScopeInstance scope) {
      EndState.enterTerminated(scope);
    }
  };

  protected TerminatingState(String name) {
    super(name);
  }

  public static void enterTerminating(ScopeInstance scope) {
    scope.setState(TERMINATING_PRIMARY);
    scope.terminateChildren();
  }
}
