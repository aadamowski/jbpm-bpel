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
package org.jbpm.bpel.persistence.db;

import java.util.Collection;

import org.hibernate.Session;

import org.jbpm.JbpmContext;
import org.jbpm.bpel.graph.exe.ScopeInstance;
import org.jbpm.bpel.graph.scope.Scope;
import org.jbpm.graph.exe.ProcessInstance;

/**
 * Scope compensation database operations.
 * @author Juan Cantu
 * @version $Revision$ $Date: 2008/06/12 08:18:53 $
 */
public class ScopeSession {

  private final Session session;

  public ScopeSession(Session session) {
    this.session = session;
  }

  public ScopeInstance nextChildToCompensate(ScopeInstance enclosingInstance) {
    Collection innerScopes = enclosingInstance.getDefinition().findNestedScopes();
    return innerScopes.isEmpty() ? null : findNextScopeToCompensate(enclosingInstance.getToken()
        .getProcessInstance(), innerScopes);
  }

  public ScopeInstance nextScopeToCompensate(ProcessInstance processInstance, Scope targetScope) {
    Collection nestedScopes = targetScope.findNestedScopes();
    nestedScopes.add(targetScope);
    return findNextScopeToCompensate(processInstance, nestedScopes);
  }

  protected ScopeInstance findNextScopeToCompensate(ProcessInstance processInstance,
      Collection nestedScopes) {
    return (ScopeInstance) session.getNamedQuery("ScopeSession.findNextScopeToCompensate")
        .setParameter("processInstance", processInstance)
        .setParameterList("nestedScopes", nestedScopes)
        .setMaxResults(1)
        .uniqueResult();
  }

  public static ScopeSession getContextInstance(JbpmContext jbpmContext) {
    Session session = jbpmContext.getSession();
    return session != null ? new ScopeSession(session) : null;
  }
}