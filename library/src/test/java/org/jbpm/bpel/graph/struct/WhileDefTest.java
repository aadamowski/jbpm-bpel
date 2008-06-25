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

import java.util.List;

import junit.framework.TestCase;

import org.jbpm.bpel.graph.basic.Empty;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.struct.RepetitiveActivity.Loop;
import org.jbpm.bpel.sublang.def.Expression;

/**
 * @author Juan Cantu
 * @version $Revision$ $Date: 2008/01/30 08:15:34 $
 */
public class WhileDefTest extends TestCase {

  private While _while;
  private Activity child;
  private Activity otherChild;

  protected void setUp() {
    _while = new While();
    child = new Empty("first");
    otherChild = new Empty("second");
  }

  public void testAddNode() {
    _while.addNode(child);
    assertConnected(child);
  }

  public void testAddNodeOverride() {
    _while.addNode(child);
    _while.addNode(otherChild);
    assertConnected(otherChild);
    assertDisconnected(child);
  }

  public void testRemoveNode() {
    _while.addNode(child);
    _while.removeNode(child);
    assertDisconnected(child);

    assertEquals(0, _while.getNodes().size());
  }

  public void testCondition() {
    Expression expression = new Expression();
    _while.setCondition(expression);
    assertEquals(expression, _while.getCondition());
  }

  private void assertConnected(Activity activity) {
    Loop loop = _while.getLoop();
    assertTrue(_while.getBegin().isConnected(loop));
    assertTrue(loop.isConnected(activity));
    assertTrue(activity.isConnected(loop));
    assertTrue(loop.isConnected(_while.getEnd()));

    assertSame(_while, activity.getCompositeActivity());

    List nodes = _while.getNodes();
    assertEquals(1, nodes.size());
    assertSame(activity, nodes.get(0));
  }

  private void assertDisconnected(Activity activity) {
    // verify activity has no incoming / outgoing transitions
    assertEquals(0, activity.getArrivingTransitions().size());
    assertEquals(0, activity.getLeavingTransitions().size());
    assertEquals(null, activity.getCompositeActivity());
  }
}
