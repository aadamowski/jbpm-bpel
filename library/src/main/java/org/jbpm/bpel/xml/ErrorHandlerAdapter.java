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

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import org.jbpm.jpdl.xml.Problem;

/**
 * Adapts a problem handler to the SAX error handling interface.
 * @author Juan Cantu
 * @version $Revision$ $Date: 2007/05/24 13:48:43 $
 */
class ErrorHandlerAdapter implements ErrorHandler {

  private final ProblemHandler handler;

  ErrorHandlerAdapter(ProblemHandler handler) {
    this.handler = handler;
  }

  public void error(SAXParseException pe) {
    Problem problem = new Problem(Problem.LEVEL_ERROR, pe.getMessage(), pe.getException());
    fillLocationData(problem, pe);
    handler.add(problem);
  }

  public void fatalError(SAXParseException pe) {
    Problem problem = new Problem(Problem.LEVEL_FATAL, pe.getMessage(), pe.getException());
    fillLocationData(problem, pe);
    handler.add(problem);
  }

  public void warning(SAXParseException pe) {
    Problem problem = new Problem(Problem.LEVEL_WARNING, pe.getMessage(), pe.getException());
    fillLocationData(problem, pe);
    handler.add(problem);
  }

  private void fillLocationData(Problem problem, SAXParseException pe) {
    // resource
    String resource = pe.getPublicId();
    if (resource == null)
      resource = pe.getSystemId();
    problem.setResource(resource);

    // line
    problem.setLine(new Integer(pe.getLineNumber()));
  }
}