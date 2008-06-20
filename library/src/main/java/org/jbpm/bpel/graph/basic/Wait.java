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

import org.hibernate.Session;

import org.jbpm.JbpmContext;
import org.jbpm.bpel.alarm.AlarmAction;
import org.jbpm.bpel.alarm.TimeDrivenActivity;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.BpelVisitor;
import org.jbpm.graph.def.Action;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;
import org.jbpm.scheduler.SchedulerService;

/**
 * Suspends the process instance for a given time period or until a certain
 * point in time has been reached.
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/07/02 23:55:04 $
 */
public class Wait extends Activity implements TimeDrivenActivity {

  private AlarmAction alarmAction;

  private static final long serialVersionUID = 1L;

  public Wait() {
  }

  public Wait(String name) {
    super(name);
  }

  public void terminate(ExecutionContext exeContext) {
    SchedulerService scheduler = AlarmAction.getSchedulerService(exeContext.getJbpmContext());
    alarmAction.deleteTimer(exeContext.getToken(), scheduler);
  }

  public void timerFired(AlarmAction anAlarm, Token token) {
    leave(new ExecutionContext(token));
  }

  public AlarmAction getAlarmAction() {
    if (action == null)
      return null;

    if (action instanceof AlarmAction)
      return (AlarmAction) action;

    // reacquire proxy of the proper type
    Session hbSession = JbpmContext.getCurrentJbpmContext().getSession();
    AlarmAction alarmAction = (AlarmAction) hbSession.load(AlarmAction.class,
        new Long(action.getId()));

    // update action reference
    action = alarmAction;

    return alarmAction;
  }

  public void setAction(Action action) {
    if (!(action instanceof AlarmAction))
      throw new IllegalArgumentException("not an alarm action: " + action);

    this.action = action;
    action.setName(name);
    ((AlarmAction) action).setTimeDrivenActivity(this);
  }

  public void accept(BpelVisitor visitor) {
    visitor.visit(this);
  }
}
