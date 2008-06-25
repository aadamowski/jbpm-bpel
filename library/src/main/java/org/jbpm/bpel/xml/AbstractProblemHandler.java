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
import javax.xml.transform.TransformerException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import org.jbpm.jpdl.xml.Problem;

/**
 * A skeletal implementation of the {@link ProblemHandler} interface intended to minimize the effort
 * required to implement this interface.
 * 
 * The programmer needs to extend this class and provide implementations for the
 * {@link ProblemHandler#add(Problem) add} and
 * {@link ProblemHandler#getProblemCount() getProblemCount} methods.
 * 
 * The documentation for each non-abstract method in this class describes its implementation in
 * detail.
 * 
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2008/01/30 08:15:33 $
 */
public abstract class AbstractProblemHandler implements ProblemHandler {

  private static final Log log = LogFactory.getLog(ProblemHandler.class);

  /**
   * Default constructor, for invocation by subclass constructors. Typically implicit.
   */
  protected AbstractProblemHandler() {
  }

  /**
   * Returns a SAX error handler backed by this problem handler. Warnings and errors reported
   * through the returned {@link ErrorHandler} instance will report a problem to this handler.
   * 
   * This implementation returns an {@link ErrorHandler} instance that reports
   * {@linkplain ErrorHandler#warning(SAXParseException) warnings},
   * {@linkplain ErrorHandler#error(SAXParseException) errors} and
   * {@linkplain ErrorHandler#fatalError(SAXParseException) fatal errors} by calling the (abstract)
   * {@link ProblemHandler#add(Problem) add} method.
   * 
   * @return a SAX error handler view of this problem handler
   */
  public ErrorHandler asSaxErrorHandler() {
    return new ErrorHandlerAdapter(this);
  }

  /**
   * Returns a TrAX error listener backed by this problem handler. Warnings and errors reported
   * through the returned {@link ErrorListener} instance will report the problem to this handler.
   * 
   * This implementation returns an {@link ErrorListener} instance that reports
   * {@linkplain ErrorListener#warning(TransformerException) warnings},
   * {@linkplain ErrorListener#error(TransformerException) errors} and
   * {@linkplain ErrorListener#fatalError(TransformerException) fatal errors} by calling the
   * (abstract) {@link ProblemHandler#add(Problem) add} method.
   * 
   * @return a TrAX error listener view of this problem handler
   */
  public ErrorListener asTraxErrorListener() {
    return new ErrorListenerAdapter(this);
  }

  /**
   * A utility to dump the given problem to the logging subsystem.
   * @param problem the problem to log
   */
  public static void logProblem(Problem problem) {
    StringBuffer detailBuffer = new StringBuffer();

    if (problem.getDescription() != null)
      detailBuffer.append(problem.getDescription());

    if (problem.getResource() != null) {
      detailBuffer.append(" (").append(problem.getResource());

      if (problem.getLine() != null)
        detailBuffer.append(':').append(problem.getLine());

      detailBuffer.append(')');
    }

    if (problem instanceof ParseProblem) {
      ParseProblem parseProblem = (ParseProblem) problem;
      if (parseProblem.getLocation() != null)
        detailBuffer.append(" at ").append(parseProblem.getLocation());
    }

    String detail = detailBuffer.toString();
    Throwable exception = problem.getException();

    switch (problem.getLevel()) {
    case Problem.LEVEL_INFO:
      log.info(detail, exception);
      break;
    case Problem.LEVEL_WARNING:
      log.warn(detail, exception);
      break;
    case Problem.LEVEL_ERROR:
      log.error(detail, exception);
      break;
    case Problem.LEVEL_FATAL:
      log.fatal(detail, exception);
      break;
    }
  }
}
