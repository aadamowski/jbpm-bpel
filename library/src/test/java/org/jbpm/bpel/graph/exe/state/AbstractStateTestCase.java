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
package org.jbpm.bpel.graph.exe.state;

import java.util.Date;

import junit.framework.TestCase;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.bpel.graph.basic.Empty;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.exe.Compensator;
import org.jbpm.bpel.graph.exe.FaultInstance;
import org.jbpm.bpel.graph.exe.ScopeInstance;
import org.jbpm.bpel.graph.exe.ScopeState;
import org.jbpm.bpel.graph.scope.Scope;
import org.jbpm.bpel.graph.struct.Flow;
import org.jbpm.bpel.graph.struct.Sequence;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

/**
 * @author Juan Cantu
 * @version $Revision$ $Date: 2008/02/04 14:35:48 $
 */
public abstract class AbstractStateTestCase extends TestCase {

  Token rootToken;
  ScopeInstance scopeInstance;
  TestScopeInstance parentInstance;
  TestScopeInstance activeInstance;
  TestScopeInstance handlingInstance;
  TestScopeInstance completedInstance;
  BpelProcessDefinition processDefinition;
  Scope scope = new Scope();
  Scope parentScope = new Scope();
  LogActivity scopeCompletionLog = new LogActivity();
  LogActivity handlerLog = new LogActivity();

  private JbpmContext jbpmContext;

  protected void setUp() {
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.getInstance("org/jbpm/bpel/graph/exe/test.jbpm.cfg.xml");
    jbpmContext = jbpmConfiguration.createJbpmContext();

    // active scope
    Scope activeScope = new Scope("active");
    activeScope.setActivity(new Empty());

    // handling scope
    Scope handlingScope = new Scope("handling");
    handlingScope.setActivity(new Empty());

    // completed scope
    Scope completedScope = new Scope("completed");
    completedScope.setActivity(new Empty());

    // flow
    Flow flow = new Flow();
    flow.addNode(activeScope);
    flow.addNode(handlingScope);
    flow.addNode(completedScope);

    // scope
    scope.installFaultExceptionHandler();
    scope.setName("scope");
    scope.setActivity(flow);

    // parent scope
    parentScope.setName("parent");
    parentScope.installFaultExceptionHandler();
    parentScope.setActivity(scope);

    // root sequence
    Sequence rootSequence = new Sequence();
    rootSequence.addNode(parentScope);
    rootSequence.addNode(scopeCompletionLog);

    /*
     * the bpel definition uses the jbpm configuration, so create a context before the definition to
     * avoid loading another configuration from the default resource
     */
    processDefinition = new BpelProcessDefinition("process", BpelConstants.NS_EXAMPLES);
    processDefinition.getGlobalScope().setActivity(rootSequence);

    // process instance
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    rootToken = processInstance.getRootToken();

    // parent scope instance
    parentInstance = createScopeInstance(parentScope, rootToken);
    rootToken.setNode(parentScope);

    // scope instance
    Token token = new Token(rootToken, scope.getName());
    token.setNode(scope);
    scopeInstance = scope.createInstance(token);
    scopeInstance.setState(getState());

    // flow token
    Token flowToken = scopeInstance.getPrimaryToken();
    flowToken.setNode(flow);

    // active token
    Token concurrentToken = new Token(flowToken, "1");
    concurrentToken.setNode(activeScope);

    Token scopeToken = new Token(concurrentToken, "active");
    activeInstance = createScopeInstance(activeScope, scopeToken);

    // handling token
    concurrentToken = new Token(flowToken, "2");
    concurrentToken.setNode(handlingScope);

    scopeToken = new Token(concurrentToken, "handling");
    handlingInstance = createScopeInstance(handlingScope, scopeToken);
    handlingInstance.setState(FaultingState.EXPLICIT);

    // completed token
    concurrentToken = new Token(flowToken, "3");
    concurrentToken.setNode(completedScope);

    scopeToken = new Token(concurrentToken, "completed");
    scopeToken.end();
    completedInstance = createScopeInstance(completedScope, scopeToken);
    completedInstance.setState(EndState.COMPLETED);
  }

  protected void tearDown() throws Exception {
    jbpmContext.close();
  }

  public abstract ScopeState getState();

  public void testFaulted() {
    try {
      scopeInstance.faulted(null);
      fail("faulted cannot be invoked at this state");
    }
    catch (IllegalStateException e) {
      // expected exception
    }
  }

  public void testTerminate() {
    try {
      scopeInstance.terminate();
      fail("terminate cannot be invoked at this state");
    }
    catch (IllegalStateException e) {
      // expected exception
    }
  }

  public void testCompleted() {
    try {
      scopeInstance.completed();
      fail("completed cannot be invoked at this state");
    }
    catch (IllegalStateException e) {
      // expected exception
    }
  }

  public void testCompensate() {
    try {
      scopeInstance.compensate(null);
      fail("compensate cannot be invoked at this state");
    }
    catch (IllegalStateException e) {
      // expected exception
    }
  }

  public void testChildrenTerminated() {
    try {
      scopeInstance.getState().childrenTerminated(scopeInstance);
      fail("children terminated cannot be invoked at this state");
    }
    catch (IllegalStateException e) {
      // expected exception
    }
  }

  // compensation won't work since it depends on persistence
  void assertChildrenCompensated() {
    assertNull(activeInstance.compensated);
    assertNull(handlingInstance.compensated);
    assertNotNull(completedInstance.compensated);
  }

  void assertChildrenTerminated() {
    assertTrue(activeInstance.terminated);
    assertTrue(handlingInstance.terminated);
    assertFalse(completedInstance.terminated);
  }

  static TestScopeInstance createScopeInstance(Scope scope, Token token) {
    TestScopeInstance scopeInstance = new TestScopeInstance(scope, token);
    token.getProcessInstance().getContextInstance().createVariable(Scope.VARIABLE_NAME,
        scopeInstance, token);
    return scopeInstance;
  }

  static class TestScopeInstance extends ScopeInstance {

    boolean childCompensated;
    boolean childTerminated;
    boolean childFaulted;

    boolean terminated;
    Date compensated;

    private static final long serialVersionUID = 1L;

    TestScopeInstance(Scope scope, Token token) {
      super(scope, token);
    }

    public void scopeCompensated(ScopeInstance child) {
      childCompensated = true;
    }

    public void scopeTerminated(ScopeInstance child) {
      childTerminated = true;
    }

    public void faulted(FaultInstance faultInstance) {
      childFaulted = true;
    }

    public void terminate() {
      terminated = true;
    }

    public void compensate(Compensator compensator) {
      compensated = new Date();
    }
  }

  static class LogActivity extends Activity {

    boolean executed;

    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext context) {
      executed = true;
    }
  }

}
