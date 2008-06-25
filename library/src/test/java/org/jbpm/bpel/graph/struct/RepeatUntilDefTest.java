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
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2008/01/30 08:15:34 $
 */
public class RepeatUntilDefTest extends TestCase {

  private RepeatUntil repeatUntil;
  private Activity child;
  private Activity otherChild;

  protected void setUp() {
    repeatUntil = new RepeatUntil();
    child = new Empty("first");
    otherChild = new Empty("second");
  }

  public void testAddNode() {
    repeatUntil.addNode(child);
    assertConnected(child);
  }

  public void testAddNodeOverride() {
    repeatUntil.addNode(child);
    repeatUntil.addNode(otherChild);
    assertConnected(otherChild);
    assertDisconnected(child);
  }

  public void testRemoveNode() {
    repeatUntil.addNode(child);
    repeatUntil.removeNode(child);
    assertDisconnected(child);

    assertEquals(0, repeatUntil.getNodes().size());
  }

  public void testCondition() {
    Expression expression = new Expression();
    repeatUntil.setCondition(expression);
    assertEquals(expression, repeatUntil.getCondition());
  }

  private void assertConnected(Activity activity) {
    Loop loop = repeatUntil.getLoop();
    assertTrue(repeatUntil.getBegin().isConnected(activity));
    assertTrue(activity.isConnected(loop));
    assertTrue(loop.isConnected(activity));
    assertTrue(loop.isConnected(repeatUntil.getEnd()));

    assertSame(repeatUntil, activity.getCompositeActivity());

    List nodes = repeatUntil.getNodes();
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
