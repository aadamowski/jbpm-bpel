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

import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import org.jbpm.bpel.alarm.AlarmAction;
import org.jbpm.bpel.graph.basic.Receive;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.def.Import;
import org.jbpm.bpel.graph.scope.Catch;
import org.jbpm.bpel.graph.scope.Scope;
import org.jbpm.bpel.integration.def.CorrelationSetDefinition;
import org.jbpm.bpel.integration.def.ReceiveAction;
import org.jbpm.bpel.variable.def.ElementType;
import org.jbpm.bpel.variable.def.MessageType;
import org.jbpm.bpel.variable.def.SchemaType;
import org.jbpm.bpel.variable.def.VariableDefinition;
import org.jbpm.bpel.variable.def.VariableType;
import org.jbpm.bpel.wsdl.PropertyAlias;
import org.jbpm.bpel.wsdl.impl.PropertyAliasImpl;

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/10/13 02:53:24 $
 */
public class BpelReaderTest extends AbstractReaderTestCase {

  protected void setUp() throws Exception {
    super.setUp();
    initMessageProperties();
  }

  public void testReadUrl() throws Exception {
    BpelProcessDefinition sampleProcess = new BpelProcessDefinition();
    // read bpel
    String resourceURI = getClass().getResource("processSample.bpel").toString();
    reader.read(sampleProcess, new InputSource(resourceURI));
    // assertions
    assertEquals(0, reader.getProblemHandler().getProblemCount());
    assertEquals(1, sampleProcess.getImportDefinition().getImports().size());
  }

  public void testReadUrl_1_1() throws Exception {
    BpelProcessDefinition sampleProcess = new BpelProcessDefinition();
    // add wsdl document
    Import _import = new Import();
    _import.setNamespace("http://manufacturing.org/wsdl/purchase");
    _import.setLocation(getClass().getResource("partnerLinkTypeSample-1_1.wsdl").toString());
    _import.setType(Import.Type.WSDL);
    reader.readImportWsdlDefinition(_import, new ProcessWsdlLocator(ProcessWsdlLocator.EMPTY_URI));
    sampleProcess.getImportDefinition().addImport(_import);
    // read bpel
    InputSource input = new InputSource(getClass().getResource("processSample-1_1.bpel").toString());
    reader.read(sampleProcess, input);
    // assertions
    assertEquals(0, reader.getProblemHandler().getProblemCount());
    assertEquals(1, sampleProcess.getImportDefinition().getImports().size());
  }

  public void testReadUrl_masterWsdl() throws Exception {
    BpelProcessDefinition sampleProcess = new BpelProcessDefinition();
    // read bpel
    String resourceURI = getClass().getResource("processSample-1_1.bpel").toString();
    reader.read(sampleProcess, new InputSource(resourceURI));
    // assertions
    assertEquals(0, reader.getProblemHandler().getProblemCount());
    assertEquals(1, sampleProcess.getImportDefinition().getImports().size());
  }

  public void testQueryLanguage() throws Exception {
    String xml = "<pd queryLanguage='ql'>"
        + "<receive partnerLink='aPartner' operation='o' variable='iv'/></pd>";
    readProcess(xml);
    assertEquals("ql", processDefinition.getQueryLanguage());
  }

  public void testExpressionLanguage() throws Exception {
    String xml = "<pd expressionLanguage='el'>"
        + "<receive partnerLink='aPartner' operation='o' variable='iv'/></pd>";
    readProcess(xml);
    assertEquals("el", processDefinition.getExpressionLanguage());
  }

  public void testSuppressJoinFailureYes() throws Exception {
    String xml = "<pd suppressJoinFailure='yes'>"
        + "<receive partnerLink='aPartner' operation='o' variable='iv'/></pd>";
    readProcess(xml);
    assertTrue(processDefinition.getGlobalScope().getSuppressJoinFailure().booleanValue());
  }

  public void testSuppressJoinFailureNo() throws Exception {
    String xml = "<pd suppressJoinFailure='no'>"
        + "<receive partnerLink='aPartner' operation='o' variable='iv'/></pd>";
    readProcess(xml);
    assertFalse(processDefinition.getGlobalScope().getSuppressJoinFailure().booleanValue());
  }

