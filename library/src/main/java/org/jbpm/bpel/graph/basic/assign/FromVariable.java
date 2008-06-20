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
import org.w3c.dom.Element;

import org.jbpm.bpel.BpelException;
import org.jbpm.bpel.sublang.def.VariableQuery;
import org.jbpm.bpel.variable.def.VariableDefinition;
import org.jbpm.bpel.variable.exe.MessageValue;
import org.jbpm.graph.exe.Token;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/09/12 23:20:20 $
 */
public class FromVariable extends From {

  private VariableDefinition variable;
  private String part;
  private VariableQuery query;

  private static final long serialVersionUID = 1L;

  private static final Log log = LogFactory.getLog(FromVariable.class);

  public Object extract(Token token) {
    // variable
    log.debug("extracting " + variable + " for " + token);
    Object value = variable.getValue(token);

    // part
    if (part != null) {
      // prevent access to a non-existent part
      if (!(value instanceof MessageValue))
        throw new BpelException("non-message variable does not have part: " + variable.getName());

      log.debug("extracting " + part + " for " + token);
      value = ((MessageValue) value).getPart(part);
    }

    // query
    if (query != null) {
      // prevent direct query on a message variable
      if (value instanceof MessageValue)
        throw new BpelException("illegal query on message variable: " + variable.getName());

      log.debug("evaluating " + query + " for " + token);
      value = query.getEvaluator().evaluate((Element) value, token);
    }
    return value;
  }

  public String getPart() {
    return part;
  }

  public void setPart(String part) {
    this.part = part;
  }

  public VariableQuery getQuery() {
    return query;
  }

  public void setQuery(VariableQuery query) {
    this.query = query;
  }

  public VariableDefinition getVariable() {
    return variable;
  }

  public void setVariable(VariableDefinition variable) {
    this.variable = variable;
  }
}
