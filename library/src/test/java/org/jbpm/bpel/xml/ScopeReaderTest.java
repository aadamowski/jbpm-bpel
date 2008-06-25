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
package org.jbpm.bpel.xml;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.jbpm.bpel.graph.basic.Receive;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.scope.Catch;
import org.jbpm.bpel.graph.scope.Handler;
import org.jbpm.bpel.graph.scope.OnAlarm;
import org.jbpm.bpel.graph.scope.OnEvent;
import org.jbpm.bpel.graph.scope.Scope;
import org.jbpm.bpel.integration.def.CorrelationSetDefinition;
import org.jbpm.bpel.integration.def.Correlations;
import org.jbpm.bpel.integration.def.PartnerLinkDefinition;
import org.jbpm.bpel.integration.def.ReceiveAction;
import org.jbpm.bpel.variable.def.ElementType;
import org.jbpm.bpel.variable.def.MessageType;
import org.jbpm.bpel.variable.def.VariableDefinition;
import org.jbpm.bpel.variable.def.VariableType;

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/03/16 00:04:38 $
 */
public class ScopeReaderTest extends AbstractReaderTestCase {

  protected void setUp() throws Exception {
    super.setUp();
    initMessageProperties();
  }

  public void testActivity() throws Exception {
    String xml = "<scope><empty/></scope>";
    Scope localScope = (Scope) readActivity(xml);
    Activity root = localScope.getActivity();

    assertEquals(localScope, root.getCompositeActivity());
  }

  public void testScope() throws Exception {
    String xml = "<scope><empty/></scope>";
    Scope localScope = (Scope) readActivity(xml);

    assertEquals(localScope, localScope.getActivity().getScope());
  }

  public void testIsolated() throws Exception {
    String xml = "<scope isolated='yes'><empty/></scope>";
    Scope localScope = (Scope) readActivity(xml);

    assertTrue(localScope.isIsolated());
  }

  public void testImplicit() throws Exception {
    String xml = "<scope isolated='yes'><empty/></scope>";
    Scope localScope = (Scope) readActivity(xml);

    assertFalse(localScope.isImplicit());
  }

  public void testVariables() throws Exception {
    String xml = "<scope>"
        + " <variables>"
        + "  <variable name='v' type='simple'/>"
        + " </variables>"
        + " <empty/>"
        + "</scope>";
    loadScope(xml);

    VariableDefinition variable = scope.getVariable("v");
    assertEquals(new QName("simple"), variable.getType().getName());
  }

  public void testPartnerLinks() throws Exception {
    String xml = "<scope xmlns:tns='http://manufacturing.org/wsdl/purchase'>"
        + " <partnerLinks>"
        + "   <partnerLink name='aPartner' partnerLinkType='tns:aPartnerLinkType'"
        + "    partnerRole='role1' myRole='role2'/>"
        + " </partnerLinks>"
        + " <empty/>"
        + "</scope>";
    loadScope(xml);

    PartnerLinkDefinition pl = scope.getPartnerLink("aPartner");
    assertEquals(new QName(NS_TNS, "aPartnerLinkType"), pl.getPartnerLinkType()
        .getQName());
  }

  public void testCorrelationSets() throws Exception {
    String xml = "<scope xmlns:def='http://manufacturing.org/wsdl/purchase'>"
        + " <correlationSets>"
        + " <correlationSet name='cs1' properties='def:p1 def:p2'/>"
        + " <correlationSet name='cs2' properties='def:p3'/>"
        + " </correlationSets>"
        + " <empty/>"
        + "</scope>";
    loadScope(xml);

    CorrelationSetDefinition cs = scope.getCorrelationSet("cs1");
    Set properties = cs.getProperties();
    assertEquals(2, properties.size());
    assertTrue(properties.contains(p1));
    assertTrue(properties.contains(p2));

    CorrelationSetDefinition cs2 = scope.getCorrelationSet("cs2");
    properties = cs2.getProperties();
    assertEquals(1, properties.size());
    assertTrue(properties.contains(p3));
  }

  public void testCatch() throws Exception {
    String xml = "<scope>"
        + " <correlationSets>"
        + "  <correlationSet name='cs' properties='tns:p1'"
        + "   xmlns:tns='http://manufacturing.org/wsdl/purchase'/>"
        + " </correlationSets>"
        + " <faultHandlers>"
        + "  <catch faultName='firstFault'>"
        + "   <receive partnerLink='aPartner' operation='o' variable='iv'>"
        + "    <correlations>"
        + "     <correlation set='cs' initiate='yes'/>"
        + "    </correlations>"
        + "   </receive>"
        + "  </catch>"
        + " </faultHandlers>"
        + " <empty/>"
        + "</scope>";
    loadScope(xml);

    List faultHandlers = scope.getFaultHandlers();
    assertEquals(1, faultHandlers.size());

    Catch faultHandler = (Catch) faultHandlers.get(0);
    verifyDefinitionsAvailability(faultHandler);
  }

