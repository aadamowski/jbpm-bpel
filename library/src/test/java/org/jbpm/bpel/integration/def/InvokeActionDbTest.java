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
package org.jbpm.bpel.integration.def;

import javax.wsdl.Definition;
import javax.wsdl.Operation;
import javax.wsdl.PortType;
import javax.wsdl.WSDLException;

import org.jbpm.bpel.graph.basic.Invoke;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.persistence.db.AbstractDbTestCase;
import org.jbpm.bpel.variable.def.VariableDefinition;
import org.jbpm.bpel.wsdl.xml.WsdlUtil;
import org.jbpm.bpel.xml.BpelConstants;

/**
 * @author Juan Cantu
 * @version $Revision$ $Date: 2007/10/13 02:53:23 $
 */
public class InvokeActionDbTest extends AbstractDbTestCase {

  BpelProcessDefinition processDefinition;
  InvokeAction invokeAction;

  protected void setUp() throws Exception {
    super.setUp();

    invokeAction = new InvokeAction();

    Invoke invoke = new Invoke("ivk");
    invoke.setAction(invokeAction);

    processDefinition = new BpelProcessDefinition("pd", BpelConstants.NS_EXAMPLES);
    processDefinition.getGlobalScope().setActivity(invoke);
  }

  public void testPartnerLink() {
    PartnerLinkDefinition partnerLink = new PartnerLinkDefinition();
    partnerLink.setName("pl");

    invokeAction.setPartnerLink(partnerLink);
    processDefinition.getGlobalScope().addPartnerLink(partnerLink);

    processDefinition = saveAndReload(processDefinition);
    invokeAction = getInvokeAction(processDefinition);

    assertEquals("pl", invokeAction.getPartnerLink().getName());
  }

  public void testOperation() throws WSDLException {
    Definition def = WsdlUtil.getSharedDefinition();

    Operation operation = def.createOperation();
    operation.setName("AM451");

    invokeAction.setOperation(operation);

    PortType portType = def.createPortType();
    portType.addOperation(operation);
    
    processDefinition.getImportDefinition().addPortType(portType);

    processDefinition = saveAndReload(processDefinition);
    invokeAction = getInvokeAction(processDefinition);

    assertEquals("AM451", invokeAction.getOperation().getName());
  }

  public void testInputVariable() {
    VariableDefinition variable = new VariableDefinition();
    variable.setName("v");

    invokeAction.setInputVariable(variable);
    processDefinition.getGlobalScope().addVariable(variable);

    processDefinition = saveAndReload(processDefinition);
    invokeAction = getInvokeAction(processDefinition);

    assertEquals("v", invokeAction.getInputVariable().getName());
  }

  public void testOutputVariable() {
    VariableDefinition variable = new VariableDefinition();
    variable.setName("v");

    invokeAction.setOutputVariable(variable);
    processDefinition.getGlobalScope().addVariable(variable);

    processDefinition = saveAndReload(processDefinition);
    invokeAction = getInvokeAction(processDefinition);

    assertEquals("v", invokeAction.getOutputVariable().getName());
  }

  public void testOutCorrelations() {
    CorrelationSetDefinition correlationSet = new CorrelationSetDefinition();
    correlationSet.setName("cs");

    processDefinition.getGlobalScope().addCorrelationSet(correlationSet);

    Correlation correlation = new Correlation();
    correlation.setSet(correlationSet);

    Correlations correlations = new Correlations();
    correlations.addCorrelation(correlation);

    invokeAction.setResponseCorrelations(correlations);

    processDefinition = saveAndReload(processDefinition);
    invokeAction = getInvokeAction(processDefinition);

    correlations = invokeAction.getResponseCorrelations();
    correlation = correlations.getCorrelation("cs");

    assertEquals(1, correlations.getCorrelations().size());
    assertEquals("cs", correlation.getSet().getName());
  }

  public void testInCorrelations() {
    CorrelationSetDefinition correlationSet = new CorrelationSetDefinition();
    correlationSet.setName("cs");

    processDefinition.getGlobalScope().addCorrelationSet(correlationSet);

    Correlation correlation = new Correlation();
    correlation.setSet(correlationSet);

    Correlations correlations = new Correlations();
    correlations.addCorrelation(correlation);

    invokeAction.setRequestCorrelations(correlations);

    processDefinition = saveAndReload(processDefinition);
    invokeAction = getInvokeAction(processDefinition);
    correlations = invokeAction.getRequestCorrelations();
    correlation = correlations.getCorrelation("cs");

    assertEquals(1, correlations.getCorrelations().size());
    assertEquals("cs", correlation.getSet().getName());
  }

  private InvokeAction getInvokeAction(BpelProcessDefinition processDefinition) {
    Activity invoke = processDefinition.getGlobalScope().getActivity();
    // reacquire proxy of the proper type
    return (InvokeAction) session.load(InvokeAction.class, new Long(
        invoke.getAction().getId()));
  }
}
