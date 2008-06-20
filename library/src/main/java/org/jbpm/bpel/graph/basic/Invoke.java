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

import org.jbpm.JbpmContext;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.BpelVisitor;
import org.jbpm.bpel.integration.IntegrationService;
import org.jbpm.bpel.integration.def.InvokeAction;
import org.jbpm.bpel.integration.def.ReceiveAction;
import org.jbpm.bpel.persistence.db.IntegrationSession;
import org.jbpm.graph.def.Action;
import org.jbpm.graph.exe.ExecutionContext;

/**
 * Calls a one-way or request-response operation on an endpoint offered by a partner.
 * @author Juan Cantú
 * @version $Revision$ $Date: 2008/01/30 07:18:22 $
 */
public class Invoke extends Activity {

  private static final long serialVersionUID = 1L;

  public Invoke() {
  }

  public Invoke(String name) {
    super(name);
  }

  public InvokeAction getInvokeAction() {
    if (action == null)
      return null;

    if (action instanceof InvokeAction)
      return (InvokeAction) action;

    // acquire proxy of the proper type
    JbpmContext jbpmContext = JbpmContext.getCurrentJbpmContext();
    IntegrationSession integrationSession = IntegrationSession.getContextInstance(jbpmContext);
    InvokeAction invokeAction = integrationSession.loadInvokeAction(action.getId());

    // update action reference
    action = invokeAction;
    return invokeAction;
  }

  public void setAction(Action action) {
    if (!(action instanceof InvokeAction))
      throw new IllegalArgumentException("not an invoke action: " + action);

    this.action = action;
    action.setName(name);
  }

  public void terminate(ExecutionContext exeContext) {
    IntegrationService integrationService = ReceiveAction.getIntegrationService(exeContext.getJbpmContext());
    integrationService.cancelInvocation(getInvokeAction(), exeContext.getToken());
  }

  public void accept(BpelVisitor visitor) {
    visitor.visit(this);
  }
}
