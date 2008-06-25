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

import javax.wsdl.Definition;
import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.w3c.dom.Element;

import com.ibm.wsdl.Constants;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.bpel.alarm.AlarmAction;
import org.jbpm.bpel.graph.basic.Receive;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.def.CompositeActivity;
import org.jbpm.bpel.graph.def.LinkDefinition;
import org.jbpm.bpel.graph.exe.state.EndState;
import org.jbpm.bpel.graph.scope.Scope;
import org.jbpm.bpel.graph.struct.Sequence;
import org.jbpm.bpel.integration.def.PartnerLinkDefinition;
import org.jbpm.bpel.integration.def.ReceiveAction;
import org.jbpm.bpel.wsdl.PartnerLinkType;
import org.jbpm.bpel.wsdl.xml.WsdlConstants;
import org.jbpm.bpel.wsdl.xml.WsdlUtil;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.bpel.xml.BpelReader;
import org.jbpm.bpel.xml.util.XmlUtil;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.scheduler.SchedulerService;

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2008/02/01 05:43:07 $
 */
public abstract class AbstractExeTestCase extends TestCase {

  protected BpelReader reader;

  protected BpelProcessDefinition pd;
  protected Receive firstActivity;
  protected Scope scope = new TestScope();
  protected ProcessInstance pi;

  private JbpmContext jbpmContext;

  private static final String WSDL_TEXT = "<definitions targetNamespace='"
      + BpelConstants.NS_EXAMPLES
      + "' xmlns:tns='"
      + BpelConstants.NS_EXAMPLES
      + "' xmlns:plt='"
      + WsdlConstants.NS_PLNK
      + "' xmlns:xsd='"
      + BpelConstants.NS_XML_SCHEMA
      + "' xmlns='"
      + Constants.NS_URI_WSDL
      + "'>"
      + " <message name='msg'>"
      + "  <part name='p1' type='xsd:int' />"
      + " </message>"
      + " <portType name='ppt'>"
      + "  <operation name='o'>"
      + "   <input message='tns:msg' />"
      + "  </operation>"
      + " </portType>"
      + " <portType name='mpt'>"
      + "  <operation name='o'>"
      + "   <input message='tns:msg' />"
      + "  </operation>"
      + " </portType>"
      + " <plt:partnerLinkType name='plt'>"
      + "  <plt:role name='role1' portType='tns:ppt' />"
      + "  <plt:role name='role2' portType='tns:mpt' />"
      + " </plt:partnerLinkType>"
      + "</definitions>";

  protected void setUp() throws Exception {
    /*
     * the reader accesses the jbpm configuration, so create a context before creating the reader to
     * avoid loading another configuration from the default resource
     */
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.getInstance("org/jbpm/bpel/graph/exe/test.jbpm.cfg.xml");
    jbpmContext = jbpmConfiguration.createJbpmContext();

    reader = new BpelReader() {

      public ReceiveAction readReceiveAction(Element receiveElem, CompositeActivity parent) {
        ReceiveAction receiveAction = new ReceiveAction();

        // partner link
        String partnerLinkName = receiveElem.getAttribute(BpelConstants.ATTR_PARTNER_LINK);
        PartnerLinkDefinition partnerLink = parent.findPartnerLink(partnerLinkName);
        receiveAction.setPartnerLink(partnerLink);

        // operation
        String operationName = receiveElem.getAttribute(BpelConstants.ATTR_OPERATION);
        receiveAction.setOperation(partnerLink.getMyRole().getPortType().getOperation(
            operationName, null, null));

        return receiveAction;
      }

      public AlarmAction readAlarmAction(Element alarmElem, CompositeActivity parent) {
        return new TestAlarm();
      }
    };

    pd = new BpelProcessDefinition("pd", BpelConstants.NS_EXAMPLES);
    scope.installFaultExceptionHandler();
    pd.addNode(scope);

    Definition def = WsdlUtil.readText(WSDL_TEXT);
    PartnerLinkType plt = WsdlUtil.getPartnerLinkType(def, new QName(BpelConstants.NS_EXAMPLES,
        "plt"));

    pd.getImportDefinition().addImport(WsdlUtil.createImport(def));

    PartnerLinkDefinition pl = new PartnerLinkDefinition();
    pl.setName("aPartner");
    pl.setPartnerLinkType(plt);
    pl.setMyRole(plt.getSecondRole());

    scope.addPartnerLink(pl);
    pi = new ProcessInstance(pd);
  }

  // ////////////////// activity read methods

