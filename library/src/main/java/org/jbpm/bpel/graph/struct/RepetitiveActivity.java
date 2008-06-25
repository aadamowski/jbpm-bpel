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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.JbpmContext;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.CompositeActivity;
import org.jbpm.bpel.persistence.db.BpelGraphSession;
import org.jbpm.bpel.sublang.def.Expression;
import org.jbpm.graph.def.Transition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;

/**
 * Common base of repetitive activities.
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2008/02/01 05:43:08 $
 */
public abstract class RepetitiveActivity extends StructuredActivity {

  private Expression condition;
  protected Loop loop = new Loop(this);

  private static Log log = LogFactory.getLog(RepetitiveActivity.class);

  protected RepetitiveActivity() {
  }

  protected RepetitiveActivity(String name) {
    super(name);
    loop.setName(name);
  }

  public Expression getCondition() {
    return condition;
  }

  public void setCondition(Expression condition) {
    this.condition = condition;
  }

  public Loop getLoop() {
    return loop;
  }

  protected abstract boolean repeatExecution(Token token);

  protected void addActivity(Activity activity) {
    // ensure this structure has only one enclosed activity
    List activities = getActivities();
    if (!activities.isEmpty())
      removeActivity((Activity) activities.get(0));

    super.addActivity(activity);
  }

  public void setName(String name) {
    super.setName(name);
    loop.setName(name);
  }

  /**
   * Loop behavior of repetitive activities.
   */
  public static class Loop extends Activity {

    private static final long serialVersionUID = 1L;

    private static ThreadLocal entranceLocal = new ThreadLocal() {

      protected Object initialValue() {
        return new HashSet();
      }
    };

    Loop() {
    }

    Loop(RepetitiveActivity repetitiveActivity) {
      repetitiveActivity.adoptActivity(this);
    }

    public void execute(ExecutionContext exeContext) {
      if (isMarked()) {
        unmark();
        log.debug("reentrance detected on "
            + getCompositeActivity()
            + " for "
            + exeContext.getToken());
        return;
      }

      Token token = exeContext.getToken();
      RepetitiveActivity repetitiveActivity = getRepetitiveActivity();

      for (;;) {
        // evaluate condition
        if (repetitiveActivity.repeatExecution(token)) {
          // mark entrance
          mark();

          // activity transition
          leave(exeContext, (Transition) getLeavingTransitions().get(0));

          // verify mark
          if (unmark()) {
            log.debug("wait state reached from " + repetitiveActivity + " for " + token);
            break;
          }
          log.debug("continuing " + repetitiveActivity + " for " + token);
        }
        else {
          // end transition
          log.debug("breaking " + repetitiveActivity + " for " + token);
          leave(exeContext, (Transition) getLeavingTransitions().get(1));
          break;
        }
      }
    }

    public RepetitiveActivity getRepetitiveActivity() {
      CompositeActivity compositeActivity = getCompositeActivity();

      // easy way out: no composite activity
      if (compositeActivity == null)
        return null;

      // check whether composite activity has the proper type already
      if (compositeActivity instanceof RepetitiveActivity)
        return (RepetitiveActivity) compositeActivity;

      // acquire proxy of the proper type
      JbpmContext jbpmContext = JbpmContext.getCurrentJbpmContext();
      BpelGraphSession graphSession = BpelGraphSession.getContextInstance(jbpmContext);
      RepetitiveActivity repetitiveActivity = graphSession.loadRepetitiveActivity(compositeActivity.getId());

      // update composite activity reference
      repetitiveActivity.adoptActivity(this);
      return repetitiveActivity;
    }

    public String toString() {
      return getCompositeActivity().toString() + "/Loop";
    }

    private boolean isMarked() {
      return getEntranceMarks().contains(this);
    }

    private void mark() {
      getEntranceMarks().add(this);
    }

    private boolean unmark() {
      return getEntranceMarks().remove(this);
    }

    private static Set getEntranceMarks() {
      return (Set) entranceLocal.get();
    }
  }
}