  public void testCatch_faultName() throws Exception {
    String xml = "<scope>"
        + " <faultHandlers>"
        + "  <catch faultName='firstFault'>"
        + "   <empty name='firstFaultActivity'/>"
        + "  </catch>"
        + " </faultHandlers>"
        + " <empty/>"
        + "</scope>";
    loadScope(xml);

    List faultHandlers = scope.getFaultHandlers();
    assertEquals(1, faultHandlers.size());

    Catch firstCatch = (Catch) faultHandlers.get(0);
    assertEquals(new QName("firstFault"), firstCatch.getFaultName());
    assertNull(firstCatch.getFaultVariable());
  }

  public void testCatch_faultName_faultMessageType() throws Exception {
    String xml = "<scope>"
        + " <faultHandlers>"
        + "  <catch faultName='secondFault' faultVariable='fm' faultMessageType='def:aQName'"
        + "   xmlns:def='http://manufacturing.org/wsdl/purchase'>"
        + "   <empty/>"
        + "  </catch>"
        + " </faultHandlers>"
        + " <empty/>"
        + "</scope>";
    loadScope(xml);

    List faultHandlers = scope.getFaultHandlers();
    assertEquals(1, faultHandlers.size());

    Catch secondCatch = (Catch) faultHandlers.get(0);
    assertEquals(new QName("secondFault"), secondCatch.getFaultName());

    VariableDefinition faultVariable = secondCatch.getFaultVariable();
    assertEquals("fm", faultVariable.getName());

    VariableType type = faultVariable.getType();
    assertEquals(new QName(NS_TNS, "aQName"), type.getName());
    assertSame(MessageType.class, type.getClass());
  }

  public void testCatch_faultMessageType() throws Exception {
    String xml = "<scope>"
        + " <faultHandlers>"
        + "  <catch faultVariable='fm' faultMessageType='def:aQName'"
        + "   xmlns:def='http://manufacturing.org/wsdl/purchase'>"
        + "   <empty/>"
        + "  </catch>"
        + " </faultHandlers>"
        + " <empty/>"
        + "</scope>";
    loadScope(xml);

    List faultHandlers = scope.getFaultHandlers();
    assertEquals(1, faultHandlers.size());

    Catch thirdCatch = (Catch) faultHandlers.get(0);
    assertNull(thirdCatch.getFaultName());

    VariableDefinition faultVariable = thirdCatch.getFaultVariable();
    assertEquals("fm", faultVariable.getName());

    VariableType type = faultVariable.getType();
    assertEquals(new QName(NS_TNS, "aQName"), type.getName());
    assertSame(MessageType.class, type.getClass());
  }

  public void testCatch_faultName_faultElement() throws Exception {
    String xml = "<scope>"
        + " <faultHandlers>"
        + "  <catch faultName='fourthFault' faultVariable='fe' faultElement='def:elem'"
        + "   xmlns:def='http://manufacturing.org/wsdl/purchase'>"
        + "   <empty/>"
        + "  </catch>"
        + " </faultHandlers>"
        + " <empty/>"
        + "</scope>";
    loadScope(xml);

    List faultHandlers = scope.getFaultHandlers();
    assertEquals(1, faultHandlers.size());

    Catch fourthCatch = (Catch) faultHandlers.get(0);
    assertEquals(new QName("fourthFault"), fourthCatch.getFaultName());

    VariableDefinition faultVariable = fourthCatch.getFaultVariable();
    assertEquals("fe", faultVariable.getName());

    VariableType type = faultVariable.getType();
    assertEquals(new QName(NS_TNS, "elem"), type.getName());
    assertSame(ElementType.class, type.getClass());
  }

  public void testCatch_faultElement() throws Exception {
    String xml = "<scope>"
        + " <faultHandlers>"
        + "  <catch faultVariable='fe' faultElement='def:elem'"
        + "   xmlns:def='http://manufacturing.org/wsdl/purchase'>"
        + "   <empty/>"
        + "  </catch>"
        + " </faultHandlers>"
        + " <empty/>"
        + "</scope>";
    loadScope(xml);

    List faultHandlers = scope.getFaultHandlers();
    assertEquals(1, faultHandlers.size());

    Catch fifthCatch = (Catch) faultHandlers.get(0);
    assertNull(fifthCatch.getFaultName());

    VariableDefinition faultVariable = fifthCatch.getFaultVariable();
    assertEquals("fe", faultVariable.getName());

    VariableType type = faultVariable.getType();
    assertEquals(new QName(NS_TNS, "elem"), type.getName());
    assertSame(ElementType.class, type.getClass());
  }

