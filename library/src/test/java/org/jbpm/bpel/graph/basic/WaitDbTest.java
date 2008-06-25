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
package org.jbpm.bpel.graph.basic;

import org.jbpm.bpel.alarm.AlarmAction;
import org.jbpm.bpel.graph.def.AbstractActivityDbTestCase;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.graph.def.Action;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/08/08 11:20:32 $
 */
public class WaitDbTest extends AbstractActivityDbTestCase {

  public void testAlarmAction() {
    AlarmAction alarmAction = new AlarmAction();

    Wait wait = createWait();
    wait.setAction(alarmAction);
    String waitName = wait.getName();

    processDefinition.getGlobalScope().setActivity(wait);

    processDefinition = saveAndReload(processDefinition);
    wait = getWait(processDefinition);

    Action action = wait.getAction();
    assertEquals(waitName, action.getName());
    /*
     * when AbstractDbTestCase.newTransaction() simply ends the current transaction and begins a new
     * one, the session preserves the objects already loaded; thus, action is still an AlarmAction
     */
    // assertFalse(action instanceof AlarmAction);

    // verify proxy reacquisition
    alarmAction = wait.getAlarmAction();
    assertEquals(waitName, alarmAction.getName());

    action = wait.getAction();
    assertTrue(action instanceof AlarmAction);
  }

  private Wait getWait(BpelProcessDefinition processDefinition) {
    return (Wait) session.load(Wait.class, new Long(processDefinition.getGlobalScope()
        .getActivity()
        .getId()));
  }

  protected Activity createActivity() {
    return createWait();
  }

  private Wait createWait() {
    return new Wait("wait");
  }
}
