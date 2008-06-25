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
public class TerminatingCompensationHandlerTest extends AbstractStateTestCase {

  public ScopeState getState() {
    return CompensatingState.TERMINATING_HANDLER;
  }

  public void testChildrenTerminated() {
    scopeInstance.getState().childrenTerminated(scopeInstance);

    assertEquals(EndState.FAULTED, scopeInstance.getState());
  }

  public void testChildrenTerminatedAtScope() {
    scopeInstance.getState().childrenTerminated(scopeInstance);

    assertEquals(EndState.FAULTED, scopeInstance.getState());
    assertTrue(parentInstance.childFaulted);
  }

  public void testChildrenCompensated() {
    try {
      scopeInstance.getState().childrenCompensated(scopeInstance);
      fail("compensate can't be invoked at this state");
    }
    catch (IllegalStateException e) {
      // we expect this exception
    }
  }
}