  public void testCatchAll() throws Exception {
    String xml = "<scope>"
        + " <correlationSets>"
        + "  <correlationSet name='cs' properties='tns:p1' "
        + "   xmlns:tns='http://manufacturing.org/wsdl/purchase'/>"
        + " </correlationSets>"
        + " <faultHandlers>"
        + "  <catchAll>"
        + "   <receive partnerLink='aPartner' operation='o' variable='iv'>"
        + "    <correlations>"
        + "     <correlation set='cs' initiate='yes'/>"
        + "    </correlations>"
        + "   </receive>"
        + "  </catchAll>"
        + " </faultHandlers>"
        + " <empty/>"
        + "</scope>";
    loadScope(xml);

    Handler catchAll = scope.getCatchAll();
    verifyDefinitionsAvailability(catchAll);
  }

  public void testCompensationHandler() throws Exception {
    String xml = "<scope>"
        + " <correlationSets>"
        + "  <correlationSet name='cs' properties='tns:p1' "
        + "   xmlns:tns='http://manufacturing.org/wsdl/purchase'/>"
        + " </correlationSets>"
        + " <compensationHandler>"
        + "  <receive partnerLink='aPartner' operation='o' variable='iv'>"
        + "   <correlations>"
        + "    <correlation set='cs' initiate='yes'/>"
        + "   </correlations>"
        + "  </receive>"
        + " </compensationHandler>"
        + " <empty/>"
        + "</scope>";
    loadScope(xml);

    Handler compensation = scope.getCompensationHandler();
    verifyDefinitionsAvailability(compensation);
  }

  public void testTerminationHandler() throws Exception {
    String xml = "<scope>"
        + " <correlationSets>"
        + "  <correlationSet name='cs' properties='tns:p1' "
        + "   xmlns:tns='http://manufacturing.org/wsdl/purchase'/>"
        + " </correlationSets>"
        + " <terminationHandler>"
        + "  <receive partnerLink='aPartner' operation='o' variable='iv'>"
        + "   <correlations>"
        + "    <correlation set='cs' initiate='yes'/>"
        + "   </correlations>"
        + "  </receive>"
        + " </terminationHandler>"
        + " <empty/>"
        + "</scope>";
    loadScope(xml);

    Handler termination = scope.getTerminationHandler();
    verifyDefinitionsAvailability(termination);
  }

  public void testOnAlarm() throws Exception {
    String xml = "<scope>"
        + " <eventHandlers>"
        + "  <onAlarm><for>$f</for><empty name='oA1'/></onAlarm>"
        + " </eventHandlers>"
        + "<empty/>"
        + "</scope>";
    loadScope(xml);

    Collection onAlarms = scope.getOnAlarms();
    assertEquals(1, onAlarms.size());

    OnAlarm onAlarm = (OnAlarm) onAlarms.iterator().next();
    assertEquals("$f", onAlarm.getAlarmAction().getFor().getText());
    assertEquals("oA1", onAlarm.getActivity().getName());
  }

  public void testOnEvent() throws Exception {
    String xml = "<scope>"
        + " <correlationSets>"
        + "  <correlationSet name='cs' properties='tns:p1' "
        + "   xmlns:tns='http://manufacturing.org/wsdl/purchase'/>"
        + " </correlationSets>"
        + " <eventHandlers xmlns:tns='http://manufacturing.org/wsdl/purchase'>"
        + "  <onEvent partnerLink='aPartner' operation='o' variable='v' messageType='tns:aQName'>"
        + "   <correlations>"
        + "    <correlation set='cs' />"
        + "   </correlations>"
        + "   <empty name='oM1'/>"
        + "  </onEvent>"
        + " </eventHandlers>"
        + " <empty/>"
        + "</scope>";
    loadScope(xml);

    Collection receivers = scope.getOnEvents();
    assertEquals(1, receivers.size());

    OnEvent onEvent = (OnEvent) receivers.iterator().next();

    ReceiveAction receiveAction = onEvent.getReceiveAction();
    assertEquals("aPartner", receiveAction.getPartnerLink().getName());
    Correlations correlations = receiveAction.getCorrelations();
    assertEquals(1, correlations.getCorrelations().size());
    assertSame(scope.getCorrelationSet("cs"), correlations.getCorrelation("cs")
        .getSet());

    assertEquals("oM1", onEvent.getActivity().getName());

    final VariableDefinition variable = onEvent.getVariableDefinition();
    assertEquals("v", variable.getName());
    assertEquals(new QName(NS_TNS, "aQName"), variable.getType().getName());
  }

  private void loadScope(String xml) throws Exception {
    Element element = parseAsBpelElement(xml);
    reader.readScope(element, scope);
  }

  private void verifyDefinitionsAvailability(Handler handler) {
    Receive activity = (Receive) handler.getActivity();
    ReceiveAction receiveAction = activity.getReceiveAction();

    // partner link
    assertSame(scope.getPartnerLink("aPartner"), receiveAction.getPartnerLink());
    // variable
    assertSame(scope.getVariable("iv"), receiveAction.getVariable());
    // correlation set
    assertSame(scope.getCorrelationSet("cs"), receiveAction.getCorrelations()
        .getCorrelation("cs")
        .getSet());
  }
}