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
package org.jbpm.bpel.graph.def;

import java.io.Serializable;

import org.apache.commons.lang.builder.ToStringBuilder;

import org.jbpm.bpel.graph.exe.LinkInstance;
import org.jbpm.bpel.graph.struct.Flow;
import org.jbpm.bpel.sublang.def.Expression;
import org.jbpm.bpel.xml.util.DatatypeUtil;
import org.jbpm.graph.exe.Token;

/**
 * Establishes explicit control dependencies between nested child activities within a
 * {@link Flow flow}.
 * @author Juan Cantú
 * @version $Revision$ $Date: 2008/02/01 05:43:08 $
 */
public class LinkDefinition implements Serializable {

  long id;
  private String name;
  private Activity target;
  private Activity source;
  private Expression transitionCondition;

  private static final long serialVersionUID = 1L;

  public LinkDefinition() {
  }

  public LinkDefinition(String name) {
    setName(name);
  }

  public void determineStatus(Token token) {
    boolean status;

    if (transitionCondition != null) {
      Object conditionValue = transitionCondition.getEvaluator().evaluate(token);
      status = DatatypeUtil.toBoolean(conditionValue);
    }
    else
      status = true;

    getInstance(token).statusDetermined(status);
  }

  public Expression getTransitionCondition() {
    return transitionCondition;
  }

  public void setTransitionCondition(Expression transitionCondition) {
    this.transitionCondition = transitionCondition;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Activity getTarget() {
    return target;
  }

  void setTarget(Activity target) {
    this.target = target;
  }

  public Activity getSource() {
    return source;
  }

  void setSource(Activity source) {
    this.source = source;
  }

  public String toString() {
    return new ToStringBuilder(this).append("name", name).append("source", source).append("target",
        target).toString();
  }

  public LinkInstance getInstance(Token token) {
    return (LinkInstance) token.getProcessInstance().getContextInstance().getVariable(name, token);
  }

  public LinkInstance createInstance(Token token) {
    LinkInstance linkInstance = new LinkInstance(this);
    token.getProcessInstance().getContextInstance().createVariable(name, linkInstance, token);
    return linkInstance;
  }
}