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

import org.jbpm.bpel.graph.basic.Empty;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.CompositeActivity;

/**
 * @author Juan Cant�
 * @version $Revision$ $Date: 2007/05/31 12:55:12 $
 */
public class EmptyReader extends ActivityReader {

  /**
   * Loads the activity properties from the given DOM element
   */
  public Activity read(Element activityElem, CompositeActivity parent) {
    Empty empty = new Empty();
    readStandardProperties(activityElem, empty, parent);
    readEmpty(activityElem, empty);
    return empty;
  }

  public void readEmpty(Element emptyElem, Empty empty) {
    validateNonInitial(emptyElem, empty);
  }
}