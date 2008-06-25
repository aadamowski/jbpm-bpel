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

import junit.framework.TestCase;

import org.jbpm.bpel.graph.basic.Empty;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.graph.def.Transition;

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/10/13 02:53:25 $
 */
public class ActivityDefTest extends TestCase {

  LinkDefinition link;
  Activity activity;

  protected void setUp() {
    activity = new Empty();
    link = new LinkDefinition("testLink");

    BpelProcessDefinition pd = new BpelProcessDefinition("pd", BpelConstants.NS_EXAMPLES);
    pd.getGlobalScope().setActivity(activity);
  }

  public void testDefaultArrivingTransition() {
    Transition t = new Transition();
    activity.addArrivingTransition(t);
    assertEquals(t, activity.getDefaultArrivingTransition());
  }

  public void testAddSource() {
    activity.addSource(link);
    assertEquals(link, activity.getSources().iterator().next());
  }

  public void testAddTarget() {
    activity.addTarget(link);
    assertEquals(link.getTarget(), activity);
    assertTrue(activity.getTargets().contains(link));
  }

  public void testSuppressJoinFailure() {
    assertFalse(activity.suppressJoinFailure());

    activity.getCompositeActivity().setSuppressJoinFailure(Boolean.TRUE);
    assertTrue(activity.suppressJoinFailure());

    activity.setSuppressJoinFailure(Boolean.FALSE);
    assertFalse(activity.suppressJoinFailure());
  }
}
