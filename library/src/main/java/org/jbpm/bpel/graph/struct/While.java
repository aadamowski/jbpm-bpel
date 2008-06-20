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
package org.jbpm.bpel.graph.struct;

import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.BpelVisitor;
import org.jbpm.bpel.xml.util.DatatypeUtil;
import org.jbpm.graph.exe.Token;

/**
 * Defines that an activity is to be repeated as long as the specified
 * {@link #getCondition() condition} is true.
 * @author Juan Cantú
 * @version $Revision$ $Date: 2008/02/01 05:43:08 $
 */
public class While extends RepetitiveActivity {

  private static final long serialVersionUID = 1L;

  public While() {
  }

  public While(String name) {
    super(name);
  }

  protected boolean repeatExecution(Token token) {
    return DatatypeUtil.toBoolean(getCondition().getEvaluator().evaluate(token)) == true;
  }

  protected void addImplicitTransitions(Activity activity) {
    getBegin().connect(loop);
    loop.connect(activity); // transition 0 (activity)
    loop.connect(getEnd()); // transition 1 (end)
    activity.connect(loop);
  }

  protected void removeImplicitTransitions(Activity activity) {
    getBegin().disconnect(loop);
    loop.disconnect(activity);
    loop.disconnect(getEnd());
    activity.disconnect(loop);
  }

  public void accept(BpelVisitor visitor) {
    visitor.visit(this);
  }
}