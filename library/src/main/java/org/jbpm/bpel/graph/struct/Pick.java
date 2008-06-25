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
package org.jbpm.bpel.graph.struct;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.JbpmContext;
import org.jbpm.bpel.alarm.AlarmAction;
import org.jbpm.bpel.alarm.TimeDrivenActivity;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.BpelVisitor;
import org.jbpm.bpel.integration.IntegrationService;
import org.jbpm.bpel.integration.def.InboundMessageActivity;
import org.jbpm.bpel.integration.def.ReceiveAction;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;
import org.jbpm.scheduler.SchedulerService;

/**
 * Blocks for one of several possible messages to arrive or for a time-out to occur. When one of
 * these triggers occurs, the associated child activity is performed.
 * @author Juan Cantú
 * @version $Revision$ $Date: 2008/02/01 05:43:08 $
 */
public class Pick extends StructuredActivity implements InboundMessageActivity, TimeDrivenActivity {

  private List onMessages = new ArrayList();
  private List onAlarms = new ArrayList();

  private boolean createInstance;

  private static final Log log = LogFactory.getLog(Pick.class);
  private static final long serialVersionUID = 1L;

  public Pick() {
  }

  public Pick(String name) {
    super(name);
  }

  // behaviour methods
  // ///////////////////////////////////////////////////////////////////////////

  public void execute(ExecutionContext exeContext) {
    Token token = exeContext.getToken();
    JbpmContext jbpmContext = exeContext.getJbpmContext();
    // prepare message receivers
    IntegrationService integrationService = ReceiveAction.getIntegrationService(jbpmContext);
    integrationService.receive(onMessages, token);
    // prepare alarms
    SchedulerService schedulerService = AlarmAction.getSchedulerService(jbpmContext);
    for (int i = 0, n = onAlarms.size(); i < n; i++) {
      AlarmAction alarmAction = (AlarmAction) onAlarms.get(i);
      alarmAction.createTimer(token, schedulerService);
    }
  }

  public void terminate(ExecutionContext exeContext) {
    Token token = exeContext.getToken();
    JbpmContext jbpmContext = exeContext.getJbpmContext();
    // end message receivers and alarms
    cancelReceptions(null, token, jbpmContext);
    deleteTimers(null, token, jbpmContext);
  }

  public void messageReceived(ReceiveAction messageTarget, Token token) {
    JbpmContext jbpmContext = JbpmContext.getCurrentJbpmContext();
    // end message receivers and alarms
    cancelReceptions(messageTarget, token, jbpmContext);
    deleteTimers(null, token, jbpmContext);
    // perform the associated activity
    performActivity(getActivity(messageTarget), new ExecutionContext(token));
  }

  public void timerFired(AlarmAction timerTarget, Token token) {
    JbpmContext jbpmContext = JbpmContext.getCurrentJbpmContext();
    // end message receivers and alarms
    cancelReceptions(null, token, jbpmContext);
    deleteTimers(timerTarget, token, jbpmContext);
    // perform the associated activity
    performActivity(getActivity(timerTarget), new ExecutionContext(token));
  }

  protected void cancelReceptions(ReceiveAction messageTarget, Token token, JbpmContext jbpmContext) {
    IntegrationService integrationService = ReceiveAction.getIntegrationService(jbpmContext);
    for (int i = 0, n = onMessages.size(); i < n; i++) {
      ReceiveAction onMessage = (ReceiveAction) onMessages.get(i);
      if (onMessage != messageTarget)
        integrationService.cancelReception(onMessage, token);
    }
  }

  protected void deleteTimers(AlarmAction timerTarget, Token token, JbpmContext jbpmContext) {
    SchedulerService schedulerService = AlarmAction.getSchedulerService(jbpmContext);
    for (int i = 0, n = onAlarms.size(); i < n; i++) {
      AlarmAction onAlarm = (AlarmAction) onAlarms.get(i);
      if (onAlarm != timerTarget)
        onAlarm.deleteTimer(token, schedulerService);
    }
  }

  protected void performActivity(Activity selectedActivity, ExecutionContext exeContext) {
    List activities = getActivities();
    Token token = exeContext.getToken();

    for (int i = 0, n = activities.size(); i < n; i++) {
      Activity activity = (Activity) activities.get(i);
      if (!selectedActivity.equals(activity))
        activity.eliminatePath(token);
    }

    log.debug("selected branch: " + selectedActivity + " for " + token);
    getBegin().leave(exeContext, selectedActivity.getDefaultArrivingTransition());
  }

  // children management
  // /////////////////////////////////////////////////////////////////////////////

  protected void addActivity(Activity activity) {
    super.addActivity(activity);
    onMessages.add(null);

    // if there are alarms, move the activity at the last receptor index
    int alarmCount = onAlarms.size();
    if (alarmCount > 0) {
      List activities = getActivities();
      int receptorIndex = onMessages.size() - 1;
      activities.remove(receptorIndex + alarmCount);
      activities.add(receptorIndex, activity);
    }
  }

  protected void removeActivity(Activity activity) {
    int index = getActivities().indexOf(activity);
    if (index < onMessages.size())
      onMessages.remove(index);
    else
      onAlarms.remove(index - onMessages.size());

    super.removeActivity(activity);
  }

  // event properties
  // /////////////////////////////////////////////////////////////////////////////

  public List getOnMessages() {
    return onMessages;
  }

  public void setOnMessage(Activity activity, ReceiveAction onMessage) {
    List activities = getActivities();
    int activityIndex = activities.indexOf(activity);
    if (activityIndex == -1) {
      throw new IllegalArgumentException("cannot set message event for non-member activity: "
          + activity);
    }
    // match the positions of the receiver and the activity
    if (activityIndex < onMessages.size()) {
      onMessages.set(activityIndex, onMessage);
    }
    else {
      onAlarms.remove(activityIndex - onMessages.size());
      activities.remove(activityIndex);
      activities.add(onMessages.size(), activity);
      onMessages.add(onMessage);
    }
    // maintain the bidirectional association
    onMessage.setInboundMessageActivity(this);
  }

  public Activity getActivity(ReceiveAction onMessage) {
    return (Activity) getActivities().get(onMessages.indexOf(onMessage));
  }

  public List getOnAlarms() {
    return onAlarms;
  }

  public void setOnAlarm(Activity activity, AlarmAction onAlarm) {
    List activities = getActivities();
    int activityIndex = activities.indexOf(activity);
    if (activityIndex == -1) {
      throw new IllegalArgumentException("cannot set alarm event for non-member activity: "
          + activity);
    }
    // match the positions of the alarm and the activity
    if (activityIndex >= onMessages.size()) {
      int alarmIndex = activityIndex - onMessages.size();
      onAlarms.set(alarmIndex, onAlarm);
    }
    else {
      onMessages.remove(activityIndex);
      onAlarms.add(onAlarm);
      activities.remove(activityIndex);
      activities.add(activity);
    }
    // mantain the bidirectional association
    onAlarm.setTimeDrivenActivity(this);
  }

  public Activity getActivity(AlarmAction onAlarm) {
    return (Activity) getActivities().get(onMessages.size() + onAlarms.indexOf(onAlarm));
  }

  // Pick properties
  // ///////////////////////////////////////////////////////////////////////////

  public boolean isCreateInstance() {
    return createInstance;
  }

  public void setCreateInstance(boolean createInstance) {
    this.createInstance = createInstance;
  }

  public void accept(BpelVisitor visitor) {
    visitor.visit(this);
  }
}