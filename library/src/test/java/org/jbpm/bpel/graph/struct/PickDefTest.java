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

import java.util.Collection;

import junit.framework.TestCase;

import org.jbpm.bpel.alarm.AlarmAction;
import org.jbpm.bpel.graph.basic.Empty;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.struct.Pick;
import org.jbpm.bpel.integration.def.ReceiveAction;

/**
 * @author Juan Cantu
 * @version $Revision$ $Date: 2007/07/02 23:55:05 $
 */
public class PickDefTest extends TestCase {

  Pick pick;
  Activity first;
  Activity second;

  public void setUp() {
    pick = new Pick();
    first = new Empty("first");
    second = new Empty("second");
  }

  public void testAddNode() {
    pick.addNode(first);
    assertConnected(first);
    pick.addNode(second);
    assertConnected(second);
    assertEquals(2, pick.getOnMessages().size());
  }

  public void testRemoveNode() {
    pick.addNode(first);
    pick.addNode(second);
    pick.setOnAlarm(first, new AlarmAction());
    pick.setOnMessage(second, new ReceiveAction());

    pick.removeNode(first);
    assertDisconnected(first);

    pick.removeNode(second);
    assertDisconnected(second);

    assertEquals(0, pick.getBegin().getLeavingTransitions().size());
    assertEquals(0, pick.getEnd().getArrivingTransitions().size());

    assertEquals(0, pick.getNodes().size());
    assertEquals(0, pick.getOnMessages().size());
    assertEquals(0, pick.getOnAlarms().size());
  }

  public void testOnAlarm() {
    AlarmAction firstAlarm = new AlarmAction();
    AlarmAction secondAlarm = new AlarmAction();
    pick.addNode(first);
    pick.addNode(second);
    pick.setOnAlarm(first, firstAlarm);
    pick.setOnAlarm(second, secondAlarm);

    assertEquals(first, pick.getActivity(firstAlarm));
    assertEquals(second, pick.getActivity(secondAlarm));
  }

  public void testOnMessage() {
    ReceiveAction firstReceiver = new ReceiveAction();
    ReceiveAction secondReceiver = new ReceiveAction();
    pick.addNode(first);
    pick.addNode(second);
    pick.setOnMessage(first, firstReceiver);
    pick.setOnMessage(second, secondReceiver);

    assertEquals(first, pick.getActivity(firstReceiver));
    assertEquals(second, pick.getActivity(secondReceiver));
  }

  public void testOnMessageOverride() {
    ReceiveAction receiveAction = new ReceiveAction();
    pick.addNode(first);
    pick.addNode(second);

    pick.setOnAlarm(first, new AlarmAction());
    pick.setOnMessage(first, receiveAction);

    assertEquals(first, pick.getActivity(receiveAction));
  }

  public void testOnAlarmOverride() {
    AlarmAction alarmAction = new AlarmAction();
    pick.addNode(first);
    pick.addNode(second);

    pick.setOnMessage(first, new ReceiveAction());
    pick.setOnAlarm(first, alarmAction);

    assertEquals(first, pick.getActivity(alarmAction));
  }

  private void assertConnected(Activity activity) {
    Collection transitions = pick.getBegin().getLeavingTransitions();
    assertTrue(transitions.contains(activity.getDefaultArrivingTransition()));

    transitions = pick.getEnd().getArrivingTransitions();
    assertTrue(transitions.contains(activity.getDefaultLeavingTransition()));
  }

  private void assertDisconnected(Activity activity) {
    // validate that removed activity doesn't have incoming / outgoing
    // transitions
    assertEquals(0, activity.getArrivingTransitions().size());
    assertEquals(0, activity.getLeavingTransitions().size());
  }

}