  public void testSuppressJoinFailureDefault() throws Exception {
    String xml = "<pd><receive partnerLink='aPartner' operation='o' variable='iv'/></pd>";
    readProcess(xml);
    assertFalse(processDefinition.getGlobalScope().getSuppressJoinFailure().booleanValue());
  }

  public void testScopeDefinition() throws Exception {
    String xml = "<pd><receive partnerLink='aPartner' operation='o' variable='iv'/></pd>";
    readProcess(xml);
    List processNodes = processDefinition.getNodes();
    assertEquals(1, processNodes.size());
    Scope scope = processDefinition.getGlobalScope();
    assertEquals(processNodes.get(0), scope);
    assertEquals(processDefinition, scope.getProcessDefinition());
    assertEquals(1, scope.getNodes().size());
  }

  public void testCatchMessageVariable() throws Exception {
    String xml = "<pd xmlns:tns='http://manufacturing.org/wsdl/purchase'>"
        + " <variables>"
        + "  <variable name='v' messageType='tns:aQName'/>"
        + " </variables>"
        + " <faultHandlers>"
        + "  <catch faultVariable='v' faultMessageType='tns:aQName'>"
        + "   <empty/>"
        + "  </catch>"
        + " </faultHandlers>"
        + " <empty/>"
        + "</pd>";
    readProcess(xml);
    Scope globalScope = processDefinition.getGlobalScope();
    // fault handler exists
    List faultHandlers = globalScope.getFaultHandlers();
    assertEquals(1, faultHandlers.size());
    // fault handler has local variable
    Catch catcher = (Catch) faultHandlers.get(0);
    assertNotSame(globalScope.getVariable("v"), catcher.getFaultVariable());
  }

  public void testCatchElementVariable() throws Exception {
    String xml = "<pd>"
        + " <faultHandlers xmlns:sns='http://manufacturing.org/xsd/purchase'>"
        + "  <catch faultVariable='e' faultElement='sns:purchase'>"
        + "   <empty/>"
        + "  </catch>"
        + " </faultHandlers>"
        + " <empty/>"
        + "</pd>";
    readProcess(xml);
    Scope globalScope = processDefinition.getGlobalScope();
    // fault handler exists
    List faultHandlers = globalScope.getFaultHandlers();
    assertEquals(1, faultHandlers.size());
    // fault handler has local variable
    Catch catcher = (Catch) faultHandlers.get(0);
    assertEquals("e", catcher.getFaultVariable().getName());
  }

  public void testCatch11MessageVariable() throws Exception {
    String xml = "<pd>"
        + " <variables xmlns:tns='http://manufacturing.org/wsdl/purchase'>"
        + "  <variable name='v' messageType='tns:aQName'/>"
        + " </variables>"
        + " <faultHandlers>"
        + "  <catch faultVariable='v'>"
        + "   <empty/>"
        + "  </catch>"
        + " </faultHandlers>"
        + " <empty/>"
        + "</pd>";
    readProcess(xml);
    Scope globalScope = processDefinition.getGlobalScope();
    // fault handler exists
    List faultHandlers = globalScope.getFaultHandlers();
    assertEquals(1, faultHandlers.size());
    // fault handler references variable from enclosing scope
    Catch catcher = (Catch) faultHandlers.get(0);
    assertSame(globalScope.getVariable("v"), catcher.getFaultVariable());
  }

  public void testCatch11NonMessageVariable() throws Exception {
    ProblemCollector collector = installCollector();
    String xml = "<pd>"
        + " <variables xmlns:xsd='http://www.w3.org/2001/XMLSchema'>"
        + "  <variable name='v' type='xsd:string'/>"
        + " </variables>"
        + " <faultHandlers>"
        + "  <catch faultVariable='v'>"
        + "   <empty/>"
        + "  </catch>"
        + " </faultHandlers>"
        + " <empty/>"
        + "</pd>";
    readProcess(xml);
    assertFalse("variable is not a message", collector.getProblems().isEmpty());
  }

