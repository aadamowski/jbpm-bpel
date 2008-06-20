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

import org.jbpm.bpel.graph.basic.Empty;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/03/05 13:10:21 $
 */
public class JoinConditionDbTest extends SnippetDbTestCase {

  protected Snippet createSnippet(BpelProcessDefinition processDefinition) {
    JoinCondition joinCondition = new JoinCondition();

    Activity activity = new Empty();
    activity.setJoinCondition(joinCondition);

    processDefinition.getGlobalScope().setActivity(activity);

    return joinCondition;
  }

  protected Snippet getSnippet(BpelProcessDefinition processDefinition) {
    Activity activity = processDefinition.getGlobalScope().getActivity();
    return activity.getJoinCondition();
  }
}