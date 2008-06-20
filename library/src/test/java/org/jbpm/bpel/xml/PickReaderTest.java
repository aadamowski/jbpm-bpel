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
package org.jbpm.bpel.xml;

import java.util.List;

import javax.wsdl.Output;

import com.ibm.wsdl.OutputImpl;

import org.jbpm.bpel.alarm.AlarmAction;
import org.jbpm.bpel.graph.basic.Empty;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.struct.Pick;
import org.jbpm.bpel.integration.def.CorrelationSetDefinition;
import org.jbpm.bpel.integration.def.ReceiveAction;
import org.jbpm.bpel.variable.def.MessageType;

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/10/13 02:53:24 $
 */
public class PickReaderTest extends AbstractReaderTestCase {

  public void testCreateInstanceYes() throws Exception {
    initMessageProperties();
    String xml = "<pick createInstance='yes'>"
        + "<onMessage partnerLink='aPartner' operation='o' variable='iv'>"
        + "	<empty name='oM1'/>"
        + "</onMessage>"
        + "</pick>";

    scope.initial = true;
    Pick pick = (Pick) readActivity(xml);
    assertEquals(true, pick.isCreateInstance());
  }

  public void testCreateInstanceNo() throws Exception {
    String xml = "<pick createInstance='no'>"
        + "<onAlarm><for>$f</for><empty/></onAlarm>"
        + "</pick>";

    Pick pick = (Pick) readActivity(xml);
    assertEquals(false, pick.isCreateInstance());
  }

  public void testCreateInstanceDefault() throws Exception {
    String xml = "<pick>"
        + "<onAlarm><for>$f</for><empty/></onAlarm>"
        + "</pick>";

    Pick pick = (Pick) readActivity(xml);
    assertEquals(false, pick.isCreateInstance());
  }

  public void testOnAlarmFor() throws Exception {
    String xml = "<pick>"
        + "<onAlarm>"
        + "<for>$f</for>"
        + "<empty name='oA1'/>"
        + "</onAlarm>"
        + "</pick>";
    Pick pick = (Pick) readActivity(xml);
    AlarmAction alarmAction = ((AlarmAction) pick.getOnAlarms()
        .iterator()
        .next());
    assertEquals("$f", alarmAction.getFor().getText());
  }

  public void testOnAlarmUntil() throws Exception {
    String xml = "<pick>"
        + "<onAlarm>"
        + "<until>$u</until>"
        + "<empty name='oA1'/></onAlarm>"
        + "</pick>";

    Pick pick = (Pick) readActivity(xml);
    AlarmAction alarmAction = ((AlarmAction) pick.getOnAlarms()
        .iterator()
        .next());
    assertEquals("$u", alarmAction.getUntil().getText());
  }

  public void testOnAlarmRepeat() throws Exception {
    String xml = "<pick>"
        + "<onAlarm>"
        + "<repeatEvery>$r</repeatEvery>"
        + "<until>$u</until>"
        + "<empty name='oA1'/></onAlarm>"
        + "</pick>";

    Pick pick = (Pick) readActivity(xml);
    AlarmAction alarmAction = ((AlarmAction) pick.getOnAlarms()
        .iterator()
        .next());
    assertEquals("$r", alarmAction.getRepeatEvery().getText());
    assertEquals("$u", alarmAction.getUntil().getText());
  }

  public void testOnAlarmActivity() throws Exception {
    String xml = "<pick>"
        + "<onAlarm><for>$f</for><empty name='oA1'/></onAlarm>"
        + "</pick>";

    Pick pick = (Pick) readActivity(xml);
    List alarms = pick.getOnAlarms();
    assertEquals(1, alarms.size());
    Activity activity = pick.getActivity((AlarmAction) alarms.get(0));
    assertEquals(Empty.class, activity.getClass());
    assertEquals("oA1", ((Empty) activity).getName());
  }

