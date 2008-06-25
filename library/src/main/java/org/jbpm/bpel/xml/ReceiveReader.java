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

import org.jbpm.bpel.graph.basic.Receive;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.CompositeActivity;
import org.jbpm.bpel.integration.def.ReceiveAction;

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/05/31 12:55:12 $
 */
public class ReceiveReader extends ActivityReader {

  /**
   * Loads the activity properties from the given DOM element
   */
  public Activity read(Element activityElem, CompositeActivity parent) {
    Receive receive = new Receive();
    readStandardProperties(activityElem, receive, parent);
    readReceive(activityElem, receive);
    return receive;
  }

  public void readReceive(Element receiveElem, Receive receive) {
    // receiver
    ReceiveAction receiveAction = bpelReader.readReceiveAction(receiveElem,
        receive.getCompositeActivity());
    receive.setAction(receiveAction);

    // create instance
    boolean createInstance = bpelReader.readTBoolean(
        receiveElem.getAttributeNode(BpelConstants.ATTR_CREATE_INSTANCE), Boolean.FALSE)
        .booleanValue();
    if (receive.isInitial()) {
      receive.setCreateInstance(createInstance);
    }
    else if (createInstance) {
      bpelReader.getProblemHandler().add(
          new ParseProblem("receive must be initial in order to create instances", receiveElem));
    }
  }
}
