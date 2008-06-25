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
import org.jbpm.bpel.persistence.db.AbstractDbTestCase;
import org.jbpm.bpel.sublang.def.Expression;
import org.jbpm.bpel.xml.BpelConstants;

/**
 * @author Juan Cantu
 * @version $Revision$ $Date: 2007/10/13 02:53:26 $
 */
public class IfDbTest extends AbstractDbTestCase {

  private BpelProcessDefinition processDefinition;
  private If _if = new If("parent");

  protected void setUp() throws Exception {
    super.setUp();
    /*
     * the process definition accesses the jbpm configuration, so create a context before creating a
     * process definition to avoid creating another context
     */
    processDefinition = new BpelProcessDefinition("pd", BpelConstants.NS_EXAMPLES);
    processDefinition.getGlobalScope().setActivity(_if);
  }

  public void testBranch() {
    Activity activity = new Empty("activity");

    Expression condition = new Expression();
    condition.setText("$condition");

    _if.addNode(activity);
    _if.setCondition(activity, condition);

    processDefinition = saveAndReload(processDefinition);
    _if = (If) session.load(If.class, new Long(processDefinition.getGlobalScope()
        .getActivity()
        .getId()));
    activity = (Activity) _if.getNode("activity");

    assertEquals("$condition", _if.getCondition(activity).getText());
    assertNull(_if.getElse());
  }

  public void testElse() {
    Activity _else = new Empty("else");

    _if.addNode(_else);
    _if.setElse(_else);

    processDefinition = saveAndReload(processDefinition);
    _if = (If) session.load(If.class, new Long(processDefinition.getGlobalScope()
        .getActivity()
        .getId()));
    _else = _if.getElse();

    assertEquals("else", _else.getName());
    assertNull(_if.getCondition(_else));
  }

  public void testDelimiters() {
    processDefinition = saveAndReload(processDefinition);
    _if = (If) session.load(If.class, new Long(processDefinition.getGlobalScope()
        .getActivity()
        .getId()));
    Activity begin = _if.getBegin();
    Activity end = _if.getEnd();

    assertSame(_if, begin.getCompositeActivity());
    assertSame(_if, end.getCompositeActivity());
  }

  public void testConnections() {
    Activity activity = new Empty("activity");
    Expression condition = new Expression();

    _if.addNode(activity);
    _if.setCondition(activity, condition);

    processDefinition = saveAndReload(processDefinition);
    _if = (If) session.load(If.class, new Long(processDefinition.getGlobalScope()
        .getActivity()
        .getId()));
    activity = (Activity) _if.getNode("activity");
    Activity begin = _if.getBegin();
    Activity end = _if.getEnd();

    assertTrue(begin.getLeavingTransitions().contains(activity.getDefaultArrivingTransition()));
    assertTrue(end.getArrivingTransitions().contains(activity.getDefaultLeavingTransition()));
  }
}