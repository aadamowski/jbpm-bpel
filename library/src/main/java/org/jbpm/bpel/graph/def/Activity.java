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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.JbpmContext;
import org.jbpm.bpel.graph.exe.BpelFaultException;
import org.jbpm.bpel.graph.scope.Scope;
import org.jbpm.bpel.persistence.db.BpelGraphSession;
import org.jbpm.bpel.sublang.def.JoinCondition;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.bpel.xml.util.DatatypeUtil;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.GraphElement;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.def.Transition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;
import org.jbpm.util.Clock;

/**
 * Activities perform the process logic. They are divided into 2 classes: basic and structured.
 * @author Juan Cantú
 * @version $Revision$ $Date: 2008/02/01 05:43:08 $
 */
public abstract class Activity extends Node {

  private CompositeActivity compositeActivity;

  private List sources = new ArrayList();
  private List targets = new ArrayList();

  private JoinCondition joinCondition;
  private Boolean suppressJoinFailure;

  private boolean unnamed;

  private static final Log log = LogFactory.getLog(Activity.class);

  protected Activity() {
  }

  protected Activity(String name) {
    // at construction time, setName() cannot do any meaningful check
    this.name = name;
  }

  public void accept(BpelVisitor visitor) {
    // ignore call in non-visitable activities
  }

  // jbpm override
  // /////////////////////////////////////////////////////////////////////////////

  public void setName(String name) {
    if (compositeActivity != null && compositeActivity.hasNode(name))
      throw new IllegalArgumentException("duplicate name: " + name);

    this.name = name;
  }

  public boolean isUnnamed() {
    return unnamed;
  }

  public void setUnnamed(boolean unnamed) {
    this.unnamed = unnamed;
  }

  /**
   * Called by a transition to pass execution to this activity. Execution does not start until the
   * status of all incoming links has been determined and the (implicit or explicit) join condition
   * has been evaluated.
   */
  public void enter(ExecutionContext exeContext) {
    Token token = exeContext.getToken();
    /*
     * if token has ended, it means the enclosing scope has terminated, the process has exited or,
     * in general, some agent has requested execution to cease
     */
    if (token.hasEnded())
      return;

    // update the runtime context information
    token.setNode(this);

    // fire the leave-node event for this node
    fireEvent(Event.EVENTTYPE_NODE_ENTER, exeContext);

    // keep track of node entrance, so that a node-log can be generated upon leaving the node
    token.setNodeEnter(Clock.getCurrentTime());

    // remove transition references from execution context
    exeContext.setTransition(null);
    exeContext.setTransitionSource(null);

    // execute activity
    try {
      if (!targets.isEmpty()) {
        setTargetsToken(token);
        if (!areTargetsDetermined(token) || evaluateJoinCondition(token) == false)
          return;
      }
      execute(exeContext);
    }
    catch (BpelFaultException e) {
      raiseException(e, exeContext);
    }
  }

  private void setTargetsToken(Token token) {
    for (int i = 0, n = targets.size(); i < n; i++) {
      LinkDefinition target = (LinkDefinition) targets.get(i);
      target.getInstance(token).setTargetToken(token);
    }
  }

  private boolean areTargetsDetermined(Token token) {
    for (int i = 0, n = targets.size(); i < n; i++) {
      LinkDefinition target = (LinkDefinition) targets.get(i);
      if (target.getInstance(token).getStatus() == null) {
        log.debug("not executing "
            + this
            + " for "
            + token
            + ", found undetermined link: "
            + target.getName());
        return false;
      }
    }
    return true;
  }

