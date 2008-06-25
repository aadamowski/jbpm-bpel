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

import org.jbpm.bpel.graph.basic.Rethrow;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.CompositeActivity;

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/05/31 12:55:12 $
 */
public class RethrowReader extends ActivityReader {

  /**
   * Loads the activity properties from the given DOM element
   */
  public Activity read(Element activityElem, CompositeActivity parent) {
    Rethrow rethrow = new Rethrow();
    readStandardProperties(activityElem, rethrow, parent);
    readRethrow(activityElem, rethrow);
    return rethrow;
  }

  public void readRethrow(Element rethrowElem, Rethrow rethrow) {
    validateNonInitial(rethrowElem, rethrow);
  }
}
