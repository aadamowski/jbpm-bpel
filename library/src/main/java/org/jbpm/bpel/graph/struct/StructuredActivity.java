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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.CompositeActivity;
import org.jbpm.bpel.graph.def.LinkDefinition;
import org.jbpm.bpel.sublang.def.JoinCondition;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.Transition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;

/**
 * Common base of structured activities.
 * @author Juan Cantú
 * @version $Revision$ $Date: 2008/02/01 05:43:08 $
 */
public abstract class StructuredActivity extends CompositeActivity {

  private List activities = new ArrayList();
  private Begin begin = new Begin(this);
  private End end = new End(this);

  private transient Map nodesMap;

  protected StructuredActivity() {
  }

  protected StructuredActivity(String name) {
    super(name);
    begin.setName(name);
    end.setName(name);
  }

  // behaviour methods
  // /////////////////////////////////////////////////////////////////////////////

  public void enter(ExecutionContext exeContext) {
    begin.enter(exeContext);
  }

  public void execute(ExecutionContext exeContext) {
    begin.leave(exeContext, begin.getDefaultLeavingTransition());
  }

  public void leave(ExecutionContext exeContext) {
    end.leave(exeContext);
  }

  public void eliminatePath(Token token) {
    for (int i = 0, n = activities.size(); i < n; i++) {
      Activity activity = (Activity) activities.get(i);
      activity.eliminatePath(token);
    }
    end.eliminatePath(token);
  }

  // structure delimiters
  // /////////////////////////////////////////////////////////////////////////////

  public Begin getBegin() {
    return begin;
  }

  public End getEnd() {
    return end;
  }

  // children management
  // /////////////////////////////////////////////////////////////////////////////

  protected List getActivities() {
    return activities;
  }

  public Node addNode(Node node) {
    if (!(node instanceof Activity))
      throw new IllegalArgumentException("node is not activity: " + node);

    addActivity((Activity) node);
    return node;
  }

  protected void addActivity(Activity activity) {
    // activity -> delimiters
    addImplicitTransitions(activity);

    // activity -> parent
    activity.detachFromParent();
    adoptActivity(activity);

    // parent -> activity
    activities.add(activity);
    if (nodesMap != null)
      nodesMap.put(activity.getName(), activity);
  }

  public Node removeNode(Node node) {
    if (!activities.contains(node))
      return null;

    removeActivity((Activity) node);
    return node;
  }

  protected void removeActivity(Activity activity) {
    // activity -> delimiters
    removeImplicitTransitions(activity);

    // activity -> parent
    disadoptActivity(activity);

    // parent -> activity
    activities.remove(activity);
    if (nodesMap != null)
      nodesMap.remove(activity.getName());
  }

  public void reorderNode(int oldIndex, int newIndex) {
    activities.add(newIndex, activities.remove(oldIndex));
  }

  public List getNodes() {
    return activities;
  }

  public Map getNodesMap() {
    if (nodesMap == null) {
      nodesMap = new HashMap();
      for (int i = 0, n = activities.size(); i < n; i++) {
        Node node = (Node) activities.get(i);
        nodesMap.put(node.getName(), node);
      }
    }
    return nodesMap;
  }

  public Node getNode(String name) {
    Map nodesMap = getNodesMap();
    return nodesMap != null ? (Node) nodesMap.get(name) : null;
  }

  public boolean hasNode(String name) {
    Map nodesMap = getNodesMap();
    return nodesMap != null ? nodesMap.containsKey(name) : false;
  }

  /**
   * Connects the given activity to the delimiters of this structure.
   */
  protected void addImplicitTransitions(Activity activity) {
    begin.connect(activity);
    activity.connect(end);
  }

  /**
   * Disconnects the given activity from the delimiters of this structure.
   */
  protected void removeImplicitTransitions(Activity activity) {
    begin.disconnect(activity);
    activity.disconnect(end);
  }

  public JoinCondition getJoinCondition() {
    return begin.getJoinCondition();
  }

  public void setJoinCondition(JoinCondition joinCondition) {
    begin.setJoinCondition(joinCondition);
  }

  public List getSources() {
    return end.getSources();
  }

  public void addSource(LinkDefinition link) {
    end.addSource(link);
  }

  public LinkDefinition getSource(String linkName) {
    return end.getSource(linkName);
  }

  public LinkDefinition getTarget(String linkName) {
    return begin.getTarget(linkName);
  }

  public List getTargets() {
    return begin.getTargets();
  }

  public void addTarget(LinkDefinition link) {
    begin.addTarget(link);
  }

  // node properties
  // /////////////////////////////////////////////////////////////////////////////

  public void setName(String name) {
    super.setName(name);
    begin.setName(name);
    end.setName(name);
  }

  public Set getArrivingTransitions() {
    return begin.getArrivingTransitions();
  }

  public Transition getDefaultArrivingTransition() {
    return begin.getDefaultArrivingTransition();
  }

  public Transition addArrivingTransition(Transition transition) {
    return begin.addArrivingTransition(transition);
  }

  public void removeArrivingTransition(Transition transition) {
    begin.removeArrivingTransition(transition);
  }

  public List getLeavingTransitions() {
    return end.getLeavingTransitions();
  }

  public Transition getDefaultLeavingTransition() {
    return end.getDefaultLeavingTransition();
  }

  public Transition addLeavingTransition(Transition transition) {
    return end.addLeavingTransition(transition);
  }

  public void removeLeavingTransition(Transition transition) {
    end.removeLeavingTransition(transition);
  }

  /**
   * Handles the incoming flow of execution of structured activities.
   */
  public static class Begin extends Activity {

    private static final long serialVersionUID = 1L;

    Begin() {
    }

    Begin(StructuredActivity structure) {
      structure.adoptActivity(this);
    }

    public void execute(ExecutionContext exeContext) {
      getCompositeActivity().execute(exeContext);
    }

    public void skip(ExecutionContext exeContext) {
      getCompositeActivity().skip(exeContext);
    }

    public String toString() {
      return getCompositeActivity().toString() + "/Begin";
    }
  }

  /**
   * Handles the outgoing flow of execution of structured activities.
   */
  public static class End extends Activity {

    private static final long serialVersionUID = 1L;

    End() {
    }

    End(StructuredActivity structure) {
      structure.adoptActivity(this);
    }

    public void execute(ExecutionContext exeContext) {
      getCompositeActivity().leave(exeContext);
    }

    public String toString() {
      return getCompositeActivity().toString() + "/End";
    }
  }
}