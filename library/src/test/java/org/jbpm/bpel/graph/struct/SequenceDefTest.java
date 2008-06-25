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

import java.util.Iterator;

import org.jbpm.bpel.graph.basic.Empty;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.struct.Sequence;
import org.jbpm.graph.def.Transition;

import junit.framework.TestCase;

/**
 * @author Juan Cantu
 * @version $Revision$ $Date: 2006/09/27 03:53:01 $
 */
public class SequenceDefTest extends TestCase {

  Sequence sequence;
  Activity activity;

  public void setUp() {
    sequence = new Sequence();
    activity = new Empty("5");
    sequence.addNode(activity);
    activity = new Empty("6");
    sequence.addNode(activity);
    activity = new Empty("7");
    sequence.addNode(activity);
    activity = new Empty("8's");
    sequence.addNode(activity);
  }

  public void testAddNode() {
    // validate that nodes are connected in sequential order
    assertEquals(sequence.getBegin().getDefaultLeavingTransition().getTo(),
        sequence.getNode("5"));
    assertEquals(sequence.getNode("5").getDefaultLeavingTransition().getTo(),
        sequence.getNode("6"));
    assertEquals(sequence.getNode("6").getDefaultLeavingTransition().getTo(),
        sequence.getNode("7"));
    assertEquals(sequence.getNode("7").getDefaultLeavingTransition().getTo(),
        sequence.getNode("8's"));
    assertEquals(sequence.getNode("8's").getDefaultLeavingTransition().getTo(),
        sequence.getEnd());
  }

  public void testRemoveNode() {
    // remove first, middle and last activities
    Activity first = (Activity) sequence.getNode("5");
    Activity middle = (Activity) sequence.getNode("7");
    Activity last = (Activity) sequence.getNode("8's");

    sequence.removeNode(first);
    sequence.removeNode(middle);
    sequence.removeNode(last);

    // validate that remaining nodes are connected in sequential order
    assertEquals(sequence.getBegin().getDefaultLeavingTransition().getTo(),
        sequence.getNode("6"));
    assertEquals(sequence.getNode("6").getDefaultLeavingTransition().getTo(),
        sequence.getEnd());

    // validate that removed nodes don't have incoming / outgoing transitions
    assertEquals(0, first.getArrivingTransitions().size());
    assertEquals(0, first.getLeavingTransitions().size());

    assertEquals(0, middle.getArrivingTransitions().size());
    assertEquals(0, middle.getLeavingTransitions().size());

    assertEquals(0, last.getArrivingTransitions().size());
    assertEquals(0, last.getLeavingTransitions().size());
  }

  public void testReorder() {
    // this sequence of reorder invocation inverts list order
    sequence.reorderNode(1, 2);
    sequence.reorderNode(0, 3);
    sequence.reorderNode(2, 0);

    // validate that the nodes are connected in inverse sequential order
    Iterator it = sequence.getNodes().iterator();

    assertEquals(sequence.getBegin().getDefaultLeavingTransition().getTo(),
        it.next());
    assertEquals(sequence.getNode("8's").getDefaultLeavingTransition().getTo(),
        it.next());
    assertEquals(sequence.getNode("7").getDefaultLeavingTransition().getTo(),
        it.next());
    assertEquals(sequence.getNode("6").getDefaultLeavingTransition().getTo(),
        it.next());
    assertEquals(sequence.getNode("5").getDefaultLeavingTransition().getTo(),
        sequence.getEnd());
  }

  public void testMixedConnectionScenario() {
    sequence.removeNode(sequence.getNode("7"));
    sequence.removeNode(sequence.getNode("6"));
    sequence.reorderNode(0, 1);

    Activity start = sequence.getBegin();
    Activity end = sequence.getEnd();
    Activity first = (Activity) sequence.getNode("8's");
    Activity last = (Activity) sequence.getNode("5");

    Transition from = (Transition) start.getLeavingTransitions().get(0);
    Transition to = (Transition) end.getArrivingTransitions().iterator().next();

    assertEquals(from, first.getDefaultArrivingTransition());
    assertEquals(first.getDefaultLeavingTransition(),
        last.getDefaultArrivingTransition());
    assertEquals(to, last.getDefaultLeavingTransition());
  }
}