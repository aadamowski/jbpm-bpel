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
package org.jbpm.bpel.graph.scope;

import org.jbpm.bpel.graph.basic.Empty;
import org.jbpm.bpel.graph.def.AbstractActivityDbTestCase;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.integration.def.CorrelationSetDefinition;
import org.jbpm.bpel.integration.def.PartnerLinkDefinition;
import org.jbpm.bpel.variable.def.VariableDefinition;
import org.jbpm.graph.def.Action;
import org.jbpm.graph.def.ExceptionHandler;
import org.jbpm.graph.def.ProcessDefinition;

/**
 * @author Juan Cantu
 * @version $Revision$ $Date: 2007/08/08 11:20:33 $
 */
public class ScopeDbTest extends AbstractActivityDbTestCase {

  public void testActivity() {
    Scope scope = createScope();
    scope.setActivity(new Empty("e"));

    putScope(processDefinition, scope);

    processDefinition = saveAndReload(processDefinition);
    scope = getScope(processDefinition);

    assertEquals("e", scope.getActivity().getName());
  }

  public void testBpelProcessDefinition_global() {
    Scope globalScope = processDefinition.getGlobalScope();

    session.save(processDefinition);
    newTransaction();

    globalScope = (Scope) session.load(Scope.class, new Long(
        globalScope.getId()));

    ProcessDefinition pd = globalScope.getProcessDefinition();
    assertEquals("p", pd.getName());
    /*
     * when AbstractDbTestCase.newTransaction() simply ends the current transaction and begins a new
     * one, the session preserves the objects already loaded; thus, pd is still a BpelProcessDefinition
     */
    // assertFalse(pd instanceof BpelProcessDefinition);

    processDefinition = globalScope.getBpelProcessDefinition();
    assertEquals("p", processDefinition.getName());

    // verify proxy reacquisition
    pd = globalScope.getProcessDefinition();
    assertTrue(pd instanceof BpelProcessDefinition);
  }

  public void testFaultExceptionHandler() {
    Scope scope = createScope();
    scope.installFaultExceptionHandler();

    putScope(processDefinition, scope);

    processDefinition = saveAndReload(processDefinition);
    scope = getScope(processDefinition);
    ExceptionHandler handler = (ExceptionHandler) scope.getExceptionHandlers()
        .get(0);
    Action action = (Action) handler.getActions().get(0);

    assertEquals(FaultActionHandler.class.getName(),
        action.getActionDelegation().getClassName());
  }

  public void testIsolated() {
    Scope scope = createScope();
    scope.setIsolated(true);

    putScope(processDefinition, scope);

    processDefinition = saveAndReload(processDefinition);
    scope = getScope(processDefinition);

    assertTrue(scope.isIsolated());
  }

  public void testFaultHandlers() {
    Catch catcher = new Catch();
    catcher.setActivity(new Empty("onCatch"));

    Scope scope = createScope();
    scope.addCatch(catcher);

    putScope(processDefinition, scope);

    processDefinition = saveAndReload(processDefinition);
    scope = getScope(processDefinition);
    catcher = (Catch) scope.getFaultHandlers().get(0);

    assertEquals(scope, catcher.getCompositeActivity());
    assertEquals("onCatch", catcher.getActivity().getName());
  }

  public void testTerminationHandler() {
    Handler handler = new Handler();
    handler.setActivity(new Empty("onTermination"));

    Scope scope = createScope();
    scope.setTerminationHandler(handler);

    putScope(processDefinition, scope);

    processDefinition = saveAndReload(processDefinition);
    scope = getScope(processDefinition);
    handler = scope.getTerminationHandler();

    assertEquals(scope, handler.getCompositeActivity());
    assertEquals("onTermination", handler.getActivity().getName());
  }

  public void testCompensationHandler() {
    Handler handler = new Handler();
    handler.setActivity(new Empty("onCompensation"));

    Scope scope = createScope();
    scope.setCompensationHandler(handler);

    putScope(processDefinition, scope);

    processDefinition = saveAndReload(processDefinition);
    scope = getScope(processDefinition);
    handler = scope.getCompensationHandler();

    assertEquals(scope, handler.getCompositeActivity());
    assertEquals("onCompensation", handler.getActivity().getName());
  }

  public void testCatchAllHandler() {
    Handler handler = new Handler();
    handler.setActivity(new Empty("onCatchAll"));

    Scope scope = createScope();
    scope.setCatchAll(handler);

    putScope(processDefinition, scope);

    processDefinition = saveAndReload(processDefinition);
    scope = getScope(processDefinition);
    handler = scope.getCatchAll();

    assertEquals(scope, handler.getCompositeActivity());
    assertEquals("onCatchAll", handler.getActivity().getName());
  }

  public void testOnEvent() {
    OnEvent onEvent = new OnEvent();
    onEvent.setActivity(new Empty("onEvent"));

    Scope scope = createScope();
    scope.addOnEvent(onEvent);

    putScope(processDefinition, scope);

    processDefinition = saveAndReload(processDefinition);
    scope = getScope(processDefinition);
    onEvent = (OnEvent) scope.getOnEvents().iterator().next();

    assertEquals(scope, onEvent.getCompositeActivity());
    assertEquals("onEvent", onEvent.getActivity().getName());
  }

  public void testOnAlarm() {
    OnAlarm onAlarm = new OnAlarm();
    onAlarm.setActivity(new Empty("onAlarm"));

    Scope scope = createScope();
    scope.addOnAlarm(onAlarm);

    putScope(processDefinition, scope);

    processDefinition = saveAndReload(processDefinition);
    scope = getScope(processDefinition);
    onAlarm = (OnAlarm) scope.getOnAlarms().iterator().next();

    assertEquals(scope, onAlarm.getCompositeActivity());
    assertEquals("onAlarm", onAlarm.getActivity().getName());
  }

  public void testVariable() {
    VariableDefinition variable = new VariableDefinition();
    variable.setName("aVar");

    Scope scope = createScope();
    scope.addVariable(variable);

    putScope(processDefinition, scope);

    processDefinition = saveAndReload(processDefinition);
    scope = getScope(processDefinition);

    assertNotNull(scope.getVariable("aVar"));
  }

  public void testPartnerLink() {
    PartnerLinkDefinition plink = new PartnerLinkDefinition();
    plink.setName("pl");

    Scope scope = createScope();
    scope.addPartnerLink(plink);

    putScope(processDefinition, scope);

    processDefinition = saveAndReload(processDefinition);
    scope = getScope(processDefinition);

    assertNotNull(scope.getPartnerLink("pl"));
  }

  public void testCorrelationSet() {
    CorrelationSetDefinition cs = new CorrelationSetDefinition();
    cs.setName("cs");

    Scope scope = createScope();
    scope.addCorrelationSet(cs);

    putScope(processDefinition, scope);

    processDefinition = saveAndReload(processDefinition);
    scope = getScope(processDefinition);

    assertNotNull(scope.getCorrelationSet("cs"));
  }

  protected Activity createActivity() {
    return createScope();
  }

  private Scope createScope() {
    return new Scope("scope");
  }

  private void putScope(BpelProcessDefinition processDefinition, Scope scope) {
    processDefinition.getGlobalScope().setActivity(scope);
  }

  private Scope getScope(BpelProcessDefinition processDefinition) {
    return (Scope) session.load(Scope.class, new Long(
        processDefinition.getGlobalScope().getActivity().getId()));
  }
}