  /**
   * Tells whether this activity is ready to execute. Being ready depends on:
   * <ol>
   * <li>having determined the status of all incoming links</li>
   * <li>obtaining a positive result from evaluating the join condition</li>
   * </ol>
   * When the result is negative and {@link #suppressJoinFailure()} returns <code>true</code>,
   * this activity is {@linkplain #skip(ExecutionContext) skipped} before returning the result.
   */
  private boolean evaluateJoinCondition(Token token) {
    if (joinCondition != null) {
      // evaluate explicit join condition
      if (DatatypeUtil.toBoolean(joinCondition.getEvaluator().evaluate(token)))
        return true;
    }
    else {
      // the implicit condition requires the status of at least one incoming link to be positive
      for (int i = 0, n = targets.size(); i < n; i++) {
        LinkDefinition target = (LinkDefinition) targets.get(i);
        if (target.getInstance(token).getStatus().booleanValue())
          return true;
      }
    }

    // join failed, throw join failure unless suppressed
    if (!suppressJoinFailure())
      throw new BpelFaultException(BpelConstants.FAULT_JOIN_FAILURE);

    // eliminate path
    skip(new ExecutionContext(token));
    return false;
  }

  /**
   * Called to continue execution over the default leaving transition. If the activity has outgoing
   * links, their status is determined before execution continues.
   */
  public void leave(ExecutionContext exeContext) {
    Token token = exeContext.getToken();
    /*
     * if token has ended, it means the enclosing scope has terminated, the process has exited or,
     * in general, some agent has requested execution to cease
     */
    if (token.hasEnded())
      return;

    if (!sources.isEmpty()) {
      for (int i = 0, n = sources.size(); i < n; i++) {
        try {
          LinkDefinition source = (LinkDefinition) sources.get(i);
          source.determineStatus(token);
        }
        catch (BpelFaultException e) {
          /*
           * WS-BPEL 2.0 section 11.6.2 If an error occurs while evaluating the transition condition
           * of one of an activity's outgoing links, then all remaining outgoing links with targets
           * within the source activity's enclosing scope MUST NOT have their transition conditions
           * evaluated and remain in the unset state.
           */
          Scope scope = getScope();
          for (; i < n; i++) {
            LinkDefinition source = (LinkDefinition) sources.get(i);
            if (!scope.hasActivity(source.getTarget())) {
              /*
               * However, if the target of a remaining outgoing link is outside the source
               * activity's enclosing scope then the status of the link MUST be set to false.
               */
              source.getInstance(token).statusDetermined(false);
            }
          }
          // throwing the exception ensures the remaining outgoing links stay unset
          throw e;
        }
      }
    }
    // continue execution
    proceed(exeContext);
  }

  private void proceed(ExecutionContext exeContext) {
    Transition transition = getDefaultLeavingTransition();
    if (transition == null) {
      // fire node-leave event for this activity
      fireEvent(Event.EVENTTYPE_NODE_LEAVE, exeContext);
      // complete the enclosing scope instance
      Scope.getInstance(exeContext.getToken()).completed();
    }
    else
      leave(exeContext, transition);
  }

  /**
   * Returns the {@link CompositeActivity} or {@link ProcessDefinition} in which this activity is
   * contained.
   */
  public GraphElement getParent() {
    return compositeActivity;
  }

  public ProcessDefinition getProcessDefinition() {
    return compositeActivity.getProcessDefinition();
  }

  // behavior methods
  // /////////////////////////////////////////////////////////////////////////////

  /**
   * Called when the enclosing {@linkplain #getScope() scope} is forced to terminate. Concrete
   * activities override this method to perform specific termination duties.
   */
  public void terminate(ExecutionContext exeContext) {
  }

  /**
   * Called when this activity is not performed due to the value of the
   * {@link #getSuppressJoinFailure() suppressJoinFailure} attribute being <code>yes</code> and
   * the (implicit or explicit) {@linkplain #getJoinCondition() join condition} evaluating to
   * <code>false</code>.
   */
  public void skip(ExecutionContext exeContext) {
    Token token = exeContext.getToken();
    /*
     * if token has ended, it means the enclosing scope has terminated, the process has exited or,
     * in general, some agent has requested execution to cease
     */
    if (token.hasEnded())
      return;

    // dead path elimination
    eliminatePath(token);

    // continue execution
    proceed(exeContext);
  }

