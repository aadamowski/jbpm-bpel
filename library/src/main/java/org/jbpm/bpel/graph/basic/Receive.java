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
import org.jbpm.bpel.integration.def.InboundMessageActivity;
import org.jbpm.bpel.integration.def.ReceiveAction;
import org.jbpm.bpel.persistence.db.IntegrationSession;
import org.jbpm.graph.def.Action;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;

/**
 * Blocks for a matching message to arrive.
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/11/02 16:46:21 $
 */
public class Receive extends Activity implements InboundMessageActivity {

  private boolean createInstance;

  private static final long serialVersionUID = 1L;

  public Receive() {
  }

  public Receive(String name) {
    super(name);
  }

  public void terminate(ExecutionContext exeContext) {
    IntegrationService integrationService = ReceiveAction.getIntegrationService(exeContext.getJbpmContext());
    integrationService.cancelReception(getReceiveAction(), exeContext.getToken());
  }

  public void messageReceived(ReceiveAction messageTarget, Token token) {
    // execute the next activity
    leave(new ExecutionContext(token));
  }

  public ReceiveAction getReceiveAction() {
    if (action == null)
      return null;

    if (action instanceof ReceiveAction)
      return (ReceiveAction) action;

    // reacquire proxy of the proper type
    JbpmContext jbpmContext = JbpmContext.getCurrentJbpmContext();
    IntegrationSession integrationSession = IntegrationSession.getContextInstance(jbpmContext);
    ReceiveAction receiveAction = integrationSession.loadReceiveAction(action.getId());

    // update action reference
    action = receiveAction;

    return receiveAction;
  }

  public void setAction(Action action) {
    if (!(action instanceof ReceiveAction))
      throw new IllegalArgumentException("not a receive action: " + action);

    this.action = action;
    action.setName(name);
    ((ReceiveAction) action).setInboundMessageActivity(this);
  }

  public boolean isCreateInstance() {
    return createInstance;
  }

  public void setCreateInstance(boolean createInstance) {
    this.createInstance = createInstance;
  }

  public void accept(BpelVisitor visitor) {
    visitor.visit(this);
  }
}