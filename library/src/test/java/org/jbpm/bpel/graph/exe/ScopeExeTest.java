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

import java.util.Iterator;

import com.ibm.wsdl.MessageImpl;

import org.jbpm.bpel.graph.basic.Receive;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.exe.ScopeInstance;
import org.jbpm.bpel.graph.exe.ScopeInstanceIterator;
import org.jbpm.bpel.graph.exe.state.ActiveState;
import org.jbpm.bpel.graph.exe.state.EndState;
import org.jbpm.bpel.graph.scope.Catch;
import org.jbpm.bpel.graph.scope.OnEvent;
import org.jbpm.bpel.graph.scope.Scope;
import org.jbpm.bpel.variable.def.MessageType;
import org.jbpm.bpel.variable.def.VariableDefinition;
import org.jbpm.graph.exe.Token;

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2008/02/01 05:43:07 $
 */
public class ScopeExeTest extends AbstractExeTestCase {

  Scope scopeActivity;

  public void testGetDefinition() throws Exception {
    String xml = "<scope name='1'>"
        + " <scope name='1.1'>"
        + "   <scope name='1.1.1'>"
        + "     <empty/>"
        + "   </scope>"
        + " </scope>"
        + "</scope>";
    scopeActivity = (Scope) readActivity(xml, false);
    plugInner(scopeActivity);
    Token token = executeInner();
    ScopeInstance s1 = Scope.getInstance((Token) token.getChildren()
        .values()
        .iterator()
        .next());
    assertEquals("1", s1.getDefinition().getName());
    ScopeInstance s11 = (ScopeInstance) new ScopeInstanceIterator(s1.getToken()).next();
    assertEquals("1.1", s11.getDefinition().getName());
    ScopeInstance s111 = (ScopeInstance) new ScopeInstanceIterator(
        s11.getToken()).next();
    assertEquals("1.1.1", s111.getDefinition().getName());
  }

  public void testNormalExecution() throws Exception {
    String xml = "<scope>"
        + " <receive partnerLink='aPartner' operation='o' />"
        + "</scope>";
    scopeActivity = (Scope) readActivity(xml, false);
    Receive receive = (Receive) scopeActivity.getActivity();
    plugInner(scopeActivity);

    Token token = executeInner();
    assertEquals(scopeActivity, token.getNode());
    Token scopeToken = (Token) token.getChildren().values().iterator().next();
    Token normalFlowToken = (Token) scopeToken.getChildren()
        .values()
        .iterator()
        .next();

    assertEquals(receive, normalFlowToken.getNode());

    ScopeInstance scopeInstance = Scope.getInstance(scopeToken);
    assertEquals(scopeToken, scopeInstance.getToken());
    assertNotNull(scopeInstance);
    assertEquals(scopeInstance.getState(),
        ActiveState.PRIMARY);

    assertReceiveAndComplete(normalFlowToken, receive);
    assertEquals(EndState.COMPLETED, scopeInstance.getState());

    // parent token advanced and completed
    assertCompleted(token);
  }

  public void testEventsExecutionFirstScenario() throws Exception {
    String xml = "<scope>"
        + " <receive partnerLink='aPartner' operation='o' />"
        + "<eventHandlers>"
        + " <onEvent partnerLink='aPartner' operation='o' >"
        + "   <receive partnerLink='aPartner' operation='o' />"
        + " </onEvent>"
        + " <onEvent partnerLink='aPartner' operation='o' >"
        + "   <receive partnerLink='aPartner' operation='o' />"
        + " </onEvent>"
        + "</eventHandlers>"
        + "</scope>";
    scopeActivity = (Scope) readActivity(xml, false);
    plugInner(scopeActivity);

    Iterator eventsIt = scopeActivity.getOnEvents().iterator();
    OnEvent firstEvent = (OnEvent) eventsIt.next();
    OnEvent secondEvent = (OnEvent) eventsIt.next();

    Token token = executeInner();
    Token scopeToken = (Token) token.getChildren().values().iterator().next();
    ScopeInstance scopeInstance = Scope.getInstance(scopeToken);

    firstEvent.messageReceived(firstEvent.getReceiveAction(), scopeToken);
    // first event activity is completed
    Token firstEventToken = scopeInstance.getEventToken(0);
    assertEventAndComplete(firstEventToken, (Receive) firstEvent.getActivity());

    secondEvent.messageReceived(secondEvent.getReceiveAction(), scopeToken);

    // execute scope receive, scope must reach an events pending state
    Receive receive = (Receive) scopeActivity.getActivity();
    Token activityToken = scopeInstance.getPrimaryToken();
    receive.messageReceived(receive.getReceiveAction(), activityToken);
    assertEquals(ActiveState.EVENTS, scopeInstance.getState());

    // a new message arrives to the first event. It must be rejected
    int beforeEventCount = scopeInstance.getEventTokens().size();
    firstEvent.messageReceived(firstEvent.getReceiveAction(), scopeToken);
    assertEquals(beforeEventCount, scopeInstance.getEventTokens().size());

    // second event activity is completed
    Token secondEventToken = scopeInstance.getEventToken(1);
    assertEventAndComplete(secondEventToken,
        (Receive) secondEvent.getActivity());

    // parent token advanced and completed
    assertCompleted(token);
  }

