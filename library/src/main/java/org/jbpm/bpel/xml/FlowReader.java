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
import org.jbpm.bpel.graph.def.LinkDefinition;
import org.jbpm.bpel.graph.struct.Flow;
import org.jbpm.bpel.xml.util.XmlUtil;

/**
 * Encapsulates the logic to create and connect process elements that make up the <i>flow</i>
 * structure.
 * @author Juan Cantú
 * @version $Revision$ $Date: 2008/02/01 05:43:08 $
 */
public class FlowReader extends ActivityReader {

  /**
   * Loads the activity properties from the given DOM element
   */
  public Activity read(Element activityElem, CompositeActivity parent) {
    Flow flow = new Flow();
    readStandardProperties(activityElem, flow, parent);
    readFlow(activityElem, flow);
    return flow;
  }

  public void readFlow(Element flowElem, Flow flow) {
    // links
    Element linksElem = XmlUtil.getElement(flowElem, BpelConstants.NS_BPEL,
        BpelConstants.ELEM_LINKS);
    if (linksElem != null) {
      for (Iterator i = XmlUtil.getElements(linksElem, BpelConstants.NS_BPEL,
          BpelConstants.ELEM_LINK); i.hasNext();) {
        Element linkElem = (Element) i.next();
        flow.addLink(new LinkDefinition(linkElem.getAttribute(BpelConstants.ATTR_NAME)));
      }
    }

    // activities
    for (Iterator i = bpelReader.getActivityElements(flowElem); i.hasNext();) {
      Element activityElem = (Element) i.next();
      bpelReader.readActivity(activityElem, flow);
    }
  }
}