  /**
   * Sets the status of all outgoing links to <code>false</code>. To avoid violating control
   * dependencies, this is only performed after the status of all incoming links has been
   * determined.
   */
  public void eliminatePath(Token token) {
    if (!targets.isEmpty()) {
      setTargetsToken(token);
      if (!areTargetsDetermined(token))
        return;
    }

    if (!sources.isEmpty())
      setSourcesNegativeStatus(token);
  }

  private void setSourcesNegativeStatus(Token token) {
    for (int i = 0, n = sources.size(); i < n; i++) {
      LinkDefinition source = (LinkDefinition) sources.get(i);
      source.getInstance(token).statusDetermined(false);
    }
  }

  public void targetDetermined(Token token) {
    // ignore the notification until every link is determined
    if (!areTargetsDetermined(token))
      return;

    try {
      if (!equals(token.getNode())) {
        // token left: dealing with a path previously eliminated by a structure
        setSourcesNegativeStatus(token);
      }
      else if (evaluateJoinCondition(token))
        execute(new ExecutionContext(token));
    }
    catch (BpelFaultException e) {
      raiseException(e, new ExecutionContext(token));
    }
  }

  protected boolean suppressJoinFailure() {
    return suppressJoinFailure != null ? suppressJoinFailure.booleanValue()
        : getCompositeActivity().suppressJoinFailure();
  }

  // standard attributes and elements
  // /////////////////////////////////////////////////////////////////////////////

  public JoinCondition getJoinCondition() {
    return joinCondition;
  }

  public void setJoinCondition(JoinCondition joinCondition) {
    this.joinCondition = joinCondition;
  }

  public Boolean getSuppressJoinFailure() {
    return suppressJoinFailure;
  }

  public void setSuppressJoinFailure(Boolean suppressJoinFailure) {
    this.suppressJoinFailure = suppressJoinFailure;
  }

  /**
   * Retrieves a target link by name.
   */
  public LinkDefinition getTarget(String name) {
    if (!targets.isEmpty()) {
      for (int i = 0, n = targets.size(); i < n; i++) {
        LinkDefinition target = (LinkDefinition) targets.get(i);
        if (name.equals(target.getName()))
          return target;
      }
    }
    return null;
  }

  /**
   * Retrieves a list of all target {@linkplain LinkDefinition links}.
   */
  public List getTargets() {
    return targets;
  }

  /**
   * Adds a bidirectional relation between this activity and the given target link.
   * @throws IllegalArgumentException if link is null
   */
  public void addTarget(LinkDefinition link) {
    if (link == null)
      throw new IllegalArgumentException("link is null");

    targets.add(link);
    link.setTarget(this);
  }

  /**
   * Removes the bidirectional relation between this activity and the given target link.
   * @throws IllegalArgumentException if link is null
   */
  public void removeTarget(LinkDefinition link) {
    if (link == null)
      throw new IllegalArgumentException("link is null");

    if (targets.remove(link))
      link.setTarget(null);
  }

  /**
   * Retrieves a source link by name.
   */
  public LinkDefinition getSource(String name) {
    if (!sources.isEmpty()) {
      for (int i = 0, n = sources.size(); i < n; i++) {
        LinkDefinition source = (LinkDefinition) sources.get(i);
        if (name.equals(source.getName()))
          return source;
      }
    }
    return null;
  }

  /**
   * Retrieves a list of all source {@linkplain LinkDefinition links}.
   */
  public List getSources() {
    return sources;
  }

  /**
   * Adds a bidirectional relation between this activity and the given source link.
   * @throws IllegalArgumentException if link is null
   */
  public void addSource(LinkDefinition link) {
    if (link == null)
      throw new IllegalArgumentException("link is null");

    sources.add(link);
    link.setSource(this);
  }

