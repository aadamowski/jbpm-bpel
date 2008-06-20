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
package org.jbpm.bpel.alarm;

import java.util.Calendar;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.JbpmContext;
import org.jbpm.bpel.sublang.def.Expression;
import org.jbpm.bpel.xml.util.DatatypeUtil;
import org.jbpm.bpel.xml.util.Duration;
import org.jbpm.graph.def.Action;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;
import org.jbpm.job.Timer;
import org.jbpm.scheduler.SchedulerService;

/**
 * Defines a notification to be delivered by the jBPM scheduler to a
 * {@linkplain TimeDrivenActivity time-driven activity} at a specific instant in the future, once or
 * repeteadly.
 * @author Juan Cantú
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/07/20 21:02:25 $
 */
public class AlarmAction extends Action {

  private Expression _for;
  private Expression until;
  private Expression repeatEvery;

  private TimeDrivenActivity timeDrivenActivity;

  private static Log log = LogFactory.getLog(AlarmAction.class);

  private static final String ALARM_NAME = "a:";
  private static final long serialVersionUID = 1L;

  public void createTimer(Token token, SchedulerService scheduler) {
    Timer timer = new Timer(token);
    timer.setName(ALARM_NAME + getId());
    timer.setAction(this);

    // calculate repeat interval, if any
    Duration repeatDuration = null;
    if (repeatEvery != null) {
      // repeatEvery evaluates to a duration
      repeatDuration = DatatypeUtil.toDuration(repeatEvery.getEvaluator().evaluate(token));
      /*
       * the jBPM scheduler supports one duration unit only, we use seconds since the scheduler
       * converts any other unit to seconds anyway
       */
      long repeatDurationMs = repeatDuration.getTimeInMillis();
      // convert duration to milliseconds
      timer.setRepeat((repeatDurationMs / 1000) + " seconds");
    }

    // calculate due date
    Calendar dueDate;
    if (until != null) {
      // until evaluates to a date/dateTime
      dueDate = DatatypeUtil.toDateTime(until.getEvaluator().evaluate(token));
    }
    else {
      // calculate first duration
      Duration dueDuration;

      if (_for != null) {
        // for evaluates to a duration
        dueDuration = DatatypeUtil.toDuration(_for.getEvaluator().evaluate(token));
      }
      else {
        // repeatEvery also specifies a duration
        assert repeatDuration != null : "neither 'until', 'for' nor 'repeatEvery' is specified";
        dueDuration = repeatDuration;
      }

      // add duration to present instant to give due date
      dueDate = Calendar.getInstance();
      dueDuration.addTo(dueDate);
    }
    timer.setDueDate(dueDate.getTime());

    scheduler.createTimer(timer);
    log.debug("created timer: alarm=" + this + ", token=" + token);
  }

  public void execute(ExecutionContext exeContext) throws Exception {
    if (exeContext.getTimer() == null)
      createTimer(exeContext.getToken(), getSchedulerService(exeContext.getJbpmContext()));
    else
      timeDrivenActivity.timerFired(this, exeContext.getToken());
  }

  public void deleteTimer(Token token, SchedulerService scheduler) {
    scheduler.deleteTimersByName(ALARM_NAME + getId(), token);
    log.debug("deleted timer: alarm=" + this + ", token=" + token);
  }

  public static SchedulerService getSchedulerService(JbpmContext jbpmContext) {
    return jbpmContext.getServices().getSchedulerService();
  }

  public Expression getFor() {
    return _for;
  }

  public void setFor(Expression _for) {
    this._for = _for;
  }

  public Expression getUntil() {
    return until;
  }

  public void setUntil(Expression until) {
    this.until = until;
  }

  public Expression getRepeatEvery() {
    return repeatEvery;
  }

  public void setRepeatEvery(Expression repeatEvery) {
    this.repeatEvery = repeatEvery;
  }

  public TimeDrivenActivity getTimeDrivenActivity() {
    return timeDrivenActivity;
  }

  public void setTimeDrivenActivity(TimeDrivenActivity timeDrivenActivity) {
    this.timeDrivenActivity = timeDrivenActivity;
  }

  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);

    if (_for != null)
      builder.append("for", _for.getText());
    else if (until != null)
      builder.append("until", until.getText());

    if (repeatEvery != null)
      builder.append("repeatEvery", repeatEvery.getText());

    return builder.toString();
  }
}
