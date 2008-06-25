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

import javax.xml.transform.ErrorListener;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;

import org.jbpm.jpdl.xml.Problem;

/**
 * Adapts a problem handler to the TrAX error listening interface.
 * @author Juan Cantu
 * @version $Revision$ $Date: 2007/05/24 13:48:43 $
 */
class ErrorListenerAdapter implements ErrorListener {

  private final ProblemHandler handler;

  ErrorListenerAdapter(ProblemHandler handler) {
    this.handler = handler;
  }

  public void error(TransformerException te) {
    Problem problem = new Problem(Problem.LEVEL_ERROR, te.getMessage(), te.getException());
    fillLocationData(problem, te.getLocator());
    handler.add(problem);
  }

  public void fatalError(TransformerException te) {
    Problem problem = new Problem(Problem.LEVEL_FATAL, te.getMessage(), te.getException());
    fillLocationData(problem, te.getLocator());
    handler.add(problem);
  }

  public void warning(TransformerException te) {
    Problem problem = new Problem(Problem.LEVEL_WARNING, te.getMessage(), te.getException());
    fillLocationData(problem, te.getLocator());
    handler.add(problem);
  }

  private void fillLocationData(Problem problem, SourceLocator locator) {
    if (locator == null)
      return;

    // resource
    String resource = locator.getPublicId();
    if (resource == null)
      resource = locator.getSystemId();
    problem.setResource(resource);

    // line
    problem.setLine(new Integer(locator.getLineNumber()));
  }
}