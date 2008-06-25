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

import java.util.Iterator;

import org.w3c.dom.Element;

import org.jbpm.bpel.alarm.AlarmAction;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.CompositeActivity;
import org.jbpm.bpel.graph.struct.Pick;
import org.jbpm.bpel.integration.def.ReceiveAction;
import org.jbpm.bpel.xml.util.XmlUtil;

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/05/31 12:55:12 $
 */
public class PickReader extends ActivityReader {

  /**
   * Loads the activity properties from the given DOM element
   */
  public Activity read(Element activityElem, CompositeActivity parent) {
    Pick pick = new Pick();
    readStandardProperties(activityElem, pick, parent);
    readPick(activityElem, pick);
    return pick;
  }

  public void readPick(Element pickElem, Pick pick) {
    // createInstance
    boolean createInstance = bpelReader.readTBoolean(
        pickElem.getAttributeNode(BpelConstants.ATTR_CREATE_INSTANCE), Boolean.FALSE)
        .booleanValue();
    boolean initial = pick.isInitial();
    if (initial) {
      pick.setCreateInstance(createInstance);
    }
    else if (createInstance) {
      bpelReader.getProblemHandler().add(
          new ParseProblem("pick must be initial in order to create instances", pickElem));
    }

    Iterator onMessageElemIt = XmlUtil.getElements(pickElem, BpelConstants.NS_BPEL,
        BpelConstants.ELEM_ON_MESSAGE);
    while (onMessageElemIt.hasNext()) {
      Element onMessageElem = (Element) onMessageElemIt.next();

      // receiver
      ReceiveAction receiveAction = bpelReader.readReceiveAction(onMessageElem, pick);

      // activity
      Element activityElem = bpelReader.getActivityElement(onMessageElem);
      Activity activity = bpelReader.readActivity(activityElem, pick);

      pick.setOnMessage(activity, receiveAction);
    }

    Iterator alarmElemIt = XmlUtil.getElements(pickElem, BpelConstants.NS_BPEL,
        BpelConstants.ELEM_ON_ALARM);
    if (!initial) {
      while (alarmElemIt.hasNext()) {
        Element onAlarmElem = (Element) alarmElemIt.next();

        // alarm
        AlarmAction alarmAction = bpelReader.readAlarmAction(onAlarmElem, pick);

        // activity
        Element activityElem = bpelReader.getActivityElement(onAlarmElem);
        Activity activity = bpelReader.readActivity(activityElem, pick);

        pick.setOnAlarm(activity, alarmAction);
      }
    }
    else if (alarmElemIt.hasNext()) {
      bpelReader.getProblemHandler().add(
          new ParseProblem("pick must not be initial in order to handle alarms", pickElem));
    }
  }
}
