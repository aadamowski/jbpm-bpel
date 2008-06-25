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
package org.jbpm.bpel.graph.exe;

import org.jbpm.graph.exe.Token;

import org.jbpm.bpel.graph.basic.Receive;
import org.jbpm.bpel.graph.struct.Flow;

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/07/26 00:39:11 $
 */
public class FlowExeTest extends AbstractExeTestCase {

  Flow flow;
  Receive receiveA;
  Receive receiveB;
  Receive receiveC;

  String xml = "<flow>"
      + "	<receive name='a' partnerLink='aPartner' operation='o'/>"
      + "	<receive name='b' partnerLink='aPartner' operation='o'/>"
      + "	<receive name='c' partnerLink='aPartner' operation='o'/>"
      + "</flow>";

  public void testInitialFirstScenario() throws Exception {
    flow = (Flow) readActivity(xml, true);
    plugInitial(flow);
    validateInitial("a", "b", "c");
  }

  public void testInitialSecondScenario() throws Exception {
    flow = (Flow) readActivity(xml, true);
    plugInitial(flow);
    validateInitial("b", "a", "c");
  }

  public void testInitialThirdScenario() throws Exception {
    flow = (Flow) readActivity(xml, true);
    plugInitial(flow);
    validateInitial("c", "a", "b");
  }

  public void testInnerFirstScenario() throws Exception {
    flow = (Flow) readActivity(xml, false);
    plugInner(flow);
    validateInner("a", "b", "c");
  }

  public void testInnerSecondScenario() throws Exception {
    flow = (Flow) readActivity(xml, false);
    plugInner(flow);
    validateInner("b", "a", "c");
  }

  public void testInnerThirdScenario() throws Exception {
    flow = (Flow) readActivity(xml, false);
    plugInner(flow);
    validateInner("c", "a", "b");
  }

  private void initActivities(String a, String b, String c, boolean initial) {
    receiveA = (Receive) flow.getNode(a);
    receiveB = (Receive) flow.getNode(b);
    receiveC = (Receive) flow.getNode(c);

    if (initial) {
      receiveA.setCreateInstance(true);
      receiveB.setCreateInstance(true);
      receiveC.setCreateInstance(true);
    }
  }

  private void validateInitial(String a, String b, String c) {
    initActivities(a, b, c, true);
    Token parent = executeInitial(receiveA.getReceiveAction());

    Token tokenA = parent.getChild(receiveA.getName());
    // tokenA must be waiting at the flow end for tokenB and tokenC
    assertSame(flow.getEnd(), tokenA.getNode());
    /*
     * tokenB and tokenC where created upon activation; they must be waiting in
     * their respective receive nodes
     */
    Token tokenB = parent.getChild(receiveB.getName());
    Token tokenC = parent.getChild(receiveC.getName());

    // tokenB is triggered; it must move to the end of the flow
    assertReceiveAndAdvance(tokenB, receiveB, flow.getEnd());

    // tokenC is triggered; the process instance is complete
    assertReceiveAndComplete(tokenC, receiveC);
  }

  private void validateInner(String a, String b, String c) {
    initActivities(a, b, c, false);
    Token startToken = executeInner();
    /*
     * tokenA, tokenB and tokenC where created upon activation; they must be
     * waiting in their respective receive nodes
     */
    Token tokenA = startToken.getChild(a);
    Token tokenB = startToken.getChild(b);
    Token tokenC = startToken.getChild(c);

    // tokenA is triggered; it must move to the end of the flow
    assertReceiveAndAdvance(tokenA, receiveA, flow.getEnd());

    // tokenB is triggered; it must move to the end of the flow
    assertReceiveAndAdvance(tokenB, receiveB, flow.getEnd());

    // tokenC is triggered; the process instance is completed
    assertReceiveAndComplete(tokenC, receiveC);
  }
}
