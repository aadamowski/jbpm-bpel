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
package org.jbpm.bpel.graph.basic;

import org.jbpm.bpel.graph.def.AbstractActivityDbTestCase;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.integration.def.InvokeAction;
import org.jbpm.graph.def.Action;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/08/08 11:20:32 $
 */
public class InvokeDbTest extends AbstractActivityDbTestCase {

  public void testInvokeAction() {
    InvokeAction invokeAction = new InvokeAction();

    Invoke invoke = createInvoke();
    invoke.setAction(invokeAction);
    String invokeName = invoke.getName();

    processDefinition.getGlobalScope().setActivity(invoke);

    processDefinition = saveAndReload(processDefinition);
    invoke = (Invoke) session.load(Invoke.class, new Long(processDefinition.getGlobalScope()
        .getActivity()
        .getId()));

    Action action = invoke.getAction();
    assertEquals(invokeName, action.getName());
    /*
     * when AbstractDbTestCase.newTransaction() simply ends the current transaction and begins a new
     * one, the session preserves the objects already loaded; thus, action is still an InvokeAction
     */
    // assertFalse(action instanceof InvokeAction);

    // verify proxy reacquisition
    invokeAction = invoke.getInvokeAction();
    assertEquals(invokeName, invokeAction.getName());

    action = invoke.getAction();
    assertTrue(action instanceof InvokeAction);
  }

  protected Activity createActivity() {
    return createInvoke();
  }

  private Invoke createInvoke() {
    return new Invoke("invoke");
  }
}
