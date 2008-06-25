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

import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.CompositeActivity;
import org.jbpm.bpel.graph.struct.If;
import org.jbpm.bpel.sublang.def.Expression;
import org.jbpm.bpel.xml.util.XmlUtil;

/**
 * Encapsulates the logic to create and connect process elements that make up
 * the <i>switch</i> structure.
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/05/31 12:55:12 $
 */
public class SwitchReader extends ActivityReader {

  public Activity read(Element activityElem, CompositeActivity parent) {
    If _if = new If();
    readStandardProperties(activityElem, _if, parent);
    readSwitch(activityElem, _if);
    return _if;
  }

  protected void readSwitch(Element switchElem, If _if) {
    validateNonInitial(switchElem, _if);

    // read conditional branches (case)
    Iterator caseElemIt = XmlUtil.getElements(switchElem, BpelConstants.NS_BPEL, "case");
    while (caseElemIt.hasNext()) {
      Element caseElem = (Element) caseElemIt.next();

      // condition
      Element conditionElem = XmlUtil.getElement(caseElem, BpelConstants.NS_BPEL,
          BpelConstants.ELEM_CONDITION);

      // activity
      Element activityElem = bpelReader.getActivityElement(caseElem);
      Activity activity = bpelReader.readActivity(activityElem, _if);

      Expression condition = bpelReader.readExpression(conditionElem, _if);
      _if.setCondition(activity, condition);
    }

    // read default branch (otherwise)
    Element otherwiseElem = XmlUtil.getElement(switchElem, BpelConstants.NS_BPEL, "otherwise");

    if (otherwiseElem != null) {
      Element activityElem = bpelReader.getActivityElement(otherwiseElem);
      Activity _else = bpelReader.readActivity(activityElem, _if);
      _if.setElse(_else);
    }
  }
}
