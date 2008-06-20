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

import org.jbpm.jpdl.xml.Problem;

/**
 * An implementation of the {@link ProblemHandler} interface that simply counts
 * the reported problems.
 * @author Juan Cantu
 * @version $Revision$ $Date: 2007/05/24 13:48:43 $
 */
public class ProblemCounter extends AbstractProblemHandler {

  private int problemCount;

  /**
   * {@inheritDoc}
   */
  public void add(Problem problem) {
    problemCount++;
    logProblem(problem);
  }

  /**
   * {@inheritDoc}
   */
  public int getProblemCount() {
    return problemCount;
  }
}
