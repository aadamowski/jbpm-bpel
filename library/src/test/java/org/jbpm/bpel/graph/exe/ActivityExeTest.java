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

import junit.framework.TestCase;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.bpel.graph.basic.Empty;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.def.LinkDefinition;
import org.jbpm.bpel.sublang.def.Expression;
import org.jbpm.bpel.sublang.def.JoinCondition;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.graph.def.Transition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/08/08 11:09:17 $
 */
public class ActivityExeTest extends TestCase {

  protected Activity node;
  protected Activity successor;
  protected TestTransition testTransition;
  protected Token token;
  protected ExecutionContext context;
  protected LinkDefinition targetLink;
  protected LinkInstance target;

  protected LinkDefinition positiveLink;
  protected LinkDefinition negativeLink;
  protected LinkDefinition defaultLink;

  private JbpmContext jbpmContext;

  protected static Expression TRUE_EXPRESSION = createBooleanExpression(true);
  protected static Expression FALSE_EXPRESSION = createBooleanExpression(false);

  static JoinCondition TRUE_JOIN_CONDITION = createJoinCondition(true);
  static JoinCondition FALSE_JOIN_CONDITION = createJoinCondition(false);

  protected void setUp() {
    /*
     * the process definition accesses the jbpm configuration, so create a context before creating a
     * process definition to avoid loading another configuration from the default resource
     */
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.getInstance("org/jbpm/bpel/graph/exe/test.jbpm.cfg.xml");
    jbpmContext = jbpmConfiguration.createJbpmContext();

    BpelProcessDefinition pd = new BpelProcessDefinition("testDef", BpelConstants.NS_EXAMPLES);
    ProcessInstance pi = new ProcessInstance(pd);
    token = pi.getRootToken();
    context = new ExecutionContext(token);

    node = createBpelActivity();
    pd.getGlobalScope().addNode(node);

    testTransition = new TestTransition();
    node.addLeavingTransition(testTransition);
    successor = new Empty("successor");

    targetLink = new LinkDefinition("target");
    node.addTarget(targetLink);
    target = targetLink.createInstance(token);

    positiveLink = new LinkDefinition("positive");
    node.addSource(positiveLink);
    successor.addTarget(positiveLink);
    positiveLink.setTransitionCondition(TRUE_EXPRESSION);
    positiveLink.createInstance(token);

    negativeLink = new LinkDefinition("negative");
    node.addSource(negativeLink);
    successor.addTarget(negativeLink);
    negativeLink.setTransitionCondition(FALSE_EXPRESSION);
    negativeLink.createInstance(token);

    defaultLink = new LinkDefinition("default");
    node.addSource(defaultLink);
    successor.addTarget(defaultLink);
    defaultLink.createInstance(token);
  }

  protected void tearDown() throws Exception {
    jbpmContext.close();
  }

  protected Activity createBpelActivity() {
    Activity basic = new Empty("basic");
    return basic;
  }

  public void testHandleTokenNoTargets() {
    node.getTargets().remove(node.getTarget("target"));
    enter();
    assertCompleted();
  }

  public void testHandleTokenDefaultUnset() {
    enter();
    assertNotCompleted();
  }

  public void testHandleTokenDefaultPositive() {
    target.setStatus(Boolean.TRUE);
    enter();
    assertCompleted();
  }

  public void testHandleTokenDefaultNegativeFail() {
    target.setStatus(Boolean.FALSE);
    node.setSuppressJoinFailure(Boolean.FALSE);
    try {
      enter();
      fail("join failure exception must be thrown when join condition is false");
    }
    catch (BpelFaultException e) {
      assertNotCompleted();
    }
  }

  public void testHandleTokenDefaultNegativeSuppress() {
    target.setStatus(Boolean.FALSE);
    node.setSuppressJoinFailure(Boolean.TRUE);
    enter();
    assertSkipped();
  }

  public void testHandleTokenTrueJoinCondition() {
    target.setStatus(Boolean.FALSE);
    node.setJoinCondition(TRUE_JOIN_CONDITION);
    enter();
    assertCompleted();
  }

