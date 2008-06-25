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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.JbpmContext;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.BpelVisitor;
import org.jbpm.bpel.graph.def.LinkDefinition;
import org.jbpm.bpel.persistence.db.BpelGraphSession;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;

/**
 * Specifies one or more activities to be performed concurrently. {@link LinkDefinition links} can
 * be used within a <tt>flow</tt> to define explicit control dependencies between nested child
 * activities.
 * @author Juan Cantú
 * @version $Revision$ $Date: 2008/02/01 05:43:08 $
 */
public class Flow extends StructuredActivity {

  private Map links = new HashMap();

  private static final Log log = LogFactory.getLog(Flow.class);
  private static final long serialVersionUID = 1L;

  public Flow() {
  }

  public Flow(String name) {
    super(name);
  }

  // behaviour methods
  // /////////////////////////////////////////////////////////////////////////////

  public void execute(ExecutionContext exeContext) {
    // initialize links
    Token parentToken = initializeLinks(exeContext.getToken());

    // create concurrent tokens
    Token[] concurrentTokens = createConcurrentTokens(parentToken);

    // execute concurrent tokens on activities
    List activities = getNodes();
    for (int i = 0, n = concurrentTokens.length; i < n; i++) {
      Activity activity = (Activity) activities.get(i);
      getBegin().leave(new ExecutionContext(concurrentTokens[i]),
          activity.getDefaultArrivingTransition());
      // stop executing concurrent tokens if parent token is prematurely ended
      if (parentToken.hasEnded())
        break;
    }
  }

  public Token initializeLinks(Token parentToken) {
    // easy way out: no links to initialize
    if (links.isEmpty())
      return parentToken;

    // a new token is required to contain link instances
    Token linksToken = new Token(parentToken, getName());

    for (Iterator i = links.values().iterator(); i.hasNext();) {
      LinkDefinition link = (LinkDefinition) i.next();
      link.createInstance(linksToken);
    }
    return linksToken;
  }

  public Token[] createConcurrentTokens(Token parentToken) {
    List activities = getNodes();
    int activityCount = activities.size();
    Token[] concurrentTokens = new Token[activityCount];

    for (int i = 0; i < activityCount; i++) {
      Activity activity = (Activity) activities.get(i);
      concurrentTokens[i] = new Token(parentToken, activity.getName());
    }
    return concurrentTokens;
  }

  public void terminate(ExecutionContext exeContext) {
    for (Iterator i = exeContext.getToken().getChildren().values().iterator(); i.hasNext();) {
      Token concurrentToken = (Token) i.next();
      /*
       * if concurrent token has ended, it means the concurrent activity has completed, the process
       * has exited or, in general, some agent has requested execution to cease
       */
      if (concurrentToken.hasEnded())
        continue;

      // terminate nested activities
      Activity activity = (Activity) concurrentToken.getNode();
      activity.terminate(new ExecutionContext(concurrentToken));

      // end concurrent token (do not verify parent termination)
      // also make concurrent token unable to reactivate parent
      concurrentToken.end(false);
    }
  }

  public void leave(ExecutionContext exeContext) {
    Token concurrentToken = exeContext.getToken();
    /*
     * if concurrent token has ended, it means the enclosing scope has terminated, the process has
     * exited or, in general, some agent has requested execution to cease
     */
    if (concurrentToken.hasEnded())
      return;

    // end concurrent token (do not verify parent termination)
    // also make concurrent token unable to reactivate parent
    concurrentToken.end(false);

    if (mustReactivateParent(concurrentToken, exeContext.getJbpmContext())) {
      Token parentToken;
      // if a token was created to contain link instances, end it
      if (!links.isEmpty()) {
        Token linksToken = concurrentToken.getParent();
        linksToken.end(false);
        parentToken = linksToken.getParent();
      }
      else
        parentToken = concurrentToken.getParent();

      getEnd().leave(new ExecutionContext(parentToken));
    }
  }

  public void eliminatePath(Token parentToken) {
    /*
     * create link instances before eliminating path! otherwise path elimination will try to set the
     * status of links whose instances do not even exist
     */
    super.eliminatePath(initializeLinks(parentToken));
  }

  protected boolean mustReactivateParent(Token concurrentToken, JbpmContext jbpmContext) {
    BpelGraphSession graphSession = BpelGraphSession.getContextInstance(jbpmContext);

    for (Iterator i = concurrentToken.getParent().getChildren().values().iterator(); i.hasNext();) {
      Token siblingToken = (Token) i.next();
      if (siblingToken == concurrentToken)
        continue;
      /*
       * acquire lock on the concurrent token to prevent race conditions between siblings where all
       * of them determine the parent must not be reactivated yet
       */
      if (graphSession != null)
        graphSession.lockToken(siblingToken);

      if (siblingToken.isAbleToReactivateParent()) {
        log.debug("not leaving "
            + this
            + " for "
            + concurrentToken
            + ", found active sibling: "
            + siblingToken);
        return false;
      }
    }
    return true;
  }

  protected boolean isChildInitial(Activity child) {
    /*
     * this method is only invoked from child.isInitial() on its composite activity; therefore, it
     * is valid to assume the argument is one of the concurrent activities of this flow
     */
    return true;
  }

  // LinkDefinition methods
  // /////////////////////////////////////////////////////////////////////////////

  public LinkDefinition findLink(String name) {
    LinkDefinition link = getLink(name);
    return link != null ? link : super.findLink(name);
  }

  public void addLink(LinkDefinition link) {
    links.put(link.getName(), link);
  }

  public Map getLinks() {
    return links;
  }

  public LinkDefinition getLink(String name) {
    return (LinkDefinition) links.get(name);
  }

  public void accept(BpelVisitor visitor) {
    visitor.visit(this);
  }
}
