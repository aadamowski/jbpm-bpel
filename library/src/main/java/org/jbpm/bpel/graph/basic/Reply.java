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
package org.jbpm.bpel.graph.basic;

import org.jbpm.JbpmContext;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.BpelVisitor;
import org.jbpm.bpel.integration.def.InboundMessageActivity;
import org.jbpm.bpel.integration.def.ReplyAction;
import org.jbpm.bpel.persistence.db.IntegrationSession;
import org.jbpm.graph.def.Action;

/**
 * Sends a response to a message that was received by an
 * {@linkplain InboundMessageActivity inbound message activity} (IMA). The combination of an IMA and
 * a <tt>reply</tt> forms a request-response operation.
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/11/02 16:46:21 $
 */
public class Reply extends Activity {

  private static final long serialVersionUID = 1L;

  public Reply() {
  }

  public Reply(String name) {
    super(name);
  }

  public ReplyAction getReplyAction() {
    if (action == null)
      return null;

    if (action instanceof ReplyAction)
      return (ReplyAction) action;

    // reacquire proxy of the proper type
    JbpmContext jbpmContext = JbpmContext.getCurrentJbpmContext();
    IntegrationSession integrationSession = IntegrationSession.getContextInstance(jbpmContext);
    ReplyAction replyAction = integrationSession.loadReplyAction(action.getId());

    // update action reference
    action = replyAction;

    return replyAction;
  }

  public void setAction(Action action) {
    if (!(action instanceof ReplyAction))
      throw new IllegalArgumentException("not a reply action: " + action);

    this.action = action;
    action.setName(name);
  }

  public void accept(BpelVisitor visitor) {
    visitor.visit(this);
  }
}
