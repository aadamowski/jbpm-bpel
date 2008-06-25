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

import org.jbpm.bpel.graph.basic.Empty;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.def.LinkDefinition;
import org.jbpm.bpel.persistence.db.AbstractDbTestCase;
import org.jbpm.bpel.sublang.def.Expression;
import org.jbpm.bpel.xml.BpelConstants;

/**
 * @author Juan Cantu
 * @version $Revision$ $Date: 2007/10/13 02:53:26 $
 */
public class FlowDbTest extends AbstractDbTestCase {

  private BpelProcessDefinition processDefinition;
  private Flow flow = new Flow("parent");

  protected void setUp() throws Exception {
    super.setUp();
    /*
     * the process definition accesses the jbpm configuration, so create a context before creating a
     * process definition to avoid creating another context
     */
    processDefinition = new BpelProcessDefinition("pd", BpelConstants.NS_EXAMPLES);
    processDefinition.getGlobalScope().setActivity(flow);
  }

  public void testLinks() {
    Expression transitionCondition = new Expression();
    transitionCondition.setText("$condition");

    LinkDefinition a = new LinkDefinition("a");
    a.setTransitionCondition(transitionCondition);

    LinkDefinition b = new LinkDefinition("b");

    Activity activity = new Empty("linked");
    activity.addTarget(a);
    activity.addSource(b);

    flow.addNode(activity);
    flow.addLink(b);
    flow.addLink(a);

    processDefinition = saveAndReload(processDefinition);
    flow = (Flow) session.load(Flow.class, new Long(processDefinition.getGlobalScope()
        .getActivity()
        .getId()));
    activity = (Activity) flow.getNode("linked");
    assertEquals(2, flow.getLinks().size());

    a = flow.getLink("a");
    assertEquals("$condition", a.getTransitionCondition().getText());
    assertSame(activity, a.getTarget());

    b = flow.getLink("b");
    assertNull(b.getTransitionCondition());
    assertSame(activity, b.getSource());
  }

  public void testDelimiters() {
    processDefinition = saveAndReload(processDefinition);
    flow = (Flow) session.load(Flow.class, new Long(processDefinition.getGlobalScope()
        .getActivity()
        .getId()));
    Activity begin = flow.getBegin();
    Activity end = flow.getEnd();

    assertSame(flow, begin.getCompositeActivity());
    assertSame(flow, end.getCompositeActivity());
  }

  public void testConnections() {
    Activity activity = new Empty("activity");
    flow.addNode(activity);

    processDefinition = saveAndReload(processDefinition);
    flow = (Flow) session.load(Flow.class, new Long(processDefinition.getGlobalScope()
        .getActivity()
        .getId()));
    activity = (Activity) flow.getNode("activity");
    Activity begin = flow.getBegin();
    Activity end = flow.getEnd();

    assertTrue(begin.getLeavingTransitions().contains(activity.getDefaultArrivingTransition()));
    assertTrue(end.getArrivingTransitions().contains(activity.getDefaultLeavingTransition()));
  }
}