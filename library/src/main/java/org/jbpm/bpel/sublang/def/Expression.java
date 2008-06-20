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
package org.jbpm.bpel.sublang.def;

import org.jbpm.bpel.sublang.exe.ExpressionEvaluator;

/**
 * Expressions extract and combine variable data in interesting ways to control the behavior of the
 * process.
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/09/12 23:20:15 $
 */
public class Expression extends Snippet {

  private transient ExpressionEvaluator evaluator;

  private static final long serialVersionUID = 1L;

  public ExpressionEvaluator getEvaluator() {
    if (evaluator == null)
      parse();

    return evaluator;
  }

  public void parse() {
    evaluator = getEvaluatorFactory().createEvaluator(this);
  }
}