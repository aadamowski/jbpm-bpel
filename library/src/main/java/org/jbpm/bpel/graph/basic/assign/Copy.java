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

import org.jbpm.bpel.graph.basic.AssignOperation;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;

public class Copy extends AssignOperation {

  long id;
  private From from;
  private To to;

  private static final long serialVersionUID = 1L;

  private static final Log log = LogFactory.getLog(Copy.class);

  public void execute(ExecutionContext exeContext) {
    Token token = exeContext.getToken();
    Object value = from.extract(token);
    log.debug("copying value '" + value + "' for " + token);
    to.assign(token, value);
  }

  public From getFrom() {
    return from;
  }

  public void setFrom(From from) {
    this.from = from;
  }

  public To getTo() {
    return to;
  }

  public void setTo(To to) {
    this.to = to;
  }
}