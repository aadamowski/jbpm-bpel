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

import org.jbpm.bpel.graph.exe.state.ActiveState;
import org.jbpm.bpel.graph.scope.Scope;
import org.jbpm.graph.exe.Token;

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2008/02/04 14:35:47 $
 */
class EventInstance extends ScopeInstance {

  private static final long serialVersionUID = 1L;

  EventInstance() {
  }

  EventInstance(Scope scope, Token token) {
    super(scope, token);
  }

  public void proceed() {
    // end event token (do not verify parent completion)
    getToken().end(false);

    // send completed signal to parent if it has no more pending events
    ScopeInstance parentInstance = getParent();
    if (ActiveState.EVENTS.equals(parentInstance.getState())
        && !parentInstance.hasPendingEvents()) {
      parentInstance.completed();
    }
  }
}