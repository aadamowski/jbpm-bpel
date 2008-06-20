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
import org.jbpm.graph.exe.Token;

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2008/02/04 14:35:49 $
 */
public class EndState extends ScopeState {

  private static final long serialVersionUID = 1L;

  public static final ScopeState TERMINATED = new EndState("ET");

  public static final ScopeState COMPENSATED = new EndState("EC");

  public static final ScopeState COMPLETED = new EndState("EA") {

    private static final long serialVersionUID = 1L;

    public void compensate(ScopeInstance scopeInstance) {
      CompensatingState.enterCompensating(scopeInstance);
    }
  };

  public static final ScopeState FAULTED = new EndState("EF") {

    private static final long serialVersionUID = 1L;

    public void terminate(ScopeInstance scopeInstance) {
      /*
       * in case a Terminate signal is sent to a nested scope that has already faulted internally,
       * the Terminate signal is ignored; the scope will eventually send either a Faulted or an
       * Exited signal to the parent
       */
    }
  };

  public static final ScopeState EXITED = new EndState("EX");

  private EndState(String name) {
    super(name);
  }

  public boolean isEnd() {
    return true;
  }

  public static void enterCompleted(ScopeInstance scopeInstance) {
    scopeInstance.setState(COMPLETED);
    scopeInstance.proceed();
  }

  public static void enterFaulted(ScopeInstance scopeInstance) {
    scopeInstance.setState(FAULTED);

    ScopeInstance parentInstance = scopeInstance.getParent();
    if (parentInstance != null) {
      // end scope token (do not verify parent termination)
      scopeInstance.getToken().end(false);
      // rethrow fault to parent scope
      parentInstance.faulted(scopeInstance.getFaultInstance());
    }
    else {
      // end global scope token (this will end the process instance)
      scopeInstance.getToken().end();
    }
  }

  public static void enterCompensated(ScopeInstance scopeInstance) {
    // this is never invoked on the global scope, so don't bother to check
    scopeInstance.setState(COMPENSATED);
    scopeInstance.getCompensator().scopeCompensated(scopeInstance);
  }

  public static void enterTerminated(ScopeInstance scopeInstance) {
    // this is never invoked on the global scope, so don't bother to check
    scopeInstance.setState(TERMINATED);
    scopeInstance.getParent().scopeTerminated(scopeInstance);
  }

  public static void enterExited(ScopeInstance scopeInstance) {
    scopeInstance.setState(EXITED);
    scopeInstance.proceed();
  }
}