  public void testCatch11VariableNotFound() throws Exception {
    ProblemCollector collector = installCollector();
    String xml = "<pd>"
        + " <faultHandlers>"
        + "  <catch faultVariable='v'>"
        + "   <empty/>"
        + "  </catch>"
        + " </faultHandlers>"
        + " <empty/>"
        + "</pd>";
    readProcess(xml);
    assertFalse("variable does not exist", collector.getProblems().isEmpty());
  }

  public void testForcedTerminationHandler() throws Exception {
    String xml = "<pd>"
        + " <faultHandlers>"
        + "  <catch faultName='bpel:forcedTermination' xmlns:bpel='"
        + BpelConstants.NS_BPEL
        + "'>"
        + "   <empty/>"
        + "  </catch>"
        + " </faultHandlers>"
        + " <empty/>"
        + "</pd>";
    readProcess(xml);
    assertSame(scope.getTerminationHandler(), scope.selectFaultHandler(
        BpelConstants.FAULT_FORCED_TERMINATION, null));
  }

  public void testActivity() throws Exception {
    String xml = "<pd>"
        + " <receive partnerLink='aPartner' operation='o' variable='iv'/>"
        + "</pd>";
    readProcess(xml);
    Activity root = processDefinition.getGlobalScope().getActivity();
    assertTrue(root instanceof Receive);
    assertSame(processDefinition, root.getProcessDefinition());
  }

  // Receiver
  // ///////////////////////////////////////////////////////////////////////////

  public void testReceiverPartnerLink() throws Exception {
    String xml = "<rcvr partnerLink='aPartner' operation='o' variable='iv'/>";
    ReceiveAction receiveAction = readReceiveAction(xml);
    assertEquals(partnerLink, receiveAction.getPartnerLink());
  }

  public void testReceiverPortType() throws Exception {
    ProblemCollector collector = installCollector();
    String xml = "<receive partnerLink='aPartner' portType='tns:mpt' operation='o' variable='iv'"
        + " xmlns:tns='http://manufacturing.org/wsdl/purchase'/>";
    readReceiveAction(xml);
    assertTrue(collector.getProblems().isEmpty());
  }

  public void testReceiverPortTypeDefault() throws Exception {
    ProblemCollector collector = installCollector();
    String xml = "<rcvr partnerLink='aPartner' operation='o' variable='iv'/>";
    readReceiveAction(xml);
    assertTrue(collector.getProblems().isEmpty());
  }

  public void testReceiverPortTypeNotFound() throws Exception {
    ProblemCollector collector = installCollector();
    String xml = "<rcvr partnerLink='aPartner' portType='invalidPT' operation='o' variable='iv'/>";
    readReceiveAction(xml);
    assertFalse("portType does not match myRole", collector.getProblems().isEmpty());
  }

  public void testReceiverOperation() throws Exception {
    String xml = "<rcvr partnerLink='aPartner' operation='o' variable='iv'/>";
    ReceiveAction receiveAction = readReceiveAction(xml);
    assertEquals("o", receiveAction.getOperation().getName());
  }

  public void testReceiverVariable() throws Exception {
    String xml = "<rcvr partnerLink='aPartner' operation='o' variable='iv'/>";
    ReceiveAction receiveAction = readReceiveAction(xml);
    assertSame(messageVariable, receiveAction.getVariable());
  }

  public void testReceiverCorrelations() throws Exception {
    // correlation set
    CorrelationSetDefinition corr = new CorrelationSetDefinition();
    corr.setName("corr");
    corr.addProperty(p1);
    scope.addCorrelationSet(corr);
    // alias
    MessageType messageType = (MessageType) messageVariable.getType();
    PropertyAlias alias = new PropertyAliasImpl();
    alias.setMessage(messageType.getMessage());
    alias.setProperty(p1);
    alias.setPart("p");
    messageType.addPropertyAlias(alias);

    String xml = "<rcvr partnerLink='aPartner' operation='o' variable='iv'>"
        + "   <correlations>"
        + "     <correlation set='corr'/> "
        + "   </correlations>"
        + "</rcvr>";
    ReceiveAction receiveAction = readReceiveAction(xml);

    assertNotNull(receiveAction.getCorrelations());
  }

