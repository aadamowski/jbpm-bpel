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

import java.util.List;

import org.jbpm.bpel.BpelException;

/**
 * Lists the problems detected in the course of reading a BPEL document.
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/11/29 10:16:30 $
 */
public class BpelParseException extends BpelException {

  private List problems;

  private static final long serialVersionUID = 1L;

  public BpelParseException(List problems) {
    super(problems.size() + " problem(s) found");
    this.problems = problems;
  }

  public List getProblems() {
    return problems;
  }
}
