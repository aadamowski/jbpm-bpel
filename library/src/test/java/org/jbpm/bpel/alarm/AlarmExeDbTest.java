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

import java.util.Date;
import java.util.List;

import org.hibernate.criterion.Restrictions;

import org.jbpm.bpel.persistence.db.AbstractDbTestCase;
import org.jbpm.bpel.sublang.def.Expression;
import org.jbpm.bpel.xml.util.DatatypeUtil;
import org.jbpm.bpel.xml.util.Duration;
import org.jbpm.graph.exe.Token;
import org.jbpm.job.Timer;

/**
 * @author Juan Cantu
 * @version $Revision$ $Date: 2008/02/01 05:45:29 $
 */
public class AlarmExeDbTest extends AbstractDbTestCase {

  private AlarmAction alarmAction = new AlarmAction();
  private Token token = new Token();

  protected void setUp() throws Exception {
    super.setUp();

    session.save(alarmAction);
    session.save(token);
  }

  public void testCreateTimer() {
    Expression until = new Expression();
    until.setText("'2101-04-07'");

    Expression repeatEvery = new Expression();
    repeatEvery.setText("'P3D'");

    alarmAction.setUntil(until);
    alarmAction.setRepeatEvery(repeatEvery);

    alarmAction.createTimer(token, AlarmAction.getSchedulerService(jbpmContext));

    List timers = findTimersByToken(token);
    assertEquals(1, timers.size());

    Timer timer = (Timer) timers.get(0);
    assertSame(alarmAction, timer.getAction());

    Date dueDate = DatatypeUtil.parseDateTime("2101-04-07").getTime();
    assertEquals(dueDate, timer.getDueDate());

    String repeat = (Duration.valueOf("P3D").getTimeInMillis() / 1000L)
        + " seconds";
    assertEquals(repeat, timer.getRepeat());
  }

  public void testCancelTimer() {
    Expression _for = new Expression();
    _for.setText("'PT1H'");

    alarmAction.setFor(_for);

    alarmAction.createTimer(token, AlarmAction.getSchedulerService(jbpmContext));

    List timers = findTimersByToken(token);
    assertEquals(1, timers.size());

    Timer timer = (Timer) timers.get(0);
    assertSame(alarmAction, timer.getAction());

    alarmAction.deleteTimer(token, AlarmAction.getSchedulerService(jbpmContext));

    timers = findTimersByToken(token);
    assertTrue(timers.isEmpty());
  }

  private List findTimersByToken(Token token) {
    return session.createCriteria(Timer.class).add(
        Restrictions.eq("token", token)).list();
  }
}