  // Alarm
  // ///////////////////////////////////////////////////////////////////////////

  public void testAlarmFor() throws Exception {
    String xml = "<alrm>" + " <for>$f</for>" + "</alrm>";

    AlarmAction alarmAction = readAlarmAction(xml);
    assertEquals("$f", alarmAction.getFor().getText());
  }

  public void testAlarmUntil() throws Exception {
    String xml = "<alrm>" + " <until>$u</until>" + "</alrm>";

    AlarmAction alarmAction = readAlarmAction(xml);
    assertEquals("$u", alarmAction.getUntil().getText());
  }

  public void testAlarmRepeat() throws Exception {
    String xml = "<alrm><for>$f</for><repeatEvery>$r</repeatEvery></alrm>";

    AlarmAction alarmAction = readAlarmAction(xml);
    assertEquals("$f", alarmAction.getFor().getText());
    assertEquals("$r", alarmAction.getRepeatEvery().getText());
  }

  public void testVariableName() throws Exception {
    String xml = "<variables><variable name='v'  type='simple'/></variables>";

    Map variables = readVariables(xml);
    VariableDefinition variable = (VariableDefinition) variables.get("v");
    assertEquals("v", variable.getName());
  }

  public void testVariableNameNoType() throws Exception {
    String xml = "<variables>" + " <variable name='v'/>" + "</variables>";
    ProblemCollector collector = new ProblemCollector();
    reader.setProblemHandler(collector);
    readVariables(xml);
    assertEquals(1, collector.getProblems().size());
  }

  public void testVariableType() throws Exception {
    String xml = "<variables><variable name='v' type='simple'/></variables>";
    Map variables = readVariables(xml);
    VariableDefinition variable = (VariableDefinition) variables.get("v");
    assertEquals(new QName("simple"), variable.getType().getName());
  }

  public void testVariableMessageType() throws Exception {
    String xml = "<variables xmlns:tns='http://manufacturing.org/wsdl/purchase'>"
        + " <variable name='v' messageType='tns:aQName'/>"
        + "</variables>";
    Map variables = readVariables(xml);
    VariableDefinition variable = (VariableDefinition) variables.get("v");
    VariableType type = variable.getType();
    assertTrue(type instanceof MessageType);
    assertEquals(new QName("http://manufacturing.org/wsdl/purchase", "aQName"), type.getName());
  }

  public void testVariableSimpleType() throws Exception {
    String xml = "<variables><variable name='v' type='simple'/></variables>";
    Map variables = readVariables(xml);
    VariableDefinition variable = (VariableDefinition) variables.get("v");
    VariableType type = variable.getType();
    assertTrue(type instanceof SchemaType);
    assertEquals(new QName("simple"), variable.getType().getName());
  }

  public void testVariableElement() throws Exception {
    String xml = "<variables><variable name='v' element='element'/></variables>";
    Map variables = readVariables(xml);
    VariableDefinition variable = (VariableDefinition) variables.get("v");
    VariableType type = variable.getType();
    assertTrue(type instanceof ElementType);
    assertEquals(new QName("element"), variable.getType().getName());
  }

  private void readProcess(String xml) throws Exception {
    Element element = parseAsBpelElement(xml);
    reader.readProcessAttributes(element, processDefinition);
    reader.readScope(element, processDefinition.getGlobalScope());
  }

  private Map readVariables(String xml) throws Exception {
    Element element = parseAsBpelElement(xml);
    return reader.readVariables(element, scope);
  }

  private ReceiveAction readReceiveAction(String xml) throws Exception {
    Element element = parseAsBpelElement(xml);
    return reader.readReceiveAction(element, scope);
  }

  private AlarmAction readAlarmAction(String xml) throws Exception {
    Element element = parseAsBpelElement(xml);
    return reader.readAlarmAction(element, scope);
  }

  private ProblemCollector installCollector() {
    ProblemCollector pc = new ProblemCollector();
    reader.setProblemHandler(pc);
    return pc;
  }
}
