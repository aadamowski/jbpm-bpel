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

import org.jbpm.bpel.graph.exe.ScopeState;
import org.jbpm.bpel.graph.scope.Handler;

/**
 * @author Juan Cantu
 * @version $Revision$ $Date: 2008/02/04 14:35:48 $
 */
public class TerminatingPrimaryActivityOnTerminateTest extends AbstractStateTestCase {

  public ScopeState getState() {
    return TerminatingState.TERMINATING_PRIMARY;
  }

  public void testChildrenTerminated() {
    scopeInstance.getState().childrenTerminated(scopeInstance);
    // assertEquals(TerminatingState.TERMINATING_IMPLICITLY, scope.getState());
    // has to be terminated since compensation is disabled when persistence is
    // not available
    assertEquals(EndState.TERMINATED, scopeInstance.getState());
  }

  public void testChildrenTerminatedWithHandler() {
    Handler handler = new Handler();
    handler.setActivity(handlerLog);
    scope.setTerminationHandler(handler);

    scopeInstance.getState().childrenTerminated(scopeInstance);
    assertEquals(TerminatingState.EXPLICIT, scopeInstance.getState());
    assertTrue(handlerLog.executed);
  }

  public void testFaulted() {
    scopeInstance.faulted(null);
    assertEquals(TerminatingState.TERMINATING_PRIMARY, scopeInstance.getState());
  }

  public void testChildrenCompensated() {
    try {
      scopeInstance.getState().childrenCompensated(scopeInstance);
      fail("childrenCompensated cannot be invoked at this state");
    }
    catch (IllegalStateException e) {
      // expected exception
    }
  }
}
