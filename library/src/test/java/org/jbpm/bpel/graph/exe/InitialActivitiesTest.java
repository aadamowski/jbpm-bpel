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

import java.util.ArrayList;
import java.util.List;

import org.jbpm.bpel.graph.basic.Receive;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.scope.Scope;
import org.jbpm.bpel.graph.struct.Flow;
import org.jbpm.bpel.graph.struct.Pick;
import org.jbpm.bpel.graph.struct.Sequence;
import org.jbpm.bpel.graph.struct.StructuredActivity;
import org.jbpm.bpel.integration.def.InboundMessageActivity;
import org.jbpm.bpel.integration.def.ReceiveAction;
import org.jbpm.graph.exe.Token;

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/07/26 00:39:11 $
 */
public class InitialActivitiesTest extends AbstractExeTestCase {

  private List receiveActions;
  private Receive a;
  private Pick b;
  private Receive c;
  private Receive d;
  private Receive e;
  private Receive f;

  static final String xml = "<flow>"
      + "	<receive name='a' partnerLink='aPartner' operation='o' createInstance='yes'/>"
      + " <pick name='b' createInstance='yes'>"
      + "		<onMessage partnerLink='aPartner' operation='o'>"
      + " 		<empty/>"
      + " 	</onMessage>"
      + "		<onMessage partnerLink='aPartner' operation='o'>"
      + " 		<empty/>"
      + " 	</onMessage>"
      + " </pick>"
      + " <sequence name='sequence'>"
      + "		<receive name='c' partnerLink='aPartner' operation='o' createInstance='yes'/>"
      + "		<empty/>"
      + " </sequence>"
      + " <flow name='flow'>"
      + "		<receive name='d' partnerLink='aPartner' operation='o' createInstance='yes'/>"
      + "		<receive name='e' partnerLink='aPartner' operation='o' createInstance='yes'/>"
      + " </flow>"
      + " <scope name='localScope'>"
      + "  <receive name='f' partnerLink='aPartner' operation='o' createInstance='yes'/>"
      + " </scope>"
      + "</flow>";

  protected void setUp() throws Exception {
    super.setUp();

    StructuredActivity root = (StructuredActivity) readActivity(xml, true);
    plugInitial(root);

    // Create a list with receiveActions able to trigger new instances
    receiveActions = new ArrayList();

    a = (Receive) root.getNode("a");
    receiveActions.add(a.getReceiveAction());

    b = ((Pick) root.getNode("b"));
    receiveActions.addAll(b.getOnMessages());

    Sequence sequence = (Sequence) root.getNode("sequence");
    c = ((Receive) sequence.getNode("c"));
    receiveActions.add(c.getReceiveAction());

    Flow flow = (Flow) root.getNode("flow");
    d = (Receive) flow.getNode("d");
    receiveActions.add(d.getReceiveAction());
    e = (Receive) flow.getNode("e");
    receiveActions.add(e.getReceiveAction());

    Scope scope = (Scope) root.getNode("localScope");
    f = (Receive) scope.getActivity();
    receiveActions.add(f.getReceiveAction());
  }

  public void testReceive() {
    validateOtherReceivers(a.getReceiveAction());
  }

  public void testSequenceReceive() {
    validateOtherReceivers(c.getReceiveAction());
  }

  public void testPickFirstMessage() {
    List onMessages = b.getOnMessages();
    receiveActions.remove(onMessages.get(1));
    validateOtherReceivers((ReceiveAction) onMessages.get(0));
  }

  public void testPickSecondMessage() {
    List onMessages = b.getOnMessages();
    receiveActions.remove(onMessages.get(0));
    validateOtherReceivers((ReceiveAction) onMessages.get(1));
  }

  public void testFlowFirstReceive() {
    validateOtherReceivers(d.getReceiveAction());
  }

  public void testFlowSecondReceive() {
    validateOtherReceivers(e.getReceiveAction());
  }

  public void testScopeReceive() {
    validateOtherReceivers(f.getReceiveAction());
  }

  /*
   * validate for every receiver that when a new instance is triggered, a token
   * is created for the rest of the receivers.
   */
  private void validateOtherReceivers(ReceiveAction trigger) {
    final Token parentToken = executeInitial(trigger).getProcessInstance()
        .getRootToken();
    for (int i = 0, n = receiveActions.size(); i < n; i++) {
      ReceiveAction receiveAction = (ReceiveAction) receiveActions.get(i);
      if (receiveAction.equals(trigger))
        continue;

      InboundMessageActivity messageActivity = receiveAction.getInboundMessageActivity();
      Activity node = messageActivity instanceof Pick ? ((Pick) messageActivity).getBegin()
          : (Activity) messageActivity;
      Token token = (Token) parentToken.getChildrenAtNode(node).get(0);
      assertReceiverEnabled(token, receiveAction);
    }
  }
}
