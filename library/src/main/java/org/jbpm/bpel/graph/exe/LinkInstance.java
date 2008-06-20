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
package org.jbpm.bpel.graph.exe;

import java.io.Serializable;

import org.apache.commons.lang.builder.ToStringBuilder;

import org.jbpm.bpel.graph.def.LinkDefinition;
import org.jbpm.graph.exe.Token;

public class LinkInstance implements Serializable {

  long id;
  private LinkDefinition definition = null;
  private Boolean status = null;
  private Token targetToken = null;

  private static final long serialVersionUID = 1L;

  LinkInstance() {
  }

  public LinkInstance(LinkDefinition definition) {
    this.definition = definition;
  }

  public LinkDefinition getDefinition() {
    return definition;
  }

  public Boolean getStatus() {
    return status;
  }

  public void setStatus(Boolean status) {
    this.status = status;
  }

  public Token getTargetToken() {
    return targetToken;
  }

  public void setTargetToken(Token token) {
    this.targetToken = token;
  }

  public void statusDetermined(boolean status) {
    this.status = Boolean.valueOf(status);
    if (targetToken != null)
      definition.getTarget().targetDetermined(targetToken); // notify target activity
  }

  public String toString() {
    return new ToStringBuilder(this).append("name", definition.getName())
        .append("status", status)
        .toString();
  }
}