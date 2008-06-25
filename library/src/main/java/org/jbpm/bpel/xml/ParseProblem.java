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

import org.jbpm.jpdl.xml.Problem;

import com.ibm.wsdl.util.xml.XPathUtils;

public class ParseProblem extends Problem {

  private String location;

  private static final long serialVersionUID = 1L;

  public ParseProblem(String description, Element locationElem) {
    super(Problem.LEVEL_ERROR, description);
    setLocation(locationElem);
  }

  public ParseProblem(String description, Element locationElem, Throwable exception) {
    super(Problem.LEVEL_ERROR, description, exception);
    setLocation(locationElem);
  }

  /**
   * Gets the error location.
   * @return a location path
   */
  public String getLocation() {
    return location;
  }

  /**
   * Sets the error location.
   * @param location a location path, ideally an XPath; however, this is not
   *        validated
   */
  public void setLocation(String location) {
    this.location = location;
  }

  /**
   * Calculates the XPath location path that identifies the given element, and
   * then sets it as the error location.
   * @param locationElem
   */
  public void setLocation(Element locationElem) {
    location = XPathUtils.getXPathExprFromNode(locationElem);
  }

  public String toString() {
    return super.toString() + " at " + location;
  }
}
