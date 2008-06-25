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
package org.jbpm.bpel.persistence.db;

import org.jbpm.bpel.graph.basic.Empty;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.exe.ScopeInstance;
import org.jbpm.bpel.graph.exe.state.EndState;
import org.jbpm.bpel.graph.scope.Scope;
import org.jbpm.bpel.graph.struct.Sequence;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

/*
 * Tests the ScopesSession facade. Proves that the next child to compensate of a scope instance will
 * always be a nested scope instance at any level with a COMPLETED state and the latest completion
 * date
 */
public class ScopeSessionDbTest extends AbstractDbTestCase {

  // root
  // |
  // |-- A
  // | |
  // | |-- A.1
  // | | |
  // | | +-- A.1.1
  // | |
  // | +-- A.2
  // |
  // +-- B
  // |
  // +-- B.1

  ProcessInstance processInstance;
  ScopeInstance root;
  ScopeInstance a;
  ScopeInstance a1;
  ScopeInstance a2;
  ScopeInstance a11;
  ScopeInstance b;
  ScopeInstance b1;

  // MySQL cannot store fractions of a second, so this value was raised to 1000 milliseconds
  private static final int SCOPE_COMPLETION_DELAY = 1000;

  protected void setUp() throws Exception {
    super.setUp();

    // define the process
    Scope scopeA11 = new Scope("A.1.1");
    scopeA11.setActivity(new Empty());

    Scope scopeA1 = new Scope("A.1");
    scopeA1.setActivity(scopeA11);

    Scope scopeA2 = new Scope("A.2");
    scopeA2.setActivity(new Empty());

    Sequence sequenceA = new Sequence();
    sequenceA.addNode(scopeA1);
    sequenceA.addNode(scopeA2);

    Scope scopeA = new Scope("A");
    scopeA.setActivity(sequenceA);

    Scope scopeB1 = new Scope("B.1");
    scopeB1.setActivity(new Empty());

    Scope scopeB = new Scope("B");
    scopeB.setActivity(scopeB1);

    Sequence rootSequence = new Sequence();
    rootSequence.addNode(scopeA);
    rootSequence.addNode(scopeB);

    BpelProcessDefinition processDefinition = new BpelProcessDefinition("scopeSession",
        BpelConstants.NS_EXAMPLES);
    Scope globalScope = processDefinition.getGlobalScope();
    globalScope.setActivity(rootSequence);

    // save the definition
    jbpmContext.getGraphSession().saveProcessDefinition(processDefinition);

    // create a process instance
    processInstance = new ProcessInstance(processDefinition);
    Token rootToken = processInstance.getRootToken();
    root = globalScope.createInstance(rootToken);

    // create scope instances
    Token tokenA = new Token(rootToken, "A");
    tokenA.setNode(scopeA);
    a = scopeA.createInstance(tokenA);

    Token tokenA1 = new Token(tokenA, "A.1");
    tokenA1.setNode(scopeA1);
    a1 = scopeA1.createInstance(tokenA1);

    Token tokenA2 = new Token(tokenA, "A.2");
    new Token(tokenA, "implicit a");
    tokenA2.setNode(scopeA2);
    a2 = scopeA2.createInstance(tokenA2);

    Token tokenA11 = new Token(tokenA1, "A.1.1");
    new Token(tokenA1, "implicit a1");
    tokenA11.setNode(scopeA11);
    a11 = scopeA11.createInstance(tokenA11);

    Token tokenB = new Token(rootToken, "B");
    tokenB.setNode(scopeB);
    b = scopeB.createInstance(tokenB);

    Token tokenB1 = new Token(tokenB, "B.1");
    tokenB1.setNode(scopeB1);
    b1 = scopeB1.createInstance(tokenB1);
  }

  public void testNextChildToCompensate() throws Exception {
    // completion order is T1: A.2, B.1, T2: A.1.1, T3: A.1
    a2.setState(EndState.COMPLETED);
    a2.getToken().end();

    b1.setState(EndState.COMPLETED);
    b1.getToken().end();

    Thread.sleep(SCOPE_COMPLETION_DELAY);
    a11.setState(EndState.COMPLETED);
    a11.getToken().end();

    Thread.sleep(SCOPE_COMPLETION_DELAY);
    a1.setState(EndState.COMPLETED);
    a1.getToken().end();

    // save the process instance
    processInstance = saveAndReload(processInstance);
    ScopeSession scopeSession = ScopeSession.getContextInstance(jbpmContext);

    // our compensation follows the strict reverse order of completion
    assertEquals("A.1", scopeSession.nextChildToCompensate(root).getDefinition().getName());
    assertEquals("A.1", scopeSession.nextChildToCompensate(a).getDefinition().getName());
    assertEquals("B.1", scopeSession.nextChildToCompensate(b).getDefinition().getName());
    assertEquals("A.1.1", scopeSession.nextChildToCompensate(a1).getDefinition().getName());
    assertNull(scopeSession.nextChildToCompensate(a2));
    assertNull(scopeSession.nextChildToCompensate(b1));
    assertNull(scopeSession.nextChildToCompensate(a11));
  }

  public void testNextScopeToCompensate() throws Exception {
    // completion order is T1: A.2, B.1, T2: A.1.1 T3: A.1
    a2.setState(EndState.COMPLETED);
    a2.getToken().end();

    b1.setState(EndState.COMPLETED);
    b1.getToken().end();

    Thread.sleep(SCOPE_COMPLETION_DELAY);
    a11.setState(EndState.COMPLETED);
    a11.getToken().end();

    Thread.sleep(SCOPE_COMPLETION_DELAY);
    a1.setState(EndState.COMPLETED);
    a1.getToken().end();

    // save the process instance
    processInstance = saveAndReload(processInstance);
    ScopeSession scopeSession = ScopeSession.getContextInstance(jbpmContext);

    assertEquals("A.1", scopeSession.nextScopeToCompensate(processInstance, root.getDefinition())
        .getDefinition()
        .getName());
    assertEquals("A.1", scopeSession.nextScopeToCompensate(processInstance, a.getDefinition())
        .getDefinition()
        .getName());
    assertEquals("B.1", scopeSession.nextScopeToCompensate(processInstance, b.getDefinition())
        .getDefinition()
        .getName());
    assertEquals("A.1", scopeSession.nextScopeToCompensate(processInstance, a1.getDefinition())
        .getDefinition()
        .getName());
    assertEquals("A.2", scopeSession.nextScopeToCompensate(processInstance, a2.getDefinition())
        .getDefinition()
        .getName());
    assertEquals("B.1", scopeSession.nextScopeToCompensate(processInstance, b1.getDefinition())
        .getDefinition()
        .getName());
    assertEquals("A.1.1", scopeSession.nextScopeToCompensate(processInstance, a11.getDefinition())
        .getDefinition()
        .getName());
  }
}
