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
package org.jbpm.bpel.graph.struct;

import java.util.List;

import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.BpelVisitor;
import org.jbpm.graph.def.Transition;

/**
 * Defines a collection of activities to be performed sequentially in lexical order.
 * @author Juan Cantú
 * @version $Revision$ $Date: 2008/02/01 05:43:08 $
 */
public class Sequence extends StructuredActivity {

  private static final long serialVersionUID = 1L;

  public Sequence() {
    super();
  }

  public Sequence(String name) {
    super(name);
  }

  protected boolean isChildInitial(Activity child) {
    return child.equals(getNodes().get(0));
  }

  // children management
  // /////////////////////////////////////////////////////////////////////////////

  protected void addImplicitTransitions(Activity activity) {
    List activities = getActivities();
    int activityCount = activities.size();
    End end = getEnd();

    if (activityCount > 0) {
      Activity lastActivity = (Activity) activities.get(activityCount - 1);
      lastActivity.disconnect(end);
      lastActivity.connect(activity);
    }
    else
      getBegin().connect(activity);

    activity.connect(end);
  }

  protected void removeImplicitTransitions(Activity activity) {
    Transition leaving = activity.getDefaultLeavingTransition();
    Transition arriving = activity.getDefaultArrivingTransition();

    activity.removeArrivingTransition(arriving);
    activity.removeLeavingTransition(leaving);

    Activity successor = (Activity) leaving.getTo();
    successor.removeArrivingTransition(leaving);
    successor.addArrivingTransition(arriving);
  }

  public void reorderNode(int oldIndex, int newIndex) {
    // remove activity from its old position and disconnect it from other nodes
    List activities = getActivities();
    Activity reorderedActivity = (Activity) activities.remove(oldIndex);
    removeImplicitTransitions(reorderedActivity);

    // connect activity to its new predecessor and successor
    Activity predecessor = (newIndex == 0 ? getBegin() : (Activity) activities.get(newIndex - 1));
    Activity successor = newIndex == activities.size() ? getEnd()
        : (Activity) activities.get(newIndex);

    Transition leaving = predecessor.getDefaultLeavingTransition();
    successor.removeArrivingTransition(leaving);
    reorderedActivity.addArrivingTransition(leaving);
    reorderedActivity.connect(successor);

    activities.add(newIndex, reorderedActivity);
  }

  public void accept(BpelVisitor visitor) {
    visitor.visit(this);
  }
}