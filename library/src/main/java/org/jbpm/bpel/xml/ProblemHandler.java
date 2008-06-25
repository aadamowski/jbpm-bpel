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

import org.xml.sax.ErrorHandler;

import org.jbpm.jpdl.xml.Problem;

/**
 * Problem handlers help XML document readers report errors and warnings to its clients.
 * @see BpelReader
 * @see ProcessWsdlLocator
 * @see DefinitionDescriptorReader
 * @see DeploymentDescriptorReader
 * @author Juan Cantu
 * @version $Revision$ $Date: 2007/10/13 02:53:27 $
 */
public interface ProblemHandler {

  /**
   * Called by XML readers to report a problem.
   * @param problem the problem just found
   */
  public void add(Problem problem);

  /**
   * Gets the number of problems reported so far.
   * @return the number of problems
   */
  public int getProblemCount();

  /**
   * Returns a SAX error handler backed by this problem handler. Warnings and errors reported
   * through the returned {@link ErrorHandler} instance will report the problem to this handler.
   * @return a SAX error handler view of this problem handler
   */
  public ErrorHandler asSaxErrorHandler();

  /**
   * Returns a TrAX error listener backed by this problem handler. Warnings and errors reported
   * through the returned {@link ErrorListener} instance will report the problem to this handler.
   * @return an TrAX error listener view of this problem handler
   */
  public ErrorListener asTraxErrorListener();
}
