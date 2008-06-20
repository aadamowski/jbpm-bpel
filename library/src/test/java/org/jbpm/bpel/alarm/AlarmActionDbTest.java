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

import org.jbpm.bpel.alarm.AlarmAction;
import org.jbpm.bpel.graph.basic.Wait;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.persistence.db.AbstractDbTestCase;
import org.jbpm.bpel.sublang.def.Expression;
import org.jbpm.bpel.xml.BpelConstants;

/**
 * @author Juan Cantu
 * @version $Revision$ $Date: 2007/10/13 02:53:30 $
 */
public class AlarmActionDbTest extends AbstractDbTestCase {

  BpelProcessDefinition processDefinition;
  AlarmAction alarmAction = new AlarmAction();

  protected void setUp() throws Exception {
    super.setUp();

    Wait wait = new Wait("wait");
    wait.setAction(alarmAction);

    processDefinition = new BpelProcessDefinition("pd", BpelConstants.NS_EXAMPLES);
    processDefinition.getGlobalScope().setActivity(wait);
  }

  public void testFor() {
    Expression _for = new Expression();
    _for.setText("'P2D'");

    alarmAction.setFor(_for);

    processDefinition = saveAndReload(processDefinition);
    alarmAction = getAlarm(processDefinition);

    assertEquals("'P2D'", alarmAction.getFor().getText());
  }

  public void testUntil() {
    Expression until = new Expression();
    until.setText("'2007-02-19'");

    alarmAction.setUntil(until);

    processDefinition = saveAndReload(processDefinition);
    alarmAction = getAlarm(processDefinition);

    assertEquals("'2007-02-19'", alarmAction.getUntil().getText());
  }

  public void testRepeatEvery() {
    Expression repeatEvery = new Expression();
    repeatEvery.setText("'PT5M'");

    alarmAction.setRepeatEvery(repeatEvery);

    processDefinition = saveAndReload(processDefinition);
    alarmAction = getAlarm(processDefinition);

    assertEquals("'PT5M'", alarmAction.getRepeatEvery().getText());
  }

  public void testTimeActivity() {
    processDefinition = saveAndReload(processDefinition);
    alarmAction = getAlarm(processDefinition);

    assertEquals(processDefinition.getGlobalScope().getActivity(),
        alarmAction.getTimeDrivenActivity());
  }

  private AlarmAction getAlarm(BpelProcessDefinition processDefinition) {
    Wait wait = (Wait) session.load(Wait.class, new Long(processDefinition.getGlobalScope()
        .getActivity()
        .getId()));
    return wait.getAlarmAction();
  }
}