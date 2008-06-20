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

import javax.xml.namespace.QName;

import org.jbpm.bpel.graph.exe.FaultInstance;
import org.jbpm.bpel.graph.exe.ScopeState;
import org.jbpm.bpel.graph.scope.Catch;

/**
 * @author Juan Cantu
 * @version $Revision$ $Date: 2008/02/01 05:43:07 $
 */
public class TerminatingPrimaryActivityOnFaultTest extends
    AbstractStateTestCase {

  public ScopeState getState() {
    return FaultingState.TERMINATING_PRIMARY;
  }

  public void testChildrenTerminated() {
    scopeInstance.setFaultInstance(new FaultInstance());
    scopeInstance.getState().childrenTerminated(scopeInstance);

    // assertEquals(FaultingState.FAULTING_IMPLICITLY, scope.getState());
    // has to be terminated since compensation is disabled when persistence is
    // not available
    assertEquals(EndState.FAULTED, scopeInstance.getState());
  }

  public void testChildrenTerminatedWithHandler() {
    QName faultName = new QName("aFault");
    Catch catcher = new Catch();
    catcher.setFaultName(faultName);
    catcher.setActivity(handlerLog);
    scope.addCatch(catcher);
    FaultInstance faultInstance = new FaultInstance(faultName);
    scopeInstance.setFaultInstance(faultInstance);

    scopeInstance.getState().childrenTerminated(scopeInstance);

    assertEquals(FaultingState.EXPLICIT, scopeInstance.getState());
    assertTrue(this.handlerLog.executed);
  }

  public void testChildrenCompensated() {
    try {
      scopeInstance.getState().childrenCompensated(scopeInstance);
      fail("compensate can't be invoked at this state");
    }
    catch (IllegalStateException e) {
      // expected exception
    }
  }

  public void testTerminate() {
    ScopeState oldState = scopeInstance.getState();
    scopeInstance.terminate();

    assertEquals(oldState, scopeInstance.getState());
  }
}
