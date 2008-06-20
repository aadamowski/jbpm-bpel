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

import java.util.List;
import java.util.Set;

import org.jbpm.bpel.alarm.AlarmAction;
import org.jbpm.bpel.graph.basic.Empty;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.integration.def.ReceiveAction;
import org.jbpm.bpel.persistence.db.AbstractDbTestCase;
import org.jbpm.bpel.xml.BpelConstants;

/**
 * @author Juan Cantu
 * @version $Revision$ $Date: 2007/10/13 02:53:26 $
 */
public class PickDbTest extends AbstractDbTestCase {

  private BpelProcessDefinition processDefinition;
  private Pick pick = new Pick(PICK_NAME);

  private static final String PICK_NAME = "p";
  private static final String ONMESSAGE_NAME = "om";
  private static final String ONALARM_NAME = "oa";

  protected void setUp() throws Exception {
    super.setUp();

    Activity onMessageActivity = new Empty(ONMESSAGE_NAME);
    Activity onAlarmActivity = new Empty(ONALARM_NAME);

    pick.addNode(onMessageActivity);
    pick.addNode(onAlarmActivity);

    // process, create after opening jbpm context
    processDefinition = new BpelProcessDefinition("pd", BpelConstants.NS_EXAMPLES);
    processDefinition.getGlobalScope().setActivity(pick);
  }

  public void testEvents() {
    // prepare persistent objects
    AlarmAction alarmAction = new AlarmAction();
    ReceiveAction receiveAction = new ReceiveAction();

    // pick
    pick.setOnMessage((Activity) pick.getNode(ONMESSAGE_NAME), receiveAction);
    pick.setOnAlarm((Activity) pick.getNode(ONALARM_NAME), alarmAction);

    // save objects and load them back
    processDefinition = saveAndReload(processDefinition);
    pick = getPick(processDefinition);
    receiveAction = (ReceiveAction) pick.getOnMessages().get(0);
    alarmAction = (AlarmAction) pick.getOnAlarms().get(0);

    // verify retrieved objects
    assertEquals(ONMESSAGE_NAME, pick.getActivity(receiveAction).getName());
    assertEquals(ONALARM_NAME, pick.getActivity(alarmAction).getName());
  }

  public void testConnections() {
    // save objects and load them back
    processDefinition = saveAndReload(processDefinition);
    pick = getPick(processDefinition);

    List beginTransitions = pick.getBegin().getLeavingTransitions();
    Set endTransitions = pick.getEnd().getArrivingTransitions();

    Activity onMessageActivity = (Activity) pick.getNode(ONMESSAGE_NAME);
    Activity onAlarmActivity = (Activity) pick.getNode(ONALARM_NAME);

    // verify retrieved objects
    assertTrue(beginTransitions.contains(onMessageActivity.getDefaultArrivingTransition()));
    assertTrue(endTransitions.contains(onMessageActivity.getDefaultLeavingTransition()));

    assertTrue(beginTransitions.contains(onAlarmActivity.getDefaultArrivingTransition()));
    assertTrue(endTransitions.contains(onAlarmActivity.getDefaultLeavingTransition()));
  }

  private Pick getPick(BpelProcessDefinition processDefinition) {
    Activity pick = processDefinition.getGlobalScope().getActivity();
    // reacquire proxy of the proper type
    return (Pick) session.load(Pick.class, new Long(pick.getId()));
  }
}