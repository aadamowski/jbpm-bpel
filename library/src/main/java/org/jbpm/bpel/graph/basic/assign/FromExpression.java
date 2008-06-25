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
package org.jbpm.bpel.graph.basic.assign;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.bpel.sublang.def.Expression;
import org.jbpm.graph.exe.Token;

/**
 * <code>&lt;from&gt;</code> variant that allows processes to perform simple computations on
 * properties and variables.
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/07/22 05:57:25 $
 */
public class FromExpression extends From {

  private Expression expression;

  private static final long serialVersionUID = 1L;

  private static final Log log = LogFactory.getLog(FromExpression.class);

  public Object extract(Token token) {
    log.debug("evaluating " + expression + " for " + token);
    return expression.getEvaluator().evaluate(token);
  }

  public Expression getExpression() {
    return expression;
  }

  public void setExpression(Expression expression) {
    this.expression = expression;
  }
}
