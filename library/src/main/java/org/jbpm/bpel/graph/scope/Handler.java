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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.CompositeActivity;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.exe.ExecutionContext;

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2008/02/01 05:43:08 $
 */
public class Handler extends CompositeActivity {

  private Activity activity;

  private static final long serialVersionUID = 1L;

  public Activity getActivity() {
    return activity;
  }

  public void setActivity(Activity activity) {
    if (this.activity != null)
      unsetActivity();

    if (activity != null) {
      activity.detachFromParent();
      adoptActivity(activity);

      this.activity = activity;
    }
  }

  private void unsetActivity() {
    disadoptActivity(activity);
    activity = null;
  }

  public void execute(ExecutionContext exeContext) {
    activity.enter(exeContext);
  }

  // children management
  // /////////////////////////////////////////////////////////////////////////////

  public Node addNode(Node node) {
    if (!(node instanceof Activity))
      throw new IllegalArgumentException("not an activity: " + node);

    setActivity((Activity) node);
    return node;
  }

  public Node removeNode(Node node) {
    if (node == null)
      throw new IllegalArgumentException("node is null");

    if (!node.equals(activity))
      return null;

    unsetActivity();
    return node;
  }

  public void reorderNode(int oldIndex, int newIndex) {
    if (activity == null || oldIndex != 0 || newIndex != 0) {
      throw new IndexOutOfBoundsException("could not reorder element: oldIndex="
          + oldIndex
          + ", newIndex="
          + newIndex);
    }
  }

  public List getNodes() {
    return activity != null ? Collections.singletonList(activity) : null;
  }

  public Node getNode(String name) {
    return hasNode(name) ? activity : null;
  }

  public Map getNodesMap() {
    return activity != null ? Collections.singletonMap(activity.getName(), activity) : null;
  }

  public boolean hasNode(String name) {
    return activity != null && activity.getName().equals(name);
  }
}