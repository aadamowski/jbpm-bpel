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
import org.jbpm.bpel.graph.exe.ScopeInstance;
import org.jbpm.bpel.graph.exe.state.EndState;
import org.jbpm.bpel.graph.scope.Scope;
import org.jbpm.bpel.graph.struct.Flow;
import org.jbpm.bpel.graph.struct.Sequence;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.bpel.xml.util.XmlUtil;
import org.jbpm.graph.exe.Token;

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2008/02/01 05:43:09 $
 */
public class DPE1Test extends AbstractExeTestCase {

  private Flow flow;
  private Sequence s;
  private Receive a;
  private Receive b;
  private Receive c;
  private Receive x;
  private Receive y;

  private static Element flowElem;

  /*
   * In the flow, the sequence S and the two receive activities X and Y are all concurrently enabled
   * to start when the flow starts. Within S, after activity A is completed, B cannot start until
   * the status of its incoming links from X and Y is determined and the implicit join condition is
   * evaluated. When activities X and Y complete, the join condition for B is evaluated. Suppose
   * that the expression P(X,B) OR P(Y,B) evaluates to false.
   */

  private void initActivities() {
    s = (Sequence) flow.getNode("S");
    a = (Receive) s.getNode("A");
    b = (Receive) s.getNode("B");
    c = (Receive) s.getNode("C");
    x = (Receive) flow.getNode("X");
    y = (Receive) flow.getNode("Y");
  }

  /*
   * The standard fault bpws:joinFailure will be thrown, because the environmental attribute
   * suppressJoinFailure is set to "no". Thus the behavior of the flow is interrupted and neither B
   * nor C will be performed.
   */
  public void testFail() throws Exception {
    flow = (Flow) readActivity(flowElem, false);
    flow.setSuppressJoinFailure(Boolean.FALSE);
    plugInner(flow);
    initActivities();

    Token startToken = executeInner();

    // tokens for activities a, x, y where created upon activation. They must be
    // waiting in their respective receive nodes
    Token tokenS = findToken(startToken, a);
    Token tokenX = findToken(startToken, x);
    Token tokenY = findToken(startToken, y);

    // activity a message is received, advances to b.
    assertReceiveAndAdvance(tokenS, a, b);
    // activity b is not executed due to its incoming sources
    assertReceiveDisabled(tokenS, b);

    // activity x receives message and advances to the flow's end.
    assertReceiveAndAdvance(tokenX, x, flow.getEnd());

    // activity y receives message. Join condition at b fails
    y.messageReceived(y.getReceiveAction(), tokenY);

    // validate that the receive has been disabled and that the whole
    // scope instance failed due the suppress failure.
    assertReceiveDisabled(tokenS, b);
    assertTrue(startToken.hasEnded());
    ScopeInstance scope = Scope.getInstance(startToken);
    assertEquals(BpelConstants.FAULT_JOIN_FAILURE, scope.getFaultInstance().getName());
    assertEquals(EndState.FAULTED, scope.getState());
  }

  public void testInitialFail() throws Exception {
    flow = (Flow) readActivity(flowElem, true);
    flow.setSuppressJoinFailure(Boolean.FALSE);
    plugInitial(flow);

    initActivities();
    a.setCreateInstance(true);
    x.setCreateInstance(true);
    y.setCreateInstance(true);

    Token startToken = executeInitial(a.getReceiveAction());

    // first receive was started, it must be at b.
    Token tokenS = findToken(startToken, b);
    // activity b is not executed due to its unresolved targets
    assertReceiveDisabled(tokenS, b);

    // tokens for activities x, y were created upon activation. They must be
    // waiting in their respective receive nodes
    Token tokenX = findToken(startToken, x);
    Token tokenY = findToken(startToken, y);

    // activity x receives message and advances to the flow's end.
    assertReceiveAndAdvance(tokenX, x, flow.getEnd());

    // activity y receives message. Join condition at b fails
    y.messageReceived(y.getReceiveAction(), tokenY);

    // validate that the receive has been disabled and that the whole
    // scope instance failed due the suppress failure.
    assertReceiveDisabled(tokenS, b);
    assertTrue(startToken.hasEnded());
    ScopeInstance scope = Scope.getInstance(startToken);
    assertEquals(BpelConstants.FAULT_JOIN_FAILURE, scope.getFaultInstance().getName());
    assertEquals(EndState.FAULTED, scope.getState());
  }

  /*
   * The environmental attribute suppressJoinFailure is set to "yes". B will be skipped but C will
   * be performed because the bpws:joinFailure will be suppressed by the implicit scope associated
   * with B.
   */

  public void testSuppress() throws Exception {
    flow = (Flow) readActivity(flowElem, false);
    flow.setSuppressJoinFailure(Boolean.TRUE);
    plugInner(flow);
    initActivities();

    Token startToken = executeInner();

    // tokens for activities a, x, y where created upon activation. They must be
    // waiting in their respective receive nodes
    Token tokenS = findToken(startToken, a);
    Token tokenX = findToken(startToken, x);
    Token tokenY = findToken(startToken, y);

    // activity a message is received, advances to b.
    assertReceiveAndAdvance(tokenS, a, b);
    // activity b is not executed due to its incoming sources
    assertReceiveDisabled(tokenS, b);

    // activity x receives message and advances to the flow's end.
    assertReceiveAndAdvance(tokenX, x, flow.getEnd());
    // activity y receives message and advances to the flow's end. b join
    // failure suppressed.
    assertReceiveAndAdvance(tokenY, y, flow.getEnd());

    // b was skipped and token must be at c.
    assertReceiveDisabled(tokenS, b);
    // complete the flow execution
    assertReceiveAndComplete(tokenS, c);
  }

  public void testInitialSuppress() throws Exception {
    flow = (Flow) readActivity(flowElem, true);
    flow.setSuppressJoinFailure(Boolean.TRUE);
    plugInitial(flow);

    initActivities();
    a.setCreateInstance(true);
    x.setCreateInstance(true);
    y.setCreateInstance(true);

    Token startToken = executeInitial(a.getReceiveAction());
    // first receive was started, it must be at b.
    Token tokenS = findToken(startToken, b);
    assertSame(b, tokenS.getNode());
    // activity b is not executed due to its unresolved targets
    assertReceiveDisabled(tokenS, b);

    // tokens for activities x, y where created upon activation. They must be
    // waiting in their respective receive nodes
    Token tokenX = findToken(startToken, x);
    Token tokenY = findToken(startToken, y);

    // activity x receives message and advances to the flow's end.
    assertReceiveAndAdvance(tokenX, x, flow.getEnd());
    // activity y receives message and advances to the flow's end. b join
    // failure suppressed.
    assertReceiveAndAdvance(tokenY, y, flow.getEnd());

    // b was skipped and token must be at c.
    assertReceiveDisabled(tokenS, b);
    // complete the flow execution
    assertReceiveAndComplete(tokenS, c);
  }

  public static Test suite() {
    return new Setup();
  }

  private static class Setup extends TestSetup {

    private Setup() {
      super(new TestSuite(DPE1Test.class));
    }

    protected void setUp() throws Exception {
      flowElem = (Element) XmlUtil.parseResource("dpe1.xml", DPE1Test.class).getNode();
    }

    protected void tearDown() throws Exception {
      flowElem = null;
    }
  }
}
