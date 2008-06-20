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
package org.jbpm.bpel.integration.def;

import java.util.Date;
import java.util.List;

import org.jbpm.bpel.graph.basic.Receive;
import org.jbpm.bpel.graph.def.AbstractBpelVisitor;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.exe.ScopeInstance;
import org.jbpm.bpel.graph.scope.Scope;
import org.jbpm.bpel.graph.struct.Flow;
import org.jbpm.bpel.graph.struct.If;
import org.jbpm.bpel.graph.struct.Pick;
import org.jbpm.bpel.graph.struct.Sequence;
import org.jbpm.bpel.graph.struct.While;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;

/**
 * @author Juan Cantu
 * @version $Revision$ $Date: 2007/07/26 00:39:10 $
 */
class ProcessInstanceStarter extends AbstractBpelVisitor {

  private final ReceiveAction messageTarget;
  private final Token token;

  private Token receivingToken;

  ProcessInstanceStarter(ReceiveAction messageTarget, Token token) {
    this.messageTarget = messageTarget;
    this.token = token;
  }
  
  public Token getReceivingToken() {
    return receivingToken;
  }

  public void visit(BpelProcessDefinition process) {
    /*
     * BPEL-89: fire process start event; process instance does not fire it because BPEL processes
     * do not have a start node
     */
    process.fireEvent(Event.EVENTTYPE_PROCESS_START, new ExecutionContext(token));
    visit(process.getGlobalScope());
  }

  public void visit(Receive receive) {
    assert receive.isCreateInstance() : receive;

    if (receive.getReceiveAction().equals(messageTarget)) {
      assert receivingToken == null : receivingToken;
      enterInitialActivity(receive, receivingToken = token);
    }
    else
      enterInnerActivity(receive, token);
  }

  public void visit(Sequence sequence) {    
    Activity firstActivity = (Activity) sequence.getNodes().get(0);

    if (firstActivity.isInitial()) {
      enterInitialActivity(sequence.getBegin(), token);
      firstActivity.accept(this);
    }
    else
      enterInnerActivity(sequence, token);
  }

  public void visit(If _if) {
    // do not propagate visit, no start activities here
  }

  public void visit(While _while) {
    // do not propagate visit, no start activities here
  }

  public void visit(Pick pick) {
    assert pick.isCreateInstance() : pick;

    if (pick.getOnMessages().contains(messageTarget)) {
      assert receivingToken == null : receivingToken;
      enterInitialActivity(pick, receivingToken = token);
    }
    else
      enterInnerActivity(pick, token);
  }

  public void visit(Flow flow) {
    // "enter" flow
    enterInitialActivity(flow.getBegin(), token);

    // initialize links
    Token flowToken = flow.initializeLinks(token);

    // create concurrent tokens
    List activities = flow.getNodes();
    Token[] concurrentTokens = flow.createConcurrentTokens(flowToken);
    assert activities.size() == concurrentTokens.length : concurrentTokens.length;

    // execute concurrent tokens on activities
    for (int i = 0, n = activities.size(); i < n; i++) {
      Activity activity = (Activity) activities.get(i);
      Token concurrentToken = concurrentTokens[i];

      if (activity.isInitial())
        visitInitialActivity(activity, concurrentToken);
      else
        enterInnerActivity(activity, concurrentToken);

      // stop if flow token is prematurely ended
      if (flowToken.hasEnded())
        break;
    }
  }

  public void visit(Scope scope) {
    // "enter" scope
    enterInitialActivity(scope, token);

    // instantiate scope
    Token scopeToken = token.isRoot() ? token : new Token(token,
        scope.getName());
    ScopeInstance scopeInstance = scope.createInstance(scopeToken);

    // initialize data and events
    scopeInstance.initializeData();
    scopeInstance.enableEvents();

    // execute primary token on activity
    Activity activity = scope.getActivity();
    Token primaryToken = scopeInstance.getPrimaryToken();

    if (activity.isInitial())
      visitInitialActivity(activity, primaryToken);
    else
      enterInnerActivity(activity, primaryToken);
  }

  private void visitInitialActivity(Activity activity, Token visitingToken) {
    ProcessInstanceStarter recursiveStarter = new ProcessInstanceStarter(messageTarget,
        visitingToken);
    activity.accept(recursiveStarter);

    if (recursiveStarter.receivingToken != null) {
      assert receivingToken == null : receivingToken;
      receivingToken = recursiveStarter.receivingToken;
    }
  }

  private static void enterInnerActivity(Activity activity, Token token) {
    activity.enter(new ExecutionContext(token));
  }

  private static void enterInitialActivity(Activity activity, Token token) {
    // update the runtime context information
    token.setNode(activity);

    // fire the enter-node event for this node
    activity.fireEvent(Event.EVENTTYPE_NODE_ENTER, new ExecutionContext(token));

    // keep track of node entrance in the token, so that a node-log can be
    // generated at node leave time.
    token.setNodeEnter(new Date());
  }
}