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

import java.util.Collection;

import junit.framework.TestCase;

import org.jbpm.bpel.graph.basic.Empty;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.struct.If;
import org.jbpm.bpel.sublang.def.Expression;

/**
 * @author Juan Cantu
 * @version $Revision$ $Date: 2007/03/22 13:32:55 $
 */
public class IfDefTest extends TestCase {

  If _if = new If();
  Activity first = new Empty("first");
  Activity second = new Empty("second");

  public void setUp() {
    _if.addNode(first);
    _if.addNode(second);
  }

  public void testAddNode() {
    assertConnected(first);
    assertEquals(null, _if.getCondition(first));
    assertConnected(second);
    assertEquals(null, _if.getCondition(second));
  }

  public void testCondition() {
    Expression firstExpression = new Expression();
    Expression secondExpression = new Expression();

    _if.setCondition(first, firstExpression);
    _if.setCondition(second, secondExpression);

    assertEquals(firstExpression, _if.getCondition(first));
    assertEquals(secondExpression, _if.getCondition(second));
  }

  public void testRemoveNode() {
    // remove first, middle and last activities
    _if.setElse(second);

    _if.removeNode(first);
    assertDisconnected(first);

    _if.removeNode(second);
    assertDisconnected(second);

    assertEquals(0, _if.getBegin().getLeavingTransitions().size());
    assertEquals(0, _if.getEnd().getArrivingTransitions().size());

    assertEquals(0, _if.getNodes().size());
    assertEquals(0, _if.getConditions().size());
  }

  public void testElse() {
    _if.setElse(first);
    assertEquals(first, _if.getElse());
  }

  public void testElseOverride() {
    _if.setElse(second);
    assertEquals(1, _if.getBranches().size());

    _if.setElse(first);
    assertEquals(0, _if.getBranches().size());

    assertEquals(first, _if.getElse());
    assertEquals(_if, first.getCompositeActivity());
  }

  public void testElseDefault() {
    assertEquals(null, _if.getElse());
  }

  public void testGetBranches() {
    assertEquals(2, _if.getBranches().size());
  }

  public void testGetBranchesElse() {
    _if.setElse(second);
    assertEquals(1, _if.getBranches().size());
  }

  public void testReorderBranch() {
    Expression firstExpression = new Expression();
    Expression secondExpression = new Expression();

    _if.setCondition(first, firstExpression);
    _if.setCondition(second, secondExpression);

    _if.reorderNode(1, 0);

    assertEquals(second, _if.getNodes().get(0));
    assertEquals(firstExpression, _if.getCondition(first));
    assertEquals(secondExpression, _if.getCondition(second));
  }

  public void testReorderElse() {
    Expression firstExpression = new Expression();
    _if.setCondition(first, firstExpression);
    _if.setElse(second);

    _if.reorderNode(1, 0);

    assertEquals(firstExpression, _if.getCondition(second));
    assertEquals(first, _if.getElse());
  }

  private void assertConnected(Activity activity) {
    Collection transitions = _if.getBegin().getLeavingTransitions();
    assertTrue(transitions.contains(activity.getDefaultArrivingTransition()));

    transitions = _if.getEnd().getArrivingTransitions();
    assertTrue(transitions.contains(activity.getDefaultLeavingTransition()));
  }

  private void assertDisconnected(Activity activity) {
    // validate that removed activity doesn't have incoming / outgoing
    // transitions
    assertEquals(0, activity.getArrivingTransitions().size());
    assertEquals(0, activity.getLeavingTransitions().size());
  }

}
