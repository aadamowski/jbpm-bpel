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
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2008/01/30 08:15:34 $
 */
public class RepeatUntilDbTest extends AbstractDbTestCase {

  private BpelProcessDefinition processDefinition;
  private RepeatUntil repeatUntil = new RepeatUntil();

  protected void setUp() throws Exception {
    super.setUp();
    /*
     * the process definition accesses the jbpm configuration, so create a context before creating a
     * process definition to avoid creating another context
     */
    processDefinition = new BpelProcessDefinition("pd", BpelConstants.NS_EXAMPLES);
    processDefinition.getGlobalScope().setActivity(repeatUntil);
  }

  public void testCondition() {
    Expression condition = new Expression();
    condition.setText("false()");

    repeatUntil.setCondition(condition);

    processDefinition = saveAndReload(processDefinition);
    repeatUntil = (RepeatUntil) session.load(RepeatUntil.class, new Long(
        processDefinition.getGlobalScope().getActivity().getId()));

    assertEquals("false()", repeatUntil.getCondition().getText());
  }

  public void testDelimiters() {
    processDefinition = saveAndReload(processDefinition);
    repeatUntil = (RepeatUntil) session.load(RepeatUntil.class, new Long(
        processDefinition.getGlobalScope().getActivity().getId()));
    Activity begin = repeatUntil.getBegin();
    Activity end = repeatUntil.getEnd();

    assertSame(repeatUntil, begin.getCompositeActivity());
    assertSame(repeatUntil, end.getCompositeActivity());
  }

  public void testConnections() {
    Activity activity = new Empty("activity");
    repeatUntil.addNode(activity);

    processDefinition = saveAndReload(processDefinition);
    repeatUntil = (RepeatUntil) session.load(RepeatUntil.class, new Long(
        processDefinition.getGlobalScope().getActivity().getId()));
    activity = (Activity) repeatUntil.getNode("activity");

    Activity begin = repeatUntil.getBegin();
    Activity end = repeatUntil.getEnd();
    Activity loop = repeatUntil.getLoop();

    assertSame(repeatUntil, loop.getCompositeActivity());

    assertTrue(begin.isConnected(activity));
    assertTrue(activity.isConnected(loop));
    assertTrue(loop.isConnected(activity));
    assertTrue(loop.isConnected(end));
  }
}