  public void testOnMessageActivity() throws Exception {
    initMessageProperties();
    String xml = "<pick>"
        + "<onMessage partnerLink='aPartner' operation='o' variable='iv'>"
        + "	<empty name='oM1'/>"
        + "</onMessage>"
        + "</pick>";

    Pick pick = (Pick) readActivity(xml);
    List receivers = pick.getOnMessages();
    assertEquals(1, receivers.size());
    Activity activity = pick.getActivity((ReceiveAction) receivers.get(0));
    assertEquals(Empty.class, activity.getClass());
    assertEquals("oM1", ((Empty) activity).getName());
  }

  public void testOnMessagePartnerLink() throws Exception {
    initMessageProperties();
    String xml = "<pick>"
        + "<onMessage partnerLink='aPartner' operation='o' variable='iv'>"
        + "	<empty name='oM1'/>"
        + "</onMessage>"
        + "</pick>";

    Pick pick = (Pick) readActivity(xml);
    ReceiveAction receiveAction = (ReceiveAction) pick.getOnMessages()
        .iterator()
        .next();
    assertEquals(partnerLink, receiveAction.getPartnerLink());
  }

  public void testOnMessagePortType() throws Exception {
    initMessageProperties();
    String xml = "<pick xmlns:def='http://manufacturing.org/wsdl/purchase'>"
        + "<onMessage partnerLink='aPartner' portType='def:mpt' operation='o' variable='iv'>"
        + "	<empty name='oM1'/>"
        + "</onMessage>"
        + "</pick>";

    try {
      Pick pick = (Pick) readActivity(xml);
      assertEquals(1, pick.getOnMessages().size());
    }
    catch (Exception e) {
      fail(e.toString());
    }
  }

  public void testOnMessagePortTypeDefault() throws Exception {
    initMessageProperties();
    String xml = "<pick>"
        + "<onMessage partnerLink='aPartner' operation='o' variable='iv'>"
        + "	<empty name='oM1'/>"
        + "</onMessage>"
        + "</pick>";

    try {
      Pick pick = (Pick) readActivity(xml);
      assertEquals(1, pick.getOnMessages().size());
    }
    catch (Exception e) {
      fail(e.toString());
    }
  }

  public void testOnMessageOperation() throws Exception {
    initMessageProperties();
    String xml = "<pick>"
        + "<onMessage partnerLink='aPartner' operation='o' variable='iv'>"
        + "	<empty name='oM1'/>"
        + "</onMessage>"
        + "</pick>";
    Pick pick = (Pick) readActivity(xml);
    ReceiveAction receiveAction = (ReceiveAction) pick.getOnMessages()
        .iterator()
        .next();
    assertEquals("o", receiveAction.getOperation().getName());
  }

  public void testOnMessageVariable() throws Exception {
    initMessageProperties();
    MessageType typeInfo = (MessageType) messageVariable.getType();
    Output output = new OutputImpl();
    output.setMessage(typeInfo.getMessage());
    operation.setOutput(output);

    String xml = "<pick>"
        + "<onMessage partnerLink='aPartner' operation='o' variable='iv'>"
        + "	<empty name='oM1'/>"
        + "</onMessage>"
        + "</pick>";

    Pick pick = (Pick) readActivity(xml);
    ReceiveAction receiveAction = (ReceiveAction) pick.getOnMessages()
        .iterator()
        .next();
    assertEquals(messageVariable, receiveAction.getVariable());
  }

  public void testOnMessageCorrelations() throws Exception {
    initMessageProperties();

    CorrelationSetDefinition set = new CorrelationSetDefinition();
    set.setName("corr");
    set.addProperty(p1);

    scope.addCorrelationSet(set);

    String xml = "<pick>"
        + "<onMessage partnerLink='aPartner' operation='o' variable='iv'>"
        + "	<empty name='oM1'/>"
        + "	<correlations>"
        + "		<correlation set='corr'/> "
        + "	</correlations>"
        + "</onMessage>"
        + "</pick>";
    Pick pick = (Pick) readActivity(xml);
    ReceiveAction receiveAction = (ReceiveAction) pick.getOnMessages()
        .iterator()
        .next();

    assertNotNull(receiveAction.getCorrelations());
  }
}
