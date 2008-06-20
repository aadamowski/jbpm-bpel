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

import org.apache.commons.lang.ClassUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.CompositeActivity;
import org.jbpm.bpel.graph.def.LinkDefinition;
import org.jbpm.bpel.sublang.def.JoinCondition;
import org.jbpm.bpel.xml.util.XmlUtil;

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/07/20 22:35:40 $
 */
public abstract class ActivityReader {

  protected BpelReader bpelReader;

  /**
   * Default constructor, for invocation by subclass constructors. Typically
   * implicit.
   */
  protected ActivityReader() {
  }

  public abstract Activity read(Element activityElem, CompositeActivity parent);

  protected void readStandardProperties(Element activityElem, Activity activity,
      CompositeActivity parent) {
    // name
    String name = XmlUtil.getAttribute(activityElem, BpelConstants.ATTR_NAME);
    if (name == null) {
      // provide clue as to type and position
      name = generateName(activity, parent);
      activity.setUnnamed(true);
    }
    activity.setName(name);

    // suppress join failure
    Attr suppressAttr = activityElem.getAttributeNode(BpelConstants.ATTR_SUPPRESS_JOIN_FAILURE);
    activity.setSuppressJoinFailure(bpelReader.readTBoolean(suppressAttr, null));

    // links
    readTargets(activityElem, activity, parent);
    readSources(activityElem, activity, parent);

    // attach to parent
    parent.addNode(activity);
  }

  private static String generateName(Activity activity, CompositeActivity parent) {
    String activityClass = ClassUtils.getShortClassName(activity.getClass());
    StringBuffer nameBuffer = new StringBuffer(activityClass);

    // separator
    nameBuffer.append('#');

    // index
    final int baseLength = nameBuffer.length();
    String name;

    for (int i = 1; parent.hasNode(name = nameBuffer.append(i).toString()); i++)
      nameBuffer.setLength(baseLength); // remove appended number

    return name;
  }

  protected void readSources(Element activityElem, Activity activity, CompositeActivity parent) {
    Element sourcesElem = XmlUtil.getElement(activityElem, BpelConstants.NS_BPEL,
        BpelConstants.ELEM_SOURCES);

    // easy way out: no sources to read
    if (sourcesElem == null)
      return;

    Iterator sourceElemIt = XmlUtil.getElements(sourcesElem, BpelConstants.NS_BPEL,
        BpelConstants.ELEM_SOURCE);
    while (sourceElemIt.hasNext()) {
      Element sourceElem = (Element) sourceElemIt.next();

      // link name
      String linkName = sourceElem.getAttribute(BpelConstants.ATTR_LINK_NAME);

      // register source link in activity
      LinkDefinition link = parent.findLink(linkName);
      activity.addSource(link);

      // transition condition
      Element conditionElem = XmlUtil.getElement(sourceElem, BpelConstants.NS_BPEL,
          BpelConstants.ELEM_TRANSITION_CONDITION);
      if (conditionElem != null)
        link.setTransitionCondition(bpelReader.readExpression(conditionElem, parent));
    }
  }

  protected void readTargets(Element activityElem, Activity activity, CompositeActivity parent) {
    Element targetsElem = XmlUtil.getElement(activityElem, BpelConstants.NS_BPEL,
        BpelConstants.ELEM_TARGETS);

    // easy way out: no targets to read
    if (targetsElem == null)
      return;

    // targets
    Iterator targetElemIt = XmlUtil.getElements(targetsElem, BpelConstants.NS_BPEL,
        BpelConstants.ELEM_TARGET);
    while (targetElemIt.hasNext()) {
      Element targetElem = (Element) targetElemIt.next();

      // link name
      String linkName = targetElem.getAttribute(BpelConstants.ATTR_LINK_NAME);

      // register target link in activity
      activity.addTarget(parent.findLink(linkName));
    }

    // join condition
    Element conditionElem = XmlUtil.getElement(targetsElem, BpelConstants.NS_BPEL,
        BpelConstants.ELEM_JOIN_CONDITION);
    if (conditionElem != null)
      activity.setJoinCondition(readJoinCondition(conditionElem, parent));
  }

  protected JoinCondition readJoinCondition(Element conditionElem, CompositeActivity parent) {
    JoinCondition joinCondition = new JoinCondition();
    bpelReader.readExpression(conditionElem, parent, joinCondition);
    return joinCondition;
  }
  
  protected void validateNonInitial(Element activityElem, Activity activity) {
    if (activity.isInitial()) {
      bpelReader.getProblemHandler().add(
          new ParseProblem("activity cannot be initial", activityElem));
    }
  }

  public BpelReader getBpelReader() {
    return bpelReader;
  }

  public void setBpelReader(BpelReader bpelReader) {
    this.bpelReader = bpelReader;
  }
}