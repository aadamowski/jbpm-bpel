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
package org.jbpm.bpel.graph.exe;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.jbpm.graph.exe.Token;

import org.jbpm.bpel.alarm.AlarmAction;
import org.jbpm.bpel.graph.basic.Receive;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.LinkDefinition;
import org.jbpm.bpel.graph.struct.Pick;
import org.jbpm.bpel.integration.def.ReceiveAction;

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/07/26 00:39:11 $
 */
public class PickExeTest extends AbstractExeTestCase {

  Pick pick;
  Set activities;
  
  static final String initialXml = "<pick createInstance='yes'>"
      + "	<onMessage partnerLink='aPartner' operation='o'>"
      + "	<receive name='a' partnerLink='aPartner' operation='o'/>"
      + "	</onMessage>"
      + "	<onMessage partnerLink='aPartner' operation='o'>"
      + "	<receive name='b' partnerLink='aPartner' operation='o'/>"
      + "	</onMessage>"
      + "</pick>";

  public void initActivities() {
    activities = new HashSet();
    activities.addAll(pick.getNodes());

    Token root = pi.getRootToken();
    for (Iterator it = activities.iterator(); it.hasNext();) {
      Activity activity = ((Activity) it.next());
      TestLink link = new TestLink(activity.getName());
      activity.addSource(link);
      link.createInstance(root);
    }
  }

  public void testInitialFirstReceiver() throws Exception {
    pick = (Pick) readActivity(initialXml, true);
    plugInitial(pick);
    initActivities();

    ReceiveAction firstReceiver = (ReceiveAction) pick.getOnMessages().get(0);
    validateInitial(firstReceiver);
  }

  public void testInitialSecondReceiver() throws Exception {
    pick = (Pick) readActivity(initialXml, true);
    plugInitial(pick);
    initActivities();

    ReceiveAction secondReceiver = (ReceiveAction) pick.getOnMessages().get(0);
    validateInitial(secondReceiver);
  }

  private void validateInitial(ReceiveAction receiveAction) {
    Token token = executeInitial(receiveAction);
    Receive pickedReceive = (Receive) pick.getActivity(receiveAction);
    assertReceiveAndComplete(token, pickedReceive);
    assertReceiversDisabled(token, receiveAction);
    assertLinksDetermined(token, pickedReceive);
  }

  static final String xml = "<pick>"
      + "	<onMessage partnerLink='aPartner' operation='o'>"
      + "	 <receive name='a' partnerLink='aPartner' operation='o'/>"
      + "	</onMessage>"
      + "	<onMessage partnerLink='aPartner' operation='o'>"
      + "	 <receive name='b' partnerLink='aPartner' operation='o'/>"
      + "	</onMessage>"
      + "	<onAlarm>"
      + "  <for>$f</for>"
      + "	 <receive name='c' partnerLink='aPartner' operation='o'/>"
      + "	</onAlarm>"
      + "	<onAlarm>"
      + "  <until>$u</until>"
      + "	 <receive name='d' partnerLink='aPartner' operation='o'/>"
      + "	</onAlarm>"
      + "</pick>";

  public void testInnerFirstReceiver() throws Exception {
    pick = (Pick) readActivity(xml, false);
    plugInner(pick);
    initActivities();

    ReceiveAction firstReceiver = (ReceiveAction) pick.getOnMessages().get(0);
    validateInner(firstReceiver);
  }

  public void testInnerSecondReceiver() throws Exception {
    pick = (Pick) readActivity(xml, false);
    plugInner(pick);
    initActivities();

    ReceiveAction secondReceiver = (ReceiveAction) pick.getOnMessages().get(1);
    validateInner(secondReceiver);
  }

  public void testInnerFirstAlarm() throws Exception {
    pick = (Pick) readActivity(xml, false);
    plugInner(pick);
    initActivities();

    AlarmAction alarm0 = (AlarmAction) pick.getOnAlarms().get(0);
    validateInner(alarm0);
  }

  public void testInnerSecondAlarm() throws Exception {
    pick = (Pick) readActivity(xml, false);
    plugInner(pick);
    initActivities();

    AlarmAction alarm1 = (AlarmAction) pick.getOnAlarms().get(1);
    validateInner(alarm1);
  }

  private void validateInner(ReceiveAction receiveAction) {
    Token token = executeInner();
    Receive pickedReceive = (Receive) pick.getActivity(receiveAction);
    assertEquals(pick.getBegin(), token.getNode());
    pick.messageReceived(receiveAction, token);
    assertSame(pickedReceive, token.getNode());

    assertReceiveAndComplete(token, pickedReceive);
    assertReceiversDisabled(token, receiveAction);
    assertAlarmsDisabled(token, null);
    assertLinksDetermined(token, pickedReceive);
  }

  private void validateInner(AlarmAction alarmAction) {
    Token token = executeInner();
    assertEquals(pick.getBegin(), token.getNode());
    alarmAction.getTimeDrivenActivity().timerFired(alarmAction, token);
    Receive pickedReceive = (Receive) pick.getActivity(alarmAction);

    assertReceiveAndComplete(token, pickedReceive);
    assertReceiversDisabled(token, null);
    assertAlarmsDisabled(token, alarmAction);
    assertLinksDetermined(token, pickedReceive);
  }

  private void assertAlarmsDisabled(Token token, AlarmAction alarmAction) {
    for (Iterator it = pick.getOnAlarms().iterator(); it.hasNext();) {
      AlarmAction other = (AlarmAction) it.next();
      if (other != alarmAction)
        assertAlarmDisabled(token, other);
    }
  }

  private void assertReceiversDisabled(Token token, ReceiveAction event) {
    for (Iterator it = pick.getOnMessages().iterator(); it.hasNext();) {
      ReceiveAction other = (ReceiveAction) it.next();
      if (other != event)
        assertReceiverDisabled(token, other);
    }
  }

  private void assertLinksDetermined(Token token, Activity eventActivity) {
    for (Iterator it = activities.iterator(); it.hasNext();) {
      Activity activity = ((Activity) it.next());
      LinkDefinition source = activity.getSource(activity.getName());
      Boolean reached = source.getInstance(token).getStatus();

      if (activity.equals(eventActivity)) {
        assertNotNull(reached);
      }
      else {
        assertEquals(Boolean.FALSE, reached);
      }
    }
  }
}