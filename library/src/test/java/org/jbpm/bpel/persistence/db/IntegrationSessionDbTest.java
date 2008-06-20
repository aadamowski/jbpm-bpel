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

import java.util.Collection;
import java.util.List;

import javax.xml.transform.dom.DOMSource;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.jbpm.bpel.graph.basic.Receive;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.scope.OnEvent;
import org.jbpm.bpel.graph.scope.Scope;
import org.jbpm.bpel.graph.struct.Pick;
import org.jbpm.bpel.graph.struct.Sequence;
import org.jbpm.bpel.graph.struct.StructuredActivity.Begin;
import org.jbpm.bpel.integration.def.ReceiveAction;
import org.jbpm.bpel.xml.BpelReader;
import org.jbpm.bpel.xml.util.XmlUtil;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/11/25 13:03:15 $
 */
public class IntegrationSessionDbTest extends AbstractDbTestCase {

  private BpelProcessDefinition processDefinition;
  private IntegrationSession integrationSession;

  private static DOMSource processSource;

  protected void setUp() throws Exception {
    super.setUp();

    processDefinition = new BpelProcessDefinition();

    BpelReader bpelReader = new BpelReader();
    bpelReader.read(processDefinition, processSource);
    assertEquals(0, bpelReader.getProblemHandler().getProblemCount());

    bpelGraphSession.deployProcessDefinition(processDefinition);
    Scope globalScope = processDefinition.getGlobalScope();

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    Token rootToken = processInstance.getRootToken();
    // place root token in global scope
    rootToken.setNode(processDefinition.getGlobalScope());

    // event handler
    Token eventsToken = new Token(rootToken, "events");
    // the scope activity places the events token in itself
    eventsToken.setNode(globalScope);

    Sequence main = (Sequence) globalScope.getActivity();
    Token activityToken = new Token(rootToken, "activity");
    activityToken.setNode(main);

    // receive
    Receive receive = (Receive) main.getNode("r");
    Token receiveToken = new Token(activityToken, "r");
    receiveToken.setNode(receive);

    // pick
    Pick pick = (Pick) main.getNode("p");
    Token pickToken = new Token(rootToken, "p");
    // the pick activity places the token in its begin node
    pickToken.setNode(pick.getBegin());

    /*
     * Place a token in an activity that executes upon picking a message. The parent of said
     * activity is the pick. We do not want this token; we want tokens in begin nodes
     */
    Activity onMsgActivity = (Activity) pick.getNodes().get(0);
    Token onMsgToken = new Token(pickToken, "on");
    onMsgToken.setNode(onMsgActivity);

    jbpmContext.save(processInstance);
    integrationSession = IntegrationSession.getContextInstance(jbpmContext);
  }

  public void testFindReceiveTokens() {
    Collection tokens = integrationSession.findReceiveTokens(processDefinition);
    assertEquals(1, tokens.size());

    Token token = (Token) tokens.iterator().next();
    assertEquals("r", token.getName());

    Receive receive = (Receive) token.getNode();
    assertEquals("r", receive.getName());

    ReceiveAction receiveAction = receive.getReceiveAction();
    assertEquals("schedulingPL", receiveAction.getPartnerLink().getName());
    assertEquals("returnScheduleTicket", receiveAction.getOperation().getName());
  }

  public void testFindPickTokens() {
    Collection tokens = integrationSession.findPickTokens(processDefinition);
    assertEquals(1, tokens.size());

    Token token = (Token) tokens.iterator().next();
    assertEquals("p", token.getName());

    Begin begin = (Begin) token.getNode();
    Pick pick = (Pick) begin.getCompositeActivity();
    assertEquals("p", pick.getName());

    List onMessages = pick.getOnMessages();
    assertEquals(2, onMessages.size());

    ReceiveAction receiver1 = (ReceiveAction) onMessages.get(0);
    assertEquals("schedulingPL", receiver1.getPartnerLink().getName());
    assertEquals("requestScheduling", receiver1.getOperation().getName());

    ReceiveAction receiver2 = (ReceiveAction) onMessages.get(1);
    assertEquals("schedulingPL", receiver2.getPartnerLink().getName());
    assertEquals("sendShippingSchedule", receiver2.getOperation().getName());
  }

  public void testFindEventTokens() {
    Collection tokens = integrationSession.findEventTokens(processDefinition);
    assertEquals(1, tokens.size());

    Token token = (Token) tokens.iterator().next();
    assertEquals("events", token.getName());

    Scope scope = (Scope) token.getNode();

    Collection onEvents = scope.getOnEvents();
    assertEquals(1, onEvents.size());

    OnEvent onEvent = (OnEvent) onEvents.iterator().next();
    ReceiveAction receiveAction = onEvent.getReceiveAction();
    assertEquals("schedulingPL", receiveAction.getPartnerLink().getName());
    assertEquals("cancelScheduling", receiveAction.getOperation().getName());
  }

  public static Test suite() {
    return new Setup();
  }

  private static class Setup extends TestSetup {

    private Setup() {
      super(new TestSuite(IntegrationSessionDbTest.class));
    }

    protected void setUp() throws Exception {
      processSource = XmlUtil.parseResource("integrationSession.bpel",
          IntegrationSessionDbTest.class);
    }

    protected void tearDown() throws Exception {
      processSource = null;
    }
  }
}