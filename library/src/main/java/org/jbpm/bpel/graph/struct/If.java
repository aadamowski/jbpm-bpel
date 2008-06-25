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
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.bpel.BpelException;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.BpelVisitor;
import org.jbpm.bpel.sublang.def.Expression;
import org.jbpm.bpel.xml.util.DatatypeUtil;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;

/**
 * Selects exactly one activity for execution from a set of choices.
 * @author Juan Cantú
 * @version $Revision$ $Date: 2008/02/01 05:43:08 $
 */
public class If extends StructuredActivity {

  private List conditions = new ArrayList();

  private static final Log log = LogFactory.getLog(If.class);

  private static final long serialVersionUID = 1L;

  public If() {
  }

  public If(String name) {
    super(name);
  }

  public void execute(ExecutionContext exeContext) {
    Token token = exeContext.getToken();

    // iterate over the conditions to select a branch
    Activity selectedBranch = null;
    for (int i = 0, n = conditions.size(); i < n; i++) {
      Activity branch = (Activity) getActivities().get(i);

      // if no branch has been selected,
      if (selectedBranch == null) {
        // evaluate the associated condition
        Expression condition = (Expression) conditions.get(i);
        log.debug("evaluating " + condition + " for " + token);

        // if the condition holds true,
        if (DatatypeUtil.toBoolean(condition.getEvaluator().evaluate(token))) {
          // select the branch
          selectedBranch = branch;
          log.debug("selected branch " + branch + " for " + token);
          continue;
        }
      }
      branch.eliminatePath(token);
    }

    Activity _else = getElse();
    if (_else != null) {
      // no branch has been selected?
      if (selectedBranch == null) {
        // take the else branch
        selectedBranch = _else;
        log.debug("no branch with condition selected, taking else branch: " + _else);
      }
      else
        _else.eliminatePath(token);
    }
    else if (selectedBranch == null) {
      // no else branch, go to end
      selectedBranch = getEnd();
      log.debug("no branch with condition selected and no else branch present");
    }
    getBegin().leave(exeContext, selectedBranch.getDefaultArrivingTransition());
  }

  // children management
  // /////////////////////////////////////////////////////////////////////////////

  protected void addActivity(Activity activity) {
    List activities = getActivities();
    int conditionCount = conditions.size();
    // is there a default activity?
    if (activities.size() > conditionCount) {
      // remove the default activity temporarily
      Object _else = activities.remove(conditionCount);
      // attach the new activity
      super.addActivity(activity);
      // move the default activity to the end of the list
      activities.add(_else);
    }
    else {
      // do the actual addition
      super.addActivity(activity);
    }
    /*
     * put a null placeholder in the conditions list; a future setCondition() call will associate a
     * condition with the new activity
     */
    conditions.add(null);
  }

  protected void removeActivity(Activity activity) {
    int activityIndex = getActivities().indexOf(activity);
    // if the activity is conditional, remove the associated condition
    if (activityIndex < conditions.size())
      conditions.remove(activityIndex);
    // do the actual removal
    super.removeActivity(activity);
  }

  public void reorderNode(int oldIndex, int newIndex) {
    super.reorderNode(oldIndex, newIndex);
    /*
     * activities take along their associated conditions; when trading places with the default
     * activity, the condition remains in the same place
     */
    int elseIndex = conditions.size();
    if (oldIndex < elseIndex && newIndex < elseIndex) {
      Object condition = conditions.remove(oldIndex);
      conditions.add(newIndex, condition);
    }
  }

  // If getters and setters
  // /////////////////////////////////////////////////////////////////////////////

  public Expression getCondition(Activity activity) {
    int index = getActivities().indexOf(activity);
    if (index == -1)
      throw new BpelException("cannot get the condition of a non-member activity");

    return index < conditions.size() ? (Expression) conditions.get(index) : null;
  }

  public void setCondition(Activity activity, Expression condition) {
    int index = getActivities().indexOf(activity);
    if (index == -1)
      throw new BpelException("cannot set a condition for a non-member activity");

    // is the given activity conditional?
    if (index < conditions.size()) {
      // match the condition with the activity
      conditions.set(index, condition);
    }
    else {
      // make the default activity conditional
      conditions.add(condition);
    }
  }

  public Activity getElse() {
    List activities = getActivities();
    int conditionCount = conditions.size();
    return activities.size() > conditionCount ? (Activity) activities.get(conditionCount) : null;
  }

  public void setElse(Activity _else) {
    List activities = getActivities();
    int index = activities.indexOf(_else);
    if (index == -1)
      throw new BpelException("cannot set a non-member activity as otherwise");

    // is the given activity conditional?
    int conditionCount = conditions.size();
    if (index < conditionCount) {
      // is there a default activity?
      if (activities.size() > conditionCount) {
        // remove the old default activity
        Activity oldElse = (Activity) activities.get(conditionCount);
        removeActivity(oldElse);
      }
      // move the activity to the tail of the list
      activities.remove(index);
      activities.add(_else);
      // drop the condition
      conditions.remove(index);
    }
    // else the activity is already the default
  }

  public List getBranches() {
    return getActivities().subList(0, conditions.size());
  }

  List getConditions() {
    return conditions;
  }

  public void accept(BpelVisitor visitor) {
    visitor.visit(this);
  }
}
