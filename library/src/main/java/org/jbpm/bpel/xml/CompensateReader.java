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
import org.jbpm.bpel.graph.scope.Compensate;

/**
 * Translates <code>bpel:compensate</code> elements to {@link Compensate} instances.
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/11/29 10:16:30 $
 */
public class CompensateReader extends ActivityReader {

  public Activity read(Element activityElem, CompositeActivity parent) {
    /*
     * BPEL-216 allow scope attribute to appear in compensate element for compatibility with the
     * eclipse bpel designer
     */
    if (activityElem.hasAttribute("scope")) {
      // replace scope attribute with target
      activityElem.setAttribute(BpelConstants.ATTR_TARGET, activityElem.getAttribute("scope"));
      activityElem.removeAttribute("scope");
      // read as compensateScope
      ActivityReader compensateScopeReader = bpelReader.getActivityReader(BpelConstants.ELEM_COMPENSATE_SCOPE);
      return compensateScopeReader.read(activityElem, parent);
    }

    Compensate compensate = new Compensate();
    readStandardProperties(activityElem, compensate, parent);
    readCompensate(activityElem, compensate);
    return compensate;
  }

  protected void readCompensate(Element compensateElem, Compensate compensate) {
    validateNonInitial(compensateElem, compensate);
  }
}
