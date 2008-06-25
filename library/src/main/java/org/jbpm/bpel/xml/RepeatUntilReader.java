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

import org.w3c.dom.Element;

import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.CompositeActivity;
import org.jbpm.bpel.graph.struct.RepeatUntil;
import org.jbpm.bpel.xml.util.XmlUtil;

/**
 * Encapsulates the logic to create and connect process elements that constitute the <i>repeatUntil</i>
 * structure.
 * @author Juan Cantú
 * @version $Revision$ $Date: 2008/01/30 08:15:33 $
 */
public class RepeatUntilReader extends ActivityReader {

  /**
   * Loads the activity properties from the given DOM element
   */
  public Activity read(Element activityElem, CompositeActivity parent) {
    RepeatUntil repeatUntil = new RepeatUntil();
    readStandardProperties(activityElem, repeatUntil, parent);
    readRepeatUntil(activityElem, repeatUntil);
    return repeatUntil;
  }

  public void readRepeatUntil(Element repeatUntilElem, RepeatUntil repeatUntil) {
    validateNonInitial(repeatUntilElem, repeatUntil);

    // activity
    Element activityElem = bpelReader.getActivityElement(repeatUntilElem);
    bpelReader.readActivity(activityElem, repeatUntil);

    // condition
    Element conditionElem = XmlUtil.getElement(repeatUntilElem, BpelConstants.NS_BPEL,
        BpelConstants.ELEM_CONDITION);
    repeatUntil.setCondition(bpelReader.readExpression(conditionElem, repeatUntil));
  }
}
