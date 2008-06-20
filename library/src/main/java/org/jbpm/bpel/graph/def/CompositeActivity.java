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
package org.jbpm.bpel.graph.def;

import org.jbpm.bpel.graph.scope.Scope;
import org.jbpm.bpel.graph.scope.Handler;
import org.jbpm.bpel.graph.struct.StructuredActivity;
import org.jbpm.bpel.integration.def.CorrelationSetDefinition;
import org.jbpm.bpel.integration.def.PartnerLinkDefinition;
import org.jbpm.bpel.variable.def.VariableDefinition;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.NodeCollection;
import org.jbpm.graph.def.ProcessDefinition;

/**
 * Common base for process elements that enclose one or more activities. Such process elements
 * include:
 * <ul>
 * <li>{@linkplain StructuredActivity structured activities}</li>
 * <li>{@linkplain Scope scopes}</li>
 * <li>fault, compensation and termination {@linkplain Handler handlers}</li>
 * </ul>
 * @author Juan Cantú
 * @version $Revision$ $Date: 2008/02/01 05:43:08 $
 */
public abstract class CompositeActivity extends Activity implements NodeCollection {

  protected CompositeActivity() {
  }

  protected CompositeActivity(String name) {
    super(name);
  }

  protected boolean isChildInitial(Activity child) {
    return false;
  }

  // definition retrieval methods
  // //////////////////////////////////////////////////////////////////////

  public VariableDefinition findVariable(String name) {
    CompositeActivity parent = getCompositeActivity();
    return parent != null ? parent.findVariable(name) : null;
  }

  public CorrelationSetDefinition findCorrelationSet(String name) {
    CompositeActivity parent = getCompositeActivity();
    return parent != null ? parent.findCorrelationSet(name) : null;
  }

  public PartnerLinkDefinition findPartnerLink(String name) {
    CompositeActivity parent = getCompositeActivity();
    return parent != null ? parent.findPartnerLink(name) : null;
  }

  public LinkDefinition findLink(String name) {
    CompositeActivity parent = getCompositeActivity();
    return parent != null ? parent.findLink(name) : null;
  }

  public String generateNodeName() {
    return ProcessDefinition.generateNodeName(getNodes());
  }

  public Node findNode(String hierarchicalName) {
    return ProcessDefinition.findNode(this, hierarchicalName);
  }

  public boolean isScope() {
    return false;
  }

  public boolean hasActivity(Activity activity) {
    for (CompositeActivity composite = activity.getCompositeActivity(); composite != null; composite = composite.getCompositeActivity()) {
      if (equals(composite))
        return true;
    }
    return false;
  }

  protected void adoptActivity(Activity activity) {
    final CompositeActivity composite = activity.getCompositeActivity();
    assert composite == null || equals(composite) : composite;

    activity.setCompositeActivity(this);
  }

  protected void disadoptActivity(Activity activity) {
    final CompositeActivity composite = activity.getCompositeActivity();
    assert equals(composite) : composite;

    activity.setCompositeActivity(null);
  }
}