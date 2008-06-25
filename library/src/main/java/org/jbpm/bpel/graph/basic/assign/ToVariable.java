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
public class ToVariable extends To {

  private VariableDefinition variable;
  private String part;
  private VariableQuery query;

  private static final long serialVersionUID = 1L;

  private static final Log log = LogFactory.getLog(ToVariable.class);

  public VariableDefinition getVariable() {
    return variable;
  }

  public void setVariable(VariableDefinition variable) {
    this.variable = variable;
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

  public void assign(Token token, Object value) {
    if (part != null) {
      log.debug("extracting " + variable + " for " + token);
      Object variableValue = variable.getValueForAssign(token);

      // prevent access to a non-existent part
      if (!(variableValue instanceof MessageValue)) {
        throw new BpelException("illegal part access on non-message variable: "
            + variable.getName());
      }

      MessageValue messageValue = (MessageValue) variableValue;
      if (query != null) {
        log.debug("extracting " + part + " for " + token);
        Element partValue = messageValue.getPartForAssign(part);

        log.debug("assigning " + query + " for " + token);
        query.getEvaluator().assign(partValue, token, value);
      }
      else {
        log.debug("assigning " + part + " for " + token);
        messageValue.setPart(part, value);
      }
    }
    else if (query != null) {
      log.debug("extracting " + variable + " for " + token);
      Object variableValue = variable.getValueForAssign(token);

      // prevent direct query on a message variable
      if (variableValue instanceof MessageValue)
        throw new BpelException("illegal query on message variable: " + variable.getName());

      log.debug("assigning " + query + " for " + token);
      query.getEvaluator().assign((Element) variableValue, token, value);
    }
    else {
      log.debug("assigning " + variable + " for " + token);
      variable.setValue(token, value);
    }
  }
}
