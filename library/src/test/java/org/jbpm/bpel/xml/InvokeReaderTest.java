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

import java.util.Map;

import javax.xml.namespace.QName;

import org.xml.sax.SAXException;

import org.jbpm.bpel.graph.basic.Invoke;
import org.jbpm.bpel.graph.scope.Scope;
import org.jbpm.bpel.integration.def.CorrelationSetDefinition;
import org.jbpm.bpel.integration.def.Correlations;
import org.jbpm.bpel.integration.def.InvokeAction;
import org.jbpm.bpel.variable.def.VariableDefinition;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/10/13 02:53:24 $
 */
public class InvokeReaderTest extends AbstractReaderTestCase {

  VariableDefinition outputVariable;

  protected void setUp() throws Exception {
    super.setUp();
    initMessageProperties();
    // output variable
    outputVariable = new VariableDefinition();
    outputVariable.setName("ov");
    outputVariable.setType(messageVariable.getType());
    scope.addVariable(outputVariable);
  }

  // --- INVOKE ATTRIBUTES AND ELEMENTS ---
  public void testPartnerLink() throws SAXException {
    String xml = "<invoke partnerLink='aPartner' operation='o' inputVariable='iv'/>";
    Invoke invoke = (Invoke) readActivity(xml);

    InvokeAction invokeAction = (InvokeAction) invoke.getAction();
    assertEquals(partnerLink, invokeAction.getPartnerLink());
  }

  public void testPortType() throws SAXException {
    String xml = "<invoke partnerLink='aPartner' portType='def:ppt' operation='o' inputVariable='iv'"
        + " xmlns:def='"
        + NS_TNS
        + "'/>";
    Invoke invoke = (Invoke) readActivity(xml);

    InvokeAction invokeAction = (InvokeAction) invoke.getAction();
    assertEquals(new QName(NS_TNS, "ppt"), invokeAction.getPartnerLink()
        .getPartnerRole()
        .getPortType()
        .getQName());
  }

  public void testPortTypeDefault() throws SAXException {
    String xml = "<invoke partnerLink='aPartner' operation='o' inputVariable='iv'/>";
    Invoke invoke = (Invoke) readActivity(xml);

    InvokeAction invokeAction = (InvokeAction) invoke.getAction();
    assertEquals(new QName(NS_TNS, "ppt"), invokeAction.getPartnerLink()
        .getPartnerRole()
        .getPortType()
        .getQName());
  }

  public void testPortTypeNotFound() throws Exception {
    String xml = "<invoke partnerLink='aPartner' portType='invalidPT' operation='o' inputVariable='iv'/>";
    ProblemCollector collector = new ProblemCollector();
    reader.setProblemHandler(collector);
    readActivity(xml);

    assertFalse("invoke parse must fail when portType doesn't match partnerRole's portType",
        collector.getProblems().isEmpty());
  }

  public void testOperation() throws Exception {
    String xml = "<invoke partnerLink='aPartner' operation='o' inputVariable='iv'/>";
    Invoke invoke = (Invoke) readActivity(xml);

    InvokeAction invokeAction = (InvokeAction) invoke.getAction();
    assertEquals("o", invokeAction.getOperation().getName());
  }

  public void testInputVariableDefinition() throws Exception {
    String xml = "<invoke partnerLink='aPartner' operation='o' inputVariable='iv'/>";
    Invoke invoke = (Invoke) readActivity(xml);

    InvokeAction invokeAction = (InvokeAction) invoke.getAction();
    assertSame(messageVariable, invokeAction.getInputVariable());
  }

  public void testOutputVariableDefinition() throws Exception {
    String xml = "<invoke partnerLink='aPartner' operation='o2' inputVariable='iv' outputVariable='ov'/>";
    Invoke invoke = (Invoke) readActivity(xml);

    InvokeAction invokeAction = (InvokeAction) invoke.getAction();
    assertSame(outputVariable, invokeAction.getOutputVariable());
  }

  public void testOutputVariableDefinitionDefault() throws Exception {
    String xml = "<invoke partnerLink='aPartner' operation='o' inputVariable='iv'/>";
    Invoke invoke = (Invoke) readActivity(xml);

    InvokeAction invokeAction = (InvokeAction) invoke.getAction();
    assertNull(invokeAction.getOutputVariable());
  }

  public void testCorrelations() throws Exception {
    CorrelationSetDefinition set = new CorrelationSetDefinition();
    set.setName("res");
    set.addProperty(p1);
    scope.addCorrelationSet(set);

    set = new CorrelationSetDefinition();
    set.setName("req");
    set.addProperty(p1);
    scope.addCorrelationSet(set);

    set = new CorrelationSetDefinition();
    set.setName("reqres");
    set.addProperty(p1);
    scope.addCorrelationSet(set);

    String xml = "<invoke partnerLink='aPartner' operation='o2' inputVariable='iv' outputVariable='ov'>"
        + " <correlations>"
        + "   <correlation set='res' initiate='yes' pattern='response'/> "
        + "   <correlation set='req' initiate='no' pattern='request'/> "
        + "   <correlation set='reqres' pattern='request-response'/> "
        + " </correlations>"
        + "</invoke>";
    Invoke invoke = (Invoke) readActivity(xml);

    InvokeAction invokeAction = (InvokeAction) invoke.getAction();
    Correlations correlations = invokeAction.getRequestCorrelations();
    Map correlationMap = correlations.getCorrelations();

    assertNotNull(correlationMap.get("res"));
    assertNotNull(correlationMap.get("reqres"));

    correlations = invokeAction.getResponseCorrelations();
    correlationMap = correlations.getCorrelations();

    assertNotNull(correlationMap.get("req"));
    assertNotNull(correlationMap.get("reqres"));
  }

  public void testScopedInvoke() throws Exception {
    String xml = "<invoke partnerLink='aPartner' operation='o' inputVariable='iv'>"
        + "<compensationHandler><empty/></compensationHandler>"
        + "<faultHandlers><catchAll><empty/></catchAll></faultHandlers>"
        + "</invoke>";
    Scope scope = (Scope) readActivity(xml);

    assertTrue(scope.isImplicit());
    assertNotNull(scope.getCompensationHandler());
    assertNotNull(scope.getCatchAll());

    InvokeAction invokeAction = (InvokeAction) scope.getActivity().getAction();
    assertEquals(partnerLink, invokeAction.getPartnerLink());
  }
}
