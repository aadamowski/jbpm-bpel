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
public class AcyclicGraphTest extends AbstractExeTestCase {

  /*
   * Based on the graph structure defined by the flow, the activities getBuyerInformation and
   * getSellerInformation can run concurrently. The settleTrade activity is not performed before
   * both of these activities are completed. After settleTrade completes the two activities,
   * confirmBuyer and confirmSeller are performed concurrently again.
   */

  private Flow flow;
  private Receive firstGetActivity;
  private Receive secondGetActivity;
  private Receive settleTradeActivity;
  private Receive firstConfirmActivity;
  private Receive secondConfirmActivity;

  private static Element flowElem;

  public void testFirstScenario() throws Exception {
    flow = (Flow) readActivity(flowElem, false);
    plugInner(flow);
    validateInner("getSellerInformation", "getBuyerInformation", "confirmSeller", "confirmBuyer");
  }

  public void testSecondScenario() throws Exception {
    flow = (Flow) readActivity(flowElem, false);
    plugInner(flow);
    validateInner("getSellerInformation", "getBuyerInformation", "confirmBuyer", "confirmSeller");
  }

  public void testThirdScenario() throws Exception {
    flow = (Flow) readActivity(flowElem, false);
    plugInner(flow);
    validateInner("getBuyerInformation", "getSellerInformation", "confirmSeller", "confirmBuyer");
  }

  public void testFourthScenario() throws Exception {
    flow = (Flow) readActivity(flowElem, false);
    plugInner(flow);
    validateInner("getBuyerInformation", "getSellerInformation", "confirmBuyer", "confirmSeller");
  }

  public void testInitialFirstScenario() throws Exception {
    flow = (Flow) readActivity(flowElem, true);
    plugInitial(flow);
    validateOuter("getSellerInformation", "getBuyerInformation", "confirmSeller", "confirmBuyer");
  }

  public void testInitialSecondScenario() throws Exception {
    flow = (Flow) readActivity(flowElem, true);
    plugInitial(flow);
    validateOuter("getSellerInformation", "getBuyerInformation", "confirmBuyer", "confirmSeller");
  }

  public void testInitialThirdScenario() throws Exception {
    flow = (Flow) readActivity(flowElem, true);
    plugInitial(flow);
    validateOuter("getBuyerInformation", "getSellerInformation", "confirmSeller", "confirmBuyer");
  }

  public void testInitialFourthScenario() throws Exception {
    flow = (Flow) readActivity(flowElem, true);
    plugInitial(flow);
    validateOuter("getBuyerInformation", "getSellerInformation", "confirmBuyer", "confirmSeller");
  }

  private void validateOuter(String firstGetInfo, String secondGetInfo, String firstConfirm,
      String secondConfirm) {
    initActivities(firstGetInfo, secondGetInfo, firstConfirm, secondConfirm);
    firstGetActivity.setCreateInstance(true);
    secondGetActivity.setCreateInstance(true);
    settleTradeActivity.setCreateInstance(true);
    firstConfirmActivity.setCreateInstance(true);
    secondConfirmActivity.setCreateInstance(true);

    Token startToken = executeInitial(firstGetActivity.getReceiveAction());

    // first receive was started, it must be at the end of the flow
    // TODO remove this search?
    // Token firstGetToken = findToken( startToken, flow.getEnd() );

    Token secondGetToken = findToken(startToken, secondGetActivity);
    Token settleTradeToken = findToken(startToken, settleTradeActivity);
    Token firstConfirmToken = findToken(startToken, firstConfirmActivity);
    Token secondConfirmToken = findToken(startToken, secondConfirmActivity);

    // validate that settleTrade, first and second confirm can't be executed and
    // remain in their node
    assertReceiveDisabled(firstConfirmToken, firstConfirmActivity);
    assertReceiveDisabled(secondConfirmToken, secondConfirmActivity);
    assertReceiveDisabled(settleTradeToken, settleTradeActivity);

    // secondGetActivity is triggered, It must move to the flow's end.
    assertReceiveAndAdvance(secondGetToken, secondGetActivity, flow.getEnd());

    // validate that first and second confirm can't be executed and remain in
    // their node
    assertReceiveDisabled(firstConfirmToken, firstConfirmActivity);
    assertReceiveDisabled(secondConfirmToken, secondConfirmActivity);

    // validate that settleTradeToken can be executed. settleTradeToken moved to
    // the flow's end.
    assertReceiveAndAdvance(settleTradeToken, settleTradeActivity, flow.getEnd());

    // validate that first and second confirm can be executed. first moves to
    // the flow end, second completes.
    assertReceiveAndAdvance(firstConfirmToken, firstConfirmActivity, flow.getEnd());
    assertReceiveAndComplete(secondConfirmToken, secondConfirmActivity);
  }

  private void validateInner(String firstGetInfo, String secondGetInfo, String firstConfirm,
      String secondConfirm) {
    initActivities(firstGetInfo, secondGetInfo, firstConfirm, secondConfirm);
    Token startToken = executeInner();

    // tokens for every activity where created upon activation.
    Token firstGetToken = findToken(startToken, firstGetActivity);
    Token secondGetToken = findToken(startToken, secondGetActivity);
    Token settleTradeToken = findToken(startToken, settleTradeActivity);
    Token firstConfirmToken = findToken(startToken, firstConfirmActivity);
    Token secondConfirmToken = findToken(startToken, secondConfirmActivity);

    // validate that settleTrade, first and second confirm can't be executed and
    // remain in their node
    assertReceiveDisabled(firstConfirmToken, firstConfirmActivity);
    assertReceiveDisabled(secondConfirmToken, secondConfirmActivity);
    assertReceiveDisabled(settleTradeToken, settleTradeActivity);

    // firstGetActivity is triggered, It must move to the flow's end.
    assertReceiveAndAdvance(firstGetToken, firstGetActivity, flow.getEnd());

    // validate that settleTrade, first and second confirm can't be executed and
    // remain in their node
    assertReceiveDisabled(firstConfirmToken, firstConfirmActivity);
    assertReceiveDisabled(secondConfirmToken, secondConfirmActivity);
    assertReceiveDisabled(settleTradeToken, settleTradeActivity);

    // secondGetActivity is triggered, It must move to the flow's end.
    assertReceiveAndAdvance(secondGetToken, secondGetActivity, flow.getEnd());

    // validate that first and second confirm can't be executed and remain in
    // their node
    assertReceiveDisabled(firstConfirmToken, firstConfirmActivity);
    assertReceiveDisabled(secondConfirmToken, secondConfirmActivity);

    // validate that settleTradeToken can be executed. settleTradeToken moved to
    // the flow's end.
    assertReceiveAndAdvance(settleTradeToken, settleTradeActivity, flow.getEnd());

    // validate that first and second confirm can be executed. first moves to
    // the flow end, second completes.
    assertReceiveAndAdvance(firstConfirmToken, firstConfirmActivity, flow.getEnd());
    assertReceiveAndComplete(secondConfirmToken, secondConfirmActivity);
  }

  private void initActivities(String firstGetInfo, String secondGetInfo, String firstConfirm,
      String secondConfirm) {
    firstGetActivity = (Receive) flow.getNode(firstGetInfo);
    secondGetActivity = (Receive) flow.getNode(secondGetInfo);
    settleTradeActivity = (Receive) flow.getNode("settleTrade");
    firstConfirmActivity = (Receive) flow.getNode(firstConfirm);
    secondConfirmActivity = (Receive) flow.getNode(secondConfirm);
  }

  public static Test suite() {
    return new Setup();
  }

  private static class Setup extends TestSetup {

    private Setup() {
      super(new TestSuite(AcyclicGraphTest.class));
    }

    protected void setUp() throws Exception {
      flowElem = (Element) XmlUtil.parseResource("acyclicGraph.bpel", AcyclicGraphTest.class)
          .getNode();
    }

    protected void tearDown() throws Exception {
      flowElem = null;
    }
  }
}
