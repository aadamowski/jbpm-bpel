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

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;

import org.jbpm.JbpmContext;
import org.jbpm.bpel.alarm.AlarmAction;
import org.jbpm.bpel.alarm.TimeDrivenActivity;
import org.jbpm.bpel.graph.exe.ScopeInstance;
import org.jbpm.bpel.graph.exe.state.ActiveState;
import org.jbpm.graph.def.Action;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;

/**
 * @author Juan Cantu
 * @version $Revision$ $Date: 2008/02/01 05:43:07 $
 */
public class OnAlarm extends Handler implements TimeDrivenActivity {

  private static final Log log = LogFactory.getLog(OnAlarm.class);
  private static final long serialVersionUID = 1L;

  public void timerFired(AlarmAction targetAlarm, Token token) {
    ScopeInstance scopeInstance = Scope.getInstance(token);
    if (!scopeInstance.getState().equals(
        ActiveState.PRIMARY)) {
      log.debug("scope is no longer in normal processing mode, ignoring: alarm="
          + targetAlarm
          + ", scope="
          + getCompositeActivity());
      return;
    }

    // create event token and scope instance
    Token eventToken = scopeInstance.createEventToken();
    scopeInstance.getDefinition().createEventInstance(eventToken);

    // execute associated activity
    execute(new ExecutionContext(eventToken));
  }

  public AlarmAction getAlarmAction() {
    if (action == null)
      return null;

    if (action instanceof AlarmAction)
      return (AlarmAction) action;

    // acquire proxy of the proper type
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
    ((AlarmAction) action).setTimeDrivenActivity(this);
  }

  public String toString() {
    return new ToStringBuilder(this).append("alarm", action).toString();
  }
}
