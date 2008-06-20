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
package org.jbpm.bpel.deploy;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jbpm.bpel.BpelException;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.def.CompositeActivity;
import org.jbpm.bpel.graph.scope.Scope;

/**
 * @author Juan Cantu
 * @version $Revision$ $Date: 2007/10/13 02:53:24 $
 */
public class ScopeMatcher implements DeploymentVisitor {

  private final Scope parent;
  private final Map scopeDescriptors;

  public ScopeMatcher(BpelProcessDefinition processDefinition) {
    parent = processDefinition.getGlobalScope();
    scopeDescriptors = new HashMap();
  }

  private ScopeMatcher(Scope parent, Map scopeDescriptors) {
    this.parent = parent;
    this.scopeDescriptors = scopeDescriptors;
  }

  public Map getScopeDescriptors() {
    return scopeDescriptors;
  }

  public void visit(DeploymentDescriptor deploymentDescriptor) {
    scopeDescriptors.put(parent, deploymentDescriptor);
    propagate(deploymentDescriptor);
  }

  public void visit(ScopeDescriptor scopeDescriptor) {
    Scope scope = findScope(scopeDescriptor.getName(), parent);
    // TODO if not found throw error?
    if (scope != null) {
      scopeDescriptors.put(scope, scopeDescriptor);
      new ScopeMatcher(scope, scopeDescriptors).propagate(scopeDescriptor);
    }
  }

  public void visit(PartnerLinkDescriptor partnerLinkDescriptor) {
  }

  private void propagate(ScopeDescriptor parentScopeDescriptor) {
    Iterator appScopeIt = parentScopeDescriptor.getScopes().iterator();

    while (appScopeIt.hasNext()) {
      ScopeDescriptor scopeDescriptor = (ScopeDescriptor) appScopeIt.next();
      scopeDescriptor.accept(this);
    }
  }

  private Scope findScope(String appScopeName, CompositeActivity parent) {
    Scope matchingScope = null;

    Iterator activityIt = parent.getNodes().iterator();
    while (activityIt.hasNext()) {
      Activity activity = (Activity) activityIt.next();

      if (!(activity instanceof CompositeActivity))
        continue;

      if (activity instanceof Scope) {
        if (appScopeName != null ? appScopeName.equals(activity.getName())
            : activity.isUnnamed()) {
          if (matchingScope != null) {
            throw new BpelException("conflicting scope name: " + appScopeName);
          }
          matchingScope = (Scope) activity;
        }
      }

      Scope scope = findScope(appScopeName, (CompositeActivity) activity);
      if (scope != null) {
        if (matchingScope != null) {
          throw new BpelException("conflicting scope name: " + appScopeName);
        }
        matchingScope = scope;
      }
    }

    return matchingScope;
  }
}
