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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.JbpmContext;
import org.jbpm.bpel.graph.exe.ScopeInstance;
import org.jbpm.bpel.graph.exe.state.ActiveState;
import org.jbpm.bpel.integration.def.InboundMessageActivity;
import org.jbpm.bpel.integration.def.ReceiveAction;
import org.jbpm.bpel.persistence.db.IntegrationSession;
import org.jbpm.bpel.variable.def.VariableDefinition;
import org.jbpm.graph.def.Action;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;

/**
 * @author Juan Cantu
 * @version $Revision$ $Date: 2008/02/01 05:43:07 $
 */
public class OnEvent extends Handler implements InboundMessageActivity {

  private VariableDefinition variable;

  private static final Log log = LogFactory.getLog(OnEvent.class);
  private static final long serialVersionUID = 1L;

  public void messageReceived(ReceiveAction messageTarget, Token token) {
    ScopeInstance scopeInstance = Scope.getInstance(token);
    if (!scopeInstance.getState().equals(ActiveState.PRIMARY)) {
      log.debug("message refused, scope no longer in '"
          + ActiveState.PRIMARY
          + "' state: "
          + "token="
          + token
          + ", scope="
          + getCompositeActivity());
      return;
    }

    // create event token and scope instance
    Token eventToken = scopeInstance.createEventToken();
    scopeInstance.getDefinition().createEventInstance(eventToken);

    // if this handler has a local variable definition, create the instance
    if (variable != null)
      variable.createInstance(eventToken);

    // execute associated activity
    execute(new ExecutionContext(eventToken));
  }

  // CompositeActivity override
  // //////////////////////////////////////////////////////////////

  public VariableDefinition findVariable(String name) {
    return variable != null && variable.getName().equals(name) ? variable
        : super.findVariable(name);
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
    ((ReceiveAction) action).setInboundMessageActivity(this);
  }

  public VariableDefinition getVariableDefinition() {
    return variable;
  }

  public void setVariableDefinition(VariableDefinition variable) {
    this.variable = variable;
  }
}
