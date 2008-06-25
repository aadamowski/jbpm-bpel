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
package org.jbpm.bpel.graph.scope;

import org.jbpm.bpel.alarm.AlarmAction;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.scope.OnAlarm;
import org.jbpm.bpel.graph.scope.Handler;
import org.jbpm.bpel.sublang.def.Expression;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/03/16 00:04:38 $
 */
public class OnAlarmDbTest extends AbstractHandlerDbTestCase {

  protected Handler createHandler(BpelProcessDefinition process) {
    OnAlarm onAlarm = new OnAlarm();
    process.getGlobalScope().addOnAlarm(onAlarm);
    return onAlarm;
  }

  protected Handler getHandler(BpelProcessDefinition process) {
    return (Handler) process.getGlobalScope()
        .getOnAlarms()
        .iterator()
        .next();
  }

  public void testAlarmAction() {
    // script
    String deadline = "'1982-10-04'";
    Expression script = new Expression();
    script.setText(deadline);
    // alarm
    AlarmAction alarmAction = new AlarmAction();
    alarmAction.setUntil(script);
    // handler
    OnAlarm onAlarm = (OnAlarm) handler;
    onAlarm.setAction(alarmAction);

    // save objects and load them back
    BpelProcessDefinition process = saveAndReload(onAlarm.getBpelProcessDefinition());
    onAlarm = (OnAlarm) getHandler(process);

    // verify retrieved objects
    assertEquals(deadline, onAlarm.getAlarmAction().getUntil().getText());
  }

}