  /**
   * Removes the bidirectional relation between this activity and the given source link.
   * @throws IllegalArgumentException if link is null
   */
  public void removeSource(LinkDefinition link) {
    if (link == null)
      throw new IllegalArgumentException("link is null");

    if (sources.remove(link))
      link.setSource(null);
  }

  /**
   * Returns the immediately enclosing composite activity.
   */
  public CompositeActivity getCompositeActivity() {
    return compositeActivity;
  }

  /**
   * Sets the immediately enclosing composite activity.
   */
  void setCompositeActivity(CompositeActivity compositeActivity) {
    this.compositeActivity = compositeActivity;
  }

  // utility methods
  // /////////////////////////////////////////////////////////////////////////////

  /**
   * Detaches this activity from its immediately enclosing
   * {@linkplain #getCompositeActivity() composite activity}.
   */
  public void detachFromParent() {
    if (compositeActivity != null) {
      compositeActivity.removeNode(this);
      compositeActivity = null;
    }
  }

  /**
   * Returns the enclosing process definition.
   */
  public BpelProcessDefinition getBpelProcessDefinition() {
    return (BpelProcessDefinition) getProcessDefinition();
  }

  /**
   * Returns the nearest enclosing scope.
   */
  public Scope getScope() {
    // easy way out: composite activity is not a scope
    if (!compositeActivity.isScope())
      return compositeActivity.getScope();

    // check whether composite activity has the proper type already
    if (compositeActivity instanceof Scope)
      return (Scope) compositeActivity;

    // acquire proxy of the proper type
    JbpmContext jbpmContext = JbpmContext.getCurrentJbpmContext();
    BpelGraphSession graphSession = BpelGraphSession.getContextInstance(jbpmContext);
    Scope scope = graphSession.loadScope(compositeActivity.getId());

    // update composite activity reference
    compositeActivity = scope;
    return scope;
  }

  /**
   * Mirrors the {@link #getDefaultLeavingTransition()} method to ease manipulation of activities
   * with a single arriving transition. Notice that every standard activity in a BPEL process has
   * one arriving and one leaving transition because links are implemented as context variables.
   */
  public Transition getDefaultArrivingTransition() {
    return (Transition) getArrivingTransitions().iterator().next();
  }

  /**
   * An activity is initial if there is no basic activity that logically precedes it in the behavior
   * of the process.
   */
  public boolean isInitial() {
    return (compositeActivity == null || compositeActivity.isInitial()
        && compositeActivity.isChildInitial(this))
        && getTargets().isEmpty();
  }

  /**
   * Connects this activity to the given activity by creating an implicit transition between them.
   */
  public void connect(Activity activity) {
    // create an implicit transition
    Transition transition = new Transition();
    transition.setName(getName() + '-' + activity.getName());
    transition.setProcessDefinition(processDefinition);

    // add transition to edge nodes
    addLeavingTransition(transition);
    activity.addArrivingTransition(transition);
  }

  /**
   * Tells whether this activity is connected to the given activity.
   */
  public boolean isConnected(Activity activity) {
    return findTransition(activity) != null;
  }

  /**
   * Disconnects this activity from the given activity by removing the implicit transition between
   * them.
   */
  public void disconnect(Activity activity) {
    Transition transition = findTransition(activity);
    if (transition != null) {
      removeLeavingTransition(transition);
      activity.removeArrivingTransition(transition);
    }
  }

  private Transition findTransition(Activity activity) {
    List leavingTransitions = getLeavingTransitions();
    if (leavingTransitions != null) {
      Set arrivingTransitions = activity.getArrivingTransitions();
      if (arrivingTransitions != null) {
        for (int i = 0, n = leavingTransitions.size(); i < n; i++) {
          Transition transition = (Transition) leavingTransitions.get(i);
          if (arrivingTransitions.contains(transition))
            return transition;
        }
      }
    }
    return null;
  }
}