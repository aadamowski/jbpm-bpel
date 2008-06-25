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

import org.jbpm.bpel.graph.basic.Empty;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.persistence.db.AbstractDbTestCase;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.graph.def.Transition;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/10/13 02:53:26 $
 */
public class SequenceDbTest extends AbstractDbTestCase {

  private BpelProcessDefinition processDefinition;
  private Sequence sequence = new Sequence("parent");

  protected void setUp() throws Exception {
    super.setUp();
    // process, create after opening jbpm context
    processDefinition = new BpelProcessDefinition("pd", BpelConstants.NS_EXAMPLES);
    processDefinition.getGlobalScope().setActivity(sequence);
  }

  public void testActivities() {
    Activity first = new Empty("first");
    Activity second = new Empty("second");

    sequence.addNode(first);
    sequence.addNode(second);

    processDefinition = saveAndReload(processDefinition);
    sequence = (Sequence) session.load(Sequence.class, new Long(processDefinition.getGlobalScope()
        .getActivity()
        .getId()));
    List nodes = sequence.getNodes();
    first = (Activity) nodes.get(0);
    second = (Activity) nodes.get(1);

    assertEquals(2, nodes.size());
    assertEquals("first", first.getName());
    assertEquals("second", second.getName());
  }

  public void testDelimiters() {
    processDefinition = saveAndReload(processDefinition);
    sequence = (Sequence) session.load(Sequence.class, new Long(processDefinition.getGlobalScope()
        .getActivity()
        .getId()));
    Activity begin = sequence.getBegin();
    Activity end = sequence.getEnd();

    assertSame(sequence, begin.getCompositeActivity());
    assertSame(sequence, end.getCompositeActivity());
  }

  public void testConnections() {
    Activity first = new Empty("first");
    Activity last = new Empty("second");

    sequence.addNode(first);
    sequence.addNode(last);

    processDefinition = saveAndReload(processDefinition);
    sequence = (Sequence) session.load(Sequence.class, new Long(processDefinition.getGlobalScope()
        .getActivity()
        .getId()));
    first = (Activity) sequence.getNode("first");
    last = (Activity) sequence.getNode("second");

    Activity begin = sequence.getBegin();
    Activity end = sequence.getEnd();
    Transition from = (Transition) begin.getLeavingTransitions().get(0);
    Transition to = (Transition) end.getArrivingTransitions().iterator().next();

    assertEquals(from, first.getDefaultArrivingTransition());
    assertEquals(first.getDefaultLeavingTransition(), last.getDefaultArrivingTransition());
    assertEquals(to, last.getDefaultLeavingTransition());
  }
}
