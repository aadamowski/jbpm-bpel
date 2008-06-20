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
 * @version $Revision$ $Date: 2008/02/01 05:43:07 $
 */
public class CompletedTest extends AbstractStateTestCase {

  public ScopeState getState() {
    return EndState.COMPLETED;
  }

  public void testCompensate() {
    scopeInstance.compensate(parentInstance);

    assertTrue(parentInstance.childCompensated);
    assertFalse(handlerLog.executed);
    // compensation won't work since persistence is disabled
    /*
     * assertEquals(CompensatingState.COMPENSATING_IMPLICITLY, scope.getState();
     * assertChildrenCompensated();
     */
    assertEquals(EndState.COMPENSATED, scopeInstance.getState());
  }

  public void testCompensateWithHandler() {
    Handler handler = new Handler();
    handler.setActivity(handlerLog);
    scope.setCompensationHandler(handler);

    scopeInstance.compensate(scopeInstance.getParent());
    assertTrue(handlerLog.executed);
    assertEquals(CompensatingState.EXPLICIT,
        scopeInstance.getState());
  }

}
