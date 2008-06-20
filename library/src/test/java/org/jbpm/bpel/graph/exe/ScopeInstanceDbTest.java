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

import javax.xml.namespace.QName;

import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.exe.state.EndState;
import org.jbpm.bpel.graph.scope.Compensate;
import org.jbpm.bpel.graph.scope.Scope;
import org.jbpm.bpel.persistence.db.AbstractDbTestCase;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

/**
 * @author Juan Cantu
 * @version $Revision$ $Date: 2008/02/01 05:43:07 $
 */
public class ScopeInstanceDbTest extends AbstractDbTestCase {

  public void testToken() {
    BpelProcessDefinition processDefinition = new BpelProcessDefinition("definition", BpelConstants.NS_EXAMPLES);
    graphSession.saveProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processDefinition.getGlobalScope().createInstance(
        processInstance.getRootToken());

    processInstance = saveAndReload(processInstance);
    ScopeInstance scopeInstance = Scope.getInstance(processInstance.getRootToken());
    assertEquals(processInstance.getRootToken(), scopeInstance.getToken());
  }

  public void testFaultInstance() {
    BpelProcessDefinition processDefinition = new BpelProcessDefinition("definition", BpelConstants.NS_EXAMPLES);
    graphSession.saveProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    QName faultName = new QName(BpelConstants.NS_EXAMPLES, "fault");
    processDefinition.getGlobalScope().createInstance(
        processInstance.getRootToken()).setFaultInstance(
        new FaultInstance(faultName));

    processInstance = saveAndReload(processInstance);
    ScopeInstance scopeInstance = Scope.getInstance(processInstance.getRootToken());
    assertEquals(faultName, scopeInstance.getFaultInstance().getName());
  }

  public void testCompensateCompensationListener() {
    BpelProcessDefinition processDefinition = new BpelProcessDefinition("definition", BpelConstants.NS_EXAMPLES);
    Scope globalScope = processDefinition.getGlobalScope();
    Compensate compensateActivity = new Compensate();
    globalScope.setActivity(compensateActivity);
    graphSession.saveProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    globalScope.createInstance(processInstance.getRootToken()).setCompensator(
        compensateActivity);

    processInstance = saveAndReload(processInstance);
    ScopeInstance scopeInstance = Scope.getInstance(processInstance.getRootToken());
    assertNotNull(scopeInstance.getCompensator());
  }

  public void testScopeInstanceCompensationListener() {
    BpelProcessDefinition processDefinition = new BpelProcessDefinition("definition", BpelConstants.NS_EXAMPLES);
    Scope globalScope = processDefinition.getGlobalScope();
    Scope localScope = new Scope();
    globalScope.setActivity(localScope);
    graphSession.saveProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    Token rootToken = processInstance.getRootToken();
    ScopeInstance compensator = globalScope.createInstance(rootToken);

    Token child = new Token(rootToken, "child");
    child.setNode(localScope);
    localScope.createInstance(child).setCompensator(compensator);

    processInstance = saveAndReload(processInstance);

    rootToken = processInstance.getRootToken();
    ScopeInstance globalScopeInstance = Scope.getInstance(rootToken);
    assertSame(globalScopeInstance, Scope.getInstance(
        rootToken.getChild("child")).getCompensator());
  }

  public void testScopeState() {
    BpelProcessDefinition processDefinition = new BpelProcessDefinition("definition", BpelConstants.NS_EXAMPLES);
    graphSession.saveProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processDefinition.getGlobalScope().createInstance(
        processInstance.getRootToken()).setState(EndState.COMPLETED);

    processInstance = saveAndReload(processInstance);

    ScopeInstance scopeInstance = Scope.getInstance(processInstance.getRootToken());
    assertEquals(EndState.COMPLETED, scopeInstance.getState());
  }

  public void testEventState() {
    BpelProcessDefinition processDefinition = new BpelProcessDefinition("definition", BpelConstants.NS_EXAMPLES);
    graphSession.saveProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processDefinition.getGlobalScope().createEventInstance(processInstance.getRootToken()).setState(EndState.TERMINATED);

    processInstance = saveAndReload(processInstance);

    ScopeInstance event = Scope.getInstance(processInstance.getRootToken());
    assertEquals(EndState.TERMINATED, event.getState());
  }
}
