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

import org.jbpm.bpel.graph.def.AbstractActivityDbTestCase;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.integration.def.ReplyAction;
import org.jbpm.graph.def.Action;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/08/08 11:20:32 $
 */
public class ReplyDbTest extends AbstractActivityDbTestCase {

  public void testReplyAction() {
    ReplyAction replyAction = new ReplyAction();

    Reply reply = createReply();
    reply.setAction(replyAction);
    String replyName = reply.getName();

    processDefinition.getGlobalScope().setActivity(reply);

    processDefinition = saveAndReload(processDefinition);
    reply = (Reply) session.load(Reply.class, new Long(processDefinition.getGlobalScope()
        .getActivity()
        .getId()));

    Action action = reply.getAction();
    assertEquals(replyName, action.getName());
    /*
     * when AbstractDbTestCase.newTransaction() simply ends the current transaction and begins a new
     * one, the session preserves the objects already loaded; thus, action is still a ReplyAction
     */
    // assertFalse(action instanceof ReplyAction);

    // verify proxy reacquisition
    replyAction = reply.getReplyAction();
    assertEquals(replyName, replyAction.getName());

    action = reply.getAction();
    assertTrue(action instanceof ReplyAction);

  }

  protected Activity createActivity() {
    return createReply();
  }

  private Reply createReply() {
    return new Reply("reply");
  }
}
