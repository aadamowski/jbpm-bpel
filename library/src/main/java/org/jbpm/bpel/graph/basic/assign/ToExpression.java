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
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/07/22 05:57:25 $
 */
public class ToExpression extends To {

  private Expression expression;

  private static final long serialVersionUID = 1L;

  private static final Log log = LogFactory.getLog(ToExpression.class);

  public void assign(Token token, Object value) {
    log.debug("assigning " + expression + " for " + token);
    expression.getEvaluator().assign(token, value);
  }

  public Expression getExpression() {
    return expression;
  }

  public void setExpression(Expression query) {
    this.expression = query;
  }
}
