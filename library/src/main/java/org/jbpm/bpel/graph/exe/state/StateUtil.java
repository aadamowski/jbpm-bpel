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
package org.jbpm.bpel.graph.exe.state;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.JbpmContext;
import org.jbpm.bpel.graph.exe.ScopeInstance;
import org.jbpm.bpel.graph.scope.Handler;
import org.jbpm.bpel.persistence.db.ScopeSession;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2008/02/01 05:43:08 $
 */
class StateUtil {

  private static final Log log = LogFactory.getLog(StateUtil.class);

  // suppress default constructor, ensuring non-instantiability
  private StateUtil() {
  }

  public static void invokeDefaultCompensation(ScopeInstance targetInstance) {
    ScopeSession scopeSession = ScopeSession.getContextInstance(JbpmContext.getCurrentJbpmContext());
    if (scopeSession != null) {
      ScopeInstance nestedInstance = scopeSession.nextChildToCompensate(targetInstance);
      if (nestedInstance != null) {
        nestedInstance.compensate(targetInstance);
        return;
      }
    }
    targetInstance.getState().childrenCompensated(targetInstance);
  }

  public static void executeHandler(ScopeInstance scopeInstance, Handler handler) {
    Token token = scopeInstance.createHandlerToken();
    log.debug("executing '" + handler + "' for '" + token + "'");
    handler.execute(new ExecutionContext(token));
  }
}