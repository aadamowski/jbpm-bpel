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
 * the <i>if</i> structure.
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/05/31 12:55:12 $
 */
public class IfReader extends ActivityReader {

  /**
   * Loads the activity properties from the given DOM element
   */
  public Activity read(Element activityElem, CompositeActivity parent) {
    If _if = new If();
    readStandardProperties(activityElem, _if, parent);
    readIf(activityElem, _if);
    return _if;
  }

  public void readIf(Element ifElem, If _if) {
    validateNonInitial(ifElem, _if);

    // read the first conditional branch (if)
    readBranch(ifElem, _if);

    // read the remaining conditional branches (elseIf)
    Iterator elseifElemIt = XmlUtil.getElements(ifElem, BpelConstants.NS_BPEL,
        BpelConstants.ELEM_ELSEIF);
    while (elseifElemIt.hasNext()) {
      Element elseifElem = (Element) elseifElemIt.next();
      readBranch(elseifElem, _if);
    }

    // read the default branch (else)
    Element elseElem = XmlUtil.getElement(ifElem, BpelConstants.NS_BPEL, BpelConstants.ELEM_ELSE);
    if (elseElem != null) {
      Element activityElem = bpelReader.getActivityElement(elseElem);
      Activity _else = bpelReader.readActivity(activityElem, _if);
      _if.setElse(_else);
    }
  }

  protected void readBranch(Element branchElem, If _if) {
    // activity
    Element activityElem = bpelReader.getActivityElement(branchElem);
    Activity activity = bpelReader.readActivity(activityElem, _if);

    // condition
    Element conditionElem = XmlUtil.getElement(branchElem, BpelConstants.NS_BPEL,
        BpelConstants.ELEM_CONDITION);
    Expression condition = bpelReader.readExpression(conditionElem, _if);

    _if.setCondition(activity, condition);
  }
}