  public void testHandleTokenFalseJoinConditionFail() {
    target.setStatus(Boolean.TRUE);
    node.setSuppressJoinFailure(Boolean.FALSE);
    node.setJoinCondition(FALSE_JOIN_CONDITION);
    try {
      enter();
      fail("join failure exception must be thrown when join condition is false");
    }
    catch (BpelFaultException e) {
      assertNotCompleted();
    }
  }

  public void testHandleTokenFalseJoinConditionSuppress() {
    target.setStatus(Boolean.TRUE);
    node.setJoinCondition(FALSE_JOIN_CONDITION);
    node.setSuppressJoinFailure(Boolean.TRUE);
    enter();
    assertSkipped();
  }

  public void testLinkResolvedPositive() throws Exception {
    target.setStatus(Boolean.TRUE);

    Token firstChild = new Token(token, "firstChild");
    setTokenAtActivity(firstChild);
    Token secondChild = new Token(token, "secondChild");

    target.setTargetToken(firstChild);
    targetLink.determineStatus(secondChild);
    assertCompleted();
  }

  public void testLinkResolvedNegativeSuppress() throws Exception {
    Token firstChild = new Token(token, "firstChild");
    Token currentToken = new Token(firstChild, "current");
    Token secondChild = new Token(token, "secondChild");
    Token secondGrandChild = new Token(secondChild, "secondGrandChild");
    // TODO remove this instantiation?
    new Token(secondChild, "firstGrandChild");

    node.setSuppressJoinFailure(Boolean.TRUE);

    // currentToken is waiting at the node. It is also the target token
    target.setTargetToken(currentToken);
    setTokenAtActivity(currentToken);

    targetLink.getInstance(secondGrandChild).statusDetermined(false);
    assertSkipped();
  }

  public void testLinkResolvedNegativeFail() throws Exception {
    target.setStatus(Boolean.FALSE);

    Token firstChild = new Token(token, "firstChild");
    setTokenAtActivity(firstChild);
    Token secondChild = new Token(token, "secondChild");

    node.setSuppressJoinFailure(Boolean.FALSE);
    target.setTargetToken(firstChild);

    try {
      targetLink.getInstance(secondChild).statusDetermined(false);
      fail("join failure exception must be thrown when join condition is false");
    }
    catch (BpelFaultException e) {
      assertNotCompleted();
    }
  }

  private void enter() {
    getExeNode().enter(context);
  }

  private void setTokenAtActivity(Token aToken) {
    aToken.setNode(getExeNode());
  }

  protected Activity getExeNode() {
    return node;
  }

  protected void assertCompleted() {
    // TODO assert node handlerLog as completed
    assertEquals(Boolean.TRUE, positiveLink.getInstance(token).getStatus());
    assertEquals(Boolean.FALSE, negativeLink.getInstance(token).getStatus());
    assertEquals(Boolean.TRUE, defaultLink.getInstance(token).getStatus());
    assertEquals(1, testTransition.getCallCount());
  }

  protected void assertNotCompleted() {
    // TODO assert node handlerLog as not completed
    assertEquals(null, positiveLink.getInstance(token).getStatus());
    assertEquals(null, negativeLink.getInstance(token).getStatus());
    assertEquals(null, defaultLink.getInstance(token).getStatus());
    assertEquals(0, testTransition.getCallCount());
  }

  protected void assertSkipped() {
    // TODO assert node handlerLog as skipped
    assertEquals(Boolean.FALSE, positiveLink.getInstance(token).getStatus());
    assertEquals(Boolean.FALSE, negativeLink.getInstance(token).getStatus());
    assertEquals(Boolean.FALSE, defaultLink.getInstance(token).getStatus());
    assertEquals(1, testTransition.getCallCount());
  }

  private static Expression createBooleanExpression(boolean value) {
    Expression expr = new Expression();
    expr.setText(value ? "true()" : "false()");
    return expr;
  }

  private static JoinCondition createJoinCondition(boolean value) {
    JoinCondition joinCondition = new JoinCondition();
    joinCondition.setText(value ? "true()" : "false()");
    return joinCondition;
  }

  private static class TestTransition extends Transition {

    private static final long serialVersionUID = 1L;
    int callCount = 0;

    public void take(ExecutionContext context) {
      callCount++;
    }

    public int getCallCount() {
      return callCount;
    }
  }
}