  protected void tearDown() throws Exception {
    // close the jbpm context
    jbpmContext.close();
  }

  protected Activity readActivity(String xml, boolean isInitial) throws Exception {
    String textToParse = "<parent xmlns='" + BpelConstants.NS_BPEL + "'>" + xml + "</parent>";
    Element element = (Element) XmlUtil.parseText(textToParse).getFirstChild();
    return readActivity(element, isInitial);
  }

  protected Activity readActivity(Element element, boolean isInitial) {
    ((TestScope) scope).initial = isInitial;
    Activity activity = reader.readActivity(element, scope);
    return activity;
  }

  // ////////////////// load activity into the definition

  protected void plugInner(Activity activity) {
    firstActivity = new Receive("firstActivity");
    firstActivity.setAction(new ReceiveAction());

    Sequence sequence = new Sequence("rootSequence");
    sequence.addNode(firstActivity);
    sequence.addNode(activity);

    scope.setActivity(sequence);
  }

  protected void plugInitial(Activity activity) {
    scope.setActivity(activity);
  }

  // ////////////////// execution methods

  protected Token prepareInner() {
    ScopeInstance scopeInstance = scope.createInstance(pi.getRootToken());
    scopeInstance.initializeData();
    return scopeInstance.getPrimaryToken();
  }

  protected Token executeInner() {
    Token primaryToken = prepareInner();
    firstActivity.leave(new ExecutionContext(primaryToken));
    return primaryToken;
  }

  protected Token executeInitial(ReceiveAction messageTarget) {
    // trigger process instance
    Token receivingToken = messageTarget.initializeProcessInstance(pi);
    messageTarget.getInboundMessageActivity().messageReceived(messageTarget, receivingToken);
    // return the primary activity token
    return Scope.getInstance(pi.getRootToken()).getPrimaryToken();
  }

  // ////////////////// execution assertions

  protected void assertReceiveDisabled(Token token, Receive receive) {
    assertReceiverDisabled(token, receive.getReceiveAction());
  }

  protected void assertReceiveEnabled(Token token, Receive receive) {
    assertReceiverEnabled(token, receive.getReceiveAction());
  }

  protected void assertReceiverDisabled(Token token, ReceiveAction receiveAction) {
    assertFalse(isReceiving(receiveAction, token));
  }

  protected void assertReceiverEnabled(Token token, ReceiveAction receiveAction) {
    assertTrue(isReceiving(receiveAction, token));
  }

  protected void assertAlarmEnabled(Token token, AlarmAction alarmAction) {
    assertTrue(isWaiting(alarmAction, token));
  }

  protected void assertAlarmDisabled(Token token, AlarmAction alarmAction) {
    assertFalse(isWaiting(alarmAction, token));
  }

  protected void assertReceiveAndAdvance(Token token, Receive source, Activity target) {
    assertSame(source, token.getNode());
    source.messageReceived(source.getReceiveAction(), token);
    assertSame(target, token.getNode());
  }

  protected void assertReceiveAndComplete(Token token, Receive source) {
    assertSame(source, token.getNode());
    source.messageReceived(source.getReceiveAction(), token);
    assertCompleted(token);
  }

  protected void assertCompleted(Token token) {
    assertEquals(EndState.COMPLETED, Scope.getInstance(token).getState());
    assertTrue(token.hasEnded());
  }

  protected Token findToken(Token parent, Node node) {
    return (Token) parent.getChildrenAtNode(node).get(0);
  }

  static boolean isWaiting(AlarmAction alarmAction, Token token) {
    return MockIntegrationService.hasMark(alarmAction, token);
  }

  static boolean isReceiving(ReceiveAction receiveAction, Token token) {
    return MockIntegrationService.hasMark(receiveAction, token);
  }

  private static class TestAlarm extends AlarmAction {

    private static final long serialVersionUID = 1L;

    public void deleteTimer(Token token, SchedulerService scheduler) {
      MockIntegrationService.deleteMark(this, token);
    }

    public void createTimer(Token token, SchedulerService scheduler) {
      MockIntegrationService.createMark(this, token);
    }
  }

  static class TestScope extends Scope {

    boolean initial = false;

    private static final long serialVersionUID = 1L;

    public boolean isChildInitial(Activity child) {
      return initial;
    }
  }

  static class TestLink extends LinkDefinition {

    private static final long serialVersionUID = 1L;

    TestLink(String name) {
      super(name);
    }

    public void determineStatus(Token token) {
      getInstance(token).setStatus(Boolean.TRUE);
    }
  }

}
