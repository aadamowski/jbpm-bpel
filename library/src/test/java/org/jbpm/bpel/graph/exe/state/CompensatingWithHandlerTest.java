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

/**
 * @author Juan Cantu
 * @version $Revision$ $Date: 2008/02/01 05:43:07 $
 */
public class CompensatingWithHandlerTest extends AbstractStateTestCase {

  public ScopeState getState() {
    return CompensatingState.EXPLICIT;
  }

  public void testFaulted() {
    scopeInstance.faulted(null);
    assertEquals(CompensatingState.TERMINATING_HANDLER,
        scopeInstance.getState());
    assertChildrenTerminated();
  }

  public void testFaultedAtScope() {
    scopeInstance.faulted(null);
    assertEquals(CompensatingState.TERMINATING_HANDLER,
        scopeInstance.getState());
    assertChildrenTerminated();
    assertFalse(parentInstance.childFaulted);
  }

  public void testCompleted() {
    scopeInstance.setCompensator(parentInstance);
    scopeInstance.createHandlerToken();
    scopeInstance.completed();

    assertEquals(EndState.COMPENSATED, scopeInstance.getState());
    assertTrue(parentInstance.childCompensated);
  }

  public void testCompletedAtScope() {
    scopeInstance.setCompensator(parentInstance);
    scopeInstance.createHandlerToken();
    scopeInstance.completed();

    assertEquals(EndState.COMPENSATED, scopeInstance.getState());
    assertTrue(parentInstance.childCompensated);
  }

  public void testChildrenCompensated() {
    try {
      scopeInstance.getState().childrenCompensated(scopeInstance);
      fail("childrenCompensated can't be invoked at this state");
    }
    catch (IllegalStateException e) {
      // expected exception
    }
  }
}
