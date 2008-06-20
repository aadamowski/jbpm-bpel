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
package org.jbpm.bpel.xml;

import junit.framework.Assert;

import org.w3c.dom.Element;

import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.LinkDefinition;
import org.jbpm.bpel.graph.struct.Flow;

/**
 * Tests the parsing of standard attributes and elements into a bpel activity using <empty>
 * elements.
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/10/13 02:53:24 $
 */
public class ActivityReaderTest extends AbstractReaderTestCase {

  public void testProcessDefinition() throws Exception {
    Activity activity = readActivity("<empty/>");
    assertEquals(processDefinition, activity.getProcessDefinition());
  }

  public void testSuperState() throws Exception {
    Activity activity = readActivity("<empty/>");
    assertEquals(scope, activity.getParent());
  }

  public void testName() throws Exception {
    Activity activity = readActivity("<empty name='n'/>");
    assertEquals("n", activity.getName());
  }

  public void testNameDefault() throws Exception {
    Activity activity = readActivity("<empty/>");
    assertEquals("Empty#1", activity.getName());
  }

  public void testSuppressJoinFailureYes() throws Exception {
    Activity activity = readActivity("<empty suppressJoinFailure='yes'/>");
    assertEquals(Boolean.TRUE, activity.getSuppressJoinFailure());
  }

  public void testSuppressJoinFailureNo() throws Exception {
    Activity activity = readActivity("<empty suppressJoinFailure='no'/>");
    assertEquals(Boolean.FALSE, activity.getSuppressJoinFailure());
  }

  public void testSuppressJoinFailureDefault() throws Exception {
    Activity activity = readActivity("<empty/>");
    assertNull(activity.getSuppressJoinFailure());
  }

  public void testSources() throws Exception {
    String xml = "<empty>"
        + "<sources>"
        + " <source linkName='l1'>"
        + "   <transitionCondition>$tc</transitionCondition>"
        + " </source>"
        + " <source linkName='l2'/>"
        + "</sources>"
        + "</empty>";
    Flow flow = initFlow();

    Element element = parseAsBpelElement(xml);
    Activity activity = readActivity(element, flow);

    // test link resolution
    LinkDefinition conditionedLink = activity.getSource("l1");
    Assert.assertNotNull(conditionedLink);
    Assert.assertNotNull(activity.getSource("l2"));

    // test transition condition
    conditionedLink = flow.getLink(conditionedLink.getName());
    assertEquals("$tc", conditionedLink.getTransitionCondition().getText());
  }

  public void testSourcesDefault() throws Exception {
    Activity activity = readActivity("<empty/>");
    assertTrue(activity.getSources().isEmpty());
  }

  public void testTargets() throws Exception {
    String xml = "<empty>"
        + "<targets>  "
        + " <joinCondition>'l1'</joinCondition>"
        + " <target linkName='l1'/>"
        + " <target linkName='l2'/>"
        + "</targets>"
        + "</empty>";
    Flow flow = initFlow();
    Element element = parseAsBpelElement(xml);
    Activity activity = readActivity(element, flow);

    // test link resolution
    Assert.assertNotNull(activity.getTarget("l1"));
    Assert.assertNotNull(activity.getTarget("l2"));

    // test join condition
    Assert.assertEquals("'l1'", activity.getJoinCondition().getText());
  }

  public void testTargetsDefault() throws Exception {
    Activity activity = readActivity("<empty/>");
    assertTrue(activity.getTargets().isEmpty());
  }

  public void testJoinConditionWithoutTargets() throws Exception {
    Activity activity = readActivity("<empty><joinCondition>'jc'</joinCondition></empty>");
    assertNull(activity.getJoinCondition());
  }

  public void testJoinConditionDefault() throws Exception {
    Activity activity = readActivity("<empty/>");
    assertNull(activity.getJoinCondition());
  }

  private Flow initFlow() {
    Flow flow = new Flow();
    flow.setName("f1");
    LinkDefinition link1 = new LinkDefinition("l1");
    LinkDefinition link2 = new LinkDefinition("l2");
    flow.addLink(link1);
    flow.addLink(link2);
    scope.addNode(flow);
    return flow;
  }
}