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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jbpm.jpdl.xml.Problem;

/**
 * List-based implementation of the {@link ProblemHandler} interface. In
 * addition to implementing the interface method, this class provides access to
 * the list that is used internally to store the problems.
 * @author Juan Cantu
 * @version $Revision$ $Date: 2007/05/24 13:48:43 $
 */
public class ProblemCollector extends AbstractProblemHandler {

  private final List problems = new ArrayList();
  private final String resource;

  /**
   * Constructs an empty collector with no specified resource.
   */
  public ProblemCollector() {
    resource = null;
  }

  /**
   * Constructs an empty collector with the specified resource.
   * @param resource
   */
  public ProblemCollector(String resource) {
    this.resource = resource;
  }

  /**
   * Called by XML readers to report a problem.
   * 
   * Any problem that is added to this collector with no specified
   * {@linkplain Problem#getResource() resource} will be set the resource
   * specified at {@linkplain #ProblemCollector(String) construction time}.
   * 
   * @param problem the problem just found
   */
  public void add(Problem problem) {
    if (problem.getResource() == null)
      problem.setResource(resource);

    problems.add(problem);
    logProblem(problem);
  }

  /**
   * {@inheritDoc}
   */
  public int getProblemCount() {
    return problems.size();
  }

  /**
   * Gets an unmodifiable view of the list used to store the problems.
   * @return a list of {@linkplain Problem problems}
   */
  public List getProblems() {
    return Collections.unmodifiableList(problems);
  }
}