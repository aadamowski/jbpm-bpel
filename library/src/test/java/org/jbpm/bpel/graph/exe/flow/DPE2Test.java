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
package org.jbpm.bpel.graph.exe.flow;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.w3c.dom.Element;

import org.jbpm.bpel.graph.basic.Receive;
import org.jbpm.bpel.graph.exe.AbstractExeTestCase;
import org.jbpm.bpel.graph.struct.Flow;
import org.jbpm.bpel.xml.util.XmlUtil;
import org.jbpm.graph.exe.Token;

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/11/25 13:03:14 $
 */
public class DPE2Test extends AbstractExeTestCase {

  private Flow flow;
  private Receive a;
  private Receive b;
  private Receive c;
  private Receive x;
  private Receive y;

  private static Element flowElem;

  /*
   * In the flow, five receive activities A, B, C, X and Y are all concurrently enabled to start
   * when the flow starts. A, B, and C are connected through links (with transition conditions with
   * constant truth-value of "true") instead of putting them into a sequence. B cannot start until
   * the status of its incoming links from X and Y is determined and the implicit join condition is
   * evaluated. B and thus C will always be performed. Because the join condition is a disjunction
   * and the transition condition of link AtoB is the constant "true", the join condition will
   * always evaluate "true", independent from the values of X and Y targets
   */

  private void initActivities() {
    a = (Receive) flow.getNode("A");
    b = (Receive) flow.getNode("B");
    c = (Receive) flow.getNode("C");
    x = (Receive) flow.getNode("X");
    y = (Receive) flow.getNode("Y");
  }

  public void testFail() throws Exception {
    flow = (Flow) readActivity(flowElem, false);
    flow.setSuppressJoinFailure(Boolean.FALSE);
    plugInner(flow);
    assertExecution();
  }

  public void testSuppress() throws Exception {
    flow = (Flow) readActivity(flowElem, false);
    flow.setSuppressJoinFailure(Boolean.TRUE);
    plugInner(flow);
    assertExecution();
  }

  public void testInitialFail() throws Exception {
    flow = (Flow) readActivity(flowElem, true);
    flow.setSuppressJoinFailure(Boolean.FALSE);
    plugInitial(flow);
    assertInitialExecution();
  }

  public void testInitialSuppress() throws Exception {
    flow = (Flow) readActivity(flowElem, true);
    flow.setSuppressJoinFailure(Boolean.FALSE);
    plugInitial(flow);
    assertInitialExecution();
  }

  private void assertExecution() throws Exception {
    initActivities();
    Token startToken = executeInner();

    // tokens for activities a, b, c, x, y were created upon activation
    // they must be waiting in their respective receive nodes
    Token tokenA = findToken(startToken, a);
    Token tokenB = findToken(startToken, b);
    Token tokenC = findToken(startToken, c);
    Token tokenX = findToken(startToken, x);
    Token tokenY = findToken(startToken, y);

    // activity a message is received, advances to b.
    assertReceiveAndAdvance(tokenA, a, flow.getEnd());
    // activity b is not executed due to its incoming sources
    assertReceiveDisabled(tokenB, b);

    // activity x receives message and advances to the end of the flow
    assertReceiveAndAdvance(tokenX, x, flow.getEnd());
    // activity y receives message and advances to the end of the flow
    // all links of b are determined
    assertReceiveAndAdvance(tokenY, y, flow.getEnd());
    // b is ready to advance.
    assertReceiveAndAdvance(tokenB, b, flow.getEnd());

    // complete the flow execution
    assertReceiveAndComplete(tokenC, c);
  }

  private void assertInitialExecution() throws Exception {
    initActivities();
    a.setCreateInstance(true);
    b.setCreateInstance(true);
    c.setCreateInstance(true);
    x.setCreateInstance(true);
    y.setCreateInstance(true);

    // execute first receive
    Token startToken = executeInitial(a.getReceiveAction());

    // tokens for activities b, c, x, y where created upon activation
    Token tokenB = findToken(startToken, b);
    // activity b is not executed due to its unresolved targets
    assertReceiveDisabled(tokenB, b);

    Token tokenC = findToken(startToken, c);
    Token tokenX = findToken(startToken, x);
    Token tokenY = findToken(startToken, y);

    // activity x receives message and advances to the end of the flow
    assertReceiveAndAdvance(tokenX, x, flow.getEnd());
    // activity y receives message and advances to the end of the flow
    // all links of b are determined
    assertReceiveAndAdvance(tokenY, y, flow.getEnd());
    // b is ready to advance
    assertReceiveAndAdvance(tokenB, b, flow.getEnd());

    // complete the flow execution
    assertReceiveAndComplete(tokenC, c);
  }

  public static Test suite() {
    return new Setup();
  }

  private static class Setup extends TestSetup {

    private Setup() {
      super(new TestSuite(DPE2Test.class));
    }

    protected void setUp() throws Exception {
      flowElem = (Element) XmlUtil.parseResource("dpe2.xml", DPE2Test.class).getNode();
    }

    protected void tearDown() throws Exception {
      flowElem = null;
    }
  }
}
