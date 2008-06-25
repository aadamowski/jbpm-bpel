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
package org.jbpm.bpel.graph.def;

import org.jbpm.bpel.graph.scope.Scope;
import org.jbpm.bpel.graph.struct.Flow;
import org.jbpm.bpel.graph.struct.Sequence;
import org.jbpm.bpel.persistence.db.AbstractDbTestCase;
import org.jbpm.bpel.sublang.def.JoinCondition;
import org.jbpm.bpel.xml.BpelConstants;

/**
 * @author Juan Cantu
 * @version $Revision$ $Date: 2007/08/08 11:08:03 $
 */
public abstract class AbstractActivityDbTestCase extends AbstractDbTestCase {

  protected BpelProcessDefinition processDefinition;

  protected void setUp() throws Exception {
    super.setUp();
    processDefinition = new BpelProcessDefinition("p", BpelConstants.NS_EXAMPLES);
  }

  public void testProcessDefinition() {
    Activity activity = createActivity();
    processDefinition.getGlobalScope().setActivity(activity);

    processDefinition = saveAndReload(processDefinition);
    activity = processDefinition.getGlobalScope().getActivity();

    assertEquals(processDefinition, activity.getProcessDefinition());
  }

  public void testBpelProcessDefinition() {
    Activity activity = createActivity();
    processDefinition.getGlobalScope().setActivity(activity);

    processDefinition = saveAndReload(processDefinition);
    activity = processDefinition.getGlobalScope().getActivity();

    assertEquals(processDefinition, activity.getBpelProcessDefinition());
  }

  public void testCompositeActivity() {
    Activity activity = createActivity();

    Sequence main = new Sequence("main");
    main.addNode(activity);

    processDefinition.getGlobalScope().setActivity(main);

    processDefinition = saveAndReload(processDefinition);
    activity = (Activity) session.load(Activity.class, new Long(activity.getId()));

    assertEquals(processDefinition.getGlobalScope().getActivity(), activity.getCompositeActivity());
  }

  public void testScope() {
    Activity activity = createActivity();

    Scope local = new Scope("local");
    local.setActivity(activity);

    Sequence main = new Sequence("main");
    main.addNode(local);

    processDefinition.getGlobalScope().setActivity(main);

    processDefinition = saveAndReload(processDefinition);
    activity = (Activity) session.load(Activity.class, new Long(activity.getId()));

    CompositeActivity parent = activity.getCompositeActivity();
    assertEquals("local", parent.getName());
    assertTrue(parent.isScope());
    /*
     * when AbstractDbTestCase.newTransaction() simply ends the current transaction and begins a new
     * one, the session preserves the objects already loaded; thus, parent is still a Scope
     */
    // assertFalse(parent instanceof Scope);
    local = activity.getScope();
    assertEquals("local", local.getName());

    // verify proxy reacquisition
    parent = activity.getCompositeActivity();
    assertTrue(parent instanceof Scope);
  }

  public void testJoinCondition() {
    JoinCondition joinCondition = new JoinCondition();
    joinCondition.setText("$tm");

    Activity activity = createActivity();
    activity.setJoinCondition(joinCondition);

    processDefinition.getGlobalScope().setActivity(activity);

    processDefinition = saveAndReload(processDefinition);
    activity = processDefinition.getGlobalScope().getActivity();

    assertEquals("$tm", activity.getJoinCondition().getText());
  }

  public void testSupressJoinFailure() {
    Activity activity = createActivity();
    activity.setSuppressJoinFailure(Boolean.TRUE);

    processDefinition.getGlobalScope().setActivity(activity);

    processDefinition = saveAndReload(processDefinition);
    activity = processDefinition.getGlobalScope().getActivity();

    assertEquals(Boolean.TRUE, activity.getSuppressJoinFailure());
  }

  public void testSourcesAndTargets() {
    LinkDefinition one = new LinkDefinition("one");
    LinkDefinition two = new LinkDefinition("two");
    LinkDefinition three = new LinkDefinition("three");

    Activity a = createActivity();
    a.setName("a");
    a.addSource(one);
    a.addSource(two);

    Activity b = createActivity();
    b.setName("b");
    b.addTarget(one);
    b.addSource(three);

    Activity c = createActivity();
    c.setName("c");
    c.addTarget(two);
    c.addTarget(three);

    Flow main = new Flow("main");
    main.addNode(a);
    main.addNode(b);
    main.addNode(c);

    processDefinition.getGlobalScope().setActivity(main);

    processDefinition = saveAndReload(processDefinition);
    main = (Flow) session.load(Flow.class, new Long(processDefinition.getGlobalScope()
        .getActivity()
        .getId()));

    a = (Activity) main.getNode("a");
    b = (Activity) main.getNode("b");
    c = (Activity) main.getNode("c");

    assertEquals(2, a.getSources().size());
    assertEquals(0, a.getTargets().size());

    assertEquals(1, b.getSources().size());
    assertEquals(1, b.getTargets().size());

    assertEquals(0, c.getSources().size());
    assertEquals(2, c.getTargets().size());
  }

  protected abstract Activity createActivity();
}