  public void testEventsExecutionSecondScenario() throws Exception {
    String xml = "<scope>"
        + " <receive partnerLink='aPartner' operation='o' />"
        + "<eventHandlers>"
        + " <onEvent partnerLink='aPartner' operation='o' >"
        + "   <receive partnerLink='aPartner' operation='o' />"
        + " </onEvent>"
        + "</eventHandlers>"
        + "</scope>";
    scopeActivity = (Scope) readActivity(xml, false);
    plugInner(scopeActivity);

    OnEvent firstEvent = (OnEvent) scopeActivity.getOnEvents()
        .iterator()
        .next();

    // set the handler variable
    VariableDefinition variable = new VariableDefinition();
    variable.setName("v");
    variable.setType(new MessageType(new MessageImpl()));
    firstEvent.setVariableDefinition(variable);

    Token token = executeInner();
    Token scopeToken = (Token) token.getChildren().values().iterator().next();
    ScopeInstance scopeInstance = Scope.getInstance(scopeToken);
    Token activityToken = scopeInstance.getPrimaryToken();

    firstEvent.messageReceived(firstEvent.getReceiveAction(), scopeToken);
    Token firstEventToken = scopeInstance.getEventToken(0);

    // first event activity is completed
    assertEventAndComplete(firstEventToken, (Receive) firstEvent.getActivity());

    // execute scope receive, scope must complete
    Receive receive = (Receive) scopeActivity.getActivity();
    receive.messageReceived(receive.getReceiveAction(), activityToken);

    // parent token advanced and completed
    assertEquals(EndState.COMPLETED, scopeInstance.getState());
    assertCompleted(token);
  }

  public void testFaultWithoutHandler() throws Exception {
    String xml = "<scope>"
        + "<sequence>"
        + "<scope>"
        + "  <empty/>"
        + "</scope>"
        + "<flow>"
        + "  <throw faultName='someFault'/>"
        + "  <receive name='uselessReceive' partnerLink='aPartner' operation='o'/>"
        + "</flow>"
        + "</sequence>"
        + "</scope>";

    scopeActivity = (Scope) readActivity(xml, false);
    plugInner(scopeActivity);

    Token token = executeInner();
    Token scopeToken = (Token) token.getChildren().values().iterator().next();
    ScopeInstance scopeInstance = Scope.getInstance(scopeToken);

    ScopeInstanceIterator childrenIt = new ScopeInstanceIterator(scopeToken);
    assertTrue(childrenIt.hasNext());
    // compensation won't work since persistence is disabled.
    // assertEquals(EndedState.COMPENSATED,
    // ((ScopeInstance)childrenIt.next()).getState());
    assertEquals(EndState.COMPLETED,
        ((ScopeInstance) childrenIt.next()).getState());

    // parent token advanced and completed abnormally
    assertEquals(EndState.FAULTED, scopeInstance.getState());
    assertTrue(token.hasEnded());
  }

  public void testFaultWithHandler() throws Exception {
    String xml = "<scope>"
        + "  <sequence>"
        + "    <scope>"
        + "      <empty/>"
        + "    </scope>"
        + "    <throw faultName='someFault' />"
        + "  </sequence>"
        + "  <faultHandlers>"
        + "   <catch faultName='someFault'>"
        + "     <receive partnerLink='aPartner' operation='o'/>"
        + "   </catch>"
        + "  </faultHandlers>"
        + "</scope>";

    scopeActivity = (Scope) readActivity(xml, false);
    plugInner(scopeActivity);
    Catch catcher = (Catch) scopeActivity.getFaultHandlers().iterator().next();
    Receive handlerReceive = (Receive) catcher.getActivity();

    Token token = executeInner();
    assertNull(token.getEnd());
    Token scopeToken = (Token) token.getChildren().values().iterator().next();
    ScopeInstance scopeInstance = Scope.getInstance(scopeToken);
    Token activityToken = scopeInstance.getPrimaryToken();

    assertEquals(1, activityToken.getChildren().size());

    ScopeInstanceIterator childrenIt = new ScopeInstanceIterator(scopeToken);
    assertTrue(childrenIt.hasNext());
    assertEquals(EndState.COMPLETED,
        ((ScopeInstance) childrenIt.next()).getState());
    assertFalse(childrenIt.hasNext());

    // fault receive is completed
    Token handlerToken = scopeInstance.getHandlerToken();
    assertNotNull(handlerToken);
    // TODO test that a fault variable instance is created inside the handler
    // flow token
    handlerReceive.messageReceived(handlerReceive.getReceiveAction(), handlerToken);

    // parent token exited and has ended
    assertEquals(EndState.EXITED, scopeInstance.getState());
    assertTrue(token.hasEnded());
  }

  protected void assertReceiveAndAdvance(Token token, Receive sourceNode,
      Activity targetNode) {
    Receive activity = (Receive) token.getNode();
    assertEquals(sourceNode, activity);
    activity.messageReceived(activity.getReceiveAction(), token);
    assertSame(targetNode, token.getNode());
  }

  protected void assertEventAndComplete(Token token, Receive sourceNode) {
    Receive activity = (Receive) token.getNode();
    assertEquals(sourceNode, activity);
    activity.messageReceived(activity.getReceiveAction(), token);
    assertCompleted(token);
  }
}