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

import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.AbstractActivityDbTestCase;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.integration.def.ReceiveAction;
import org.jbpm.graph.def.Action;

/**
 * @author Juan Cantu
 * @version $Revision$ $Date: 2007/08/08 11:20:32 $
 */
public class ReceiveDbTest extends AbstractActivityDbTestCase {

  public void testCreateInstance() {
    // prepare persistent objects
    // receive
    Receive receive = createReceive();
    receive.setCreateInstance(true);

    putReceive(processDefinition, receive);

    // save objects and load them back
    processDefinition = saveAndReload(processDefinition);
    receive = getReceive(processDefinition);

    // verify the retrieved objects
    assertTrue(receive.isCreateInstance());
  }

  public void testReceiveAction() {
    // prepare persistent objects
    // receive action
    ReceiveAction receiveAction = new ReceiveAction();
    // receive
    Receive receive = createReceive();
    receive.setAction(receiveAction);
    String receiveName = receive.getName();

    putReceive(processDefinition, receive);

    // save objects and load them back
    processDefinition = saveAndReload(processDefinition);
    receive = getReceive(processDefinition);

    // verify the retrieved objects
    Action action = receive.getAction();
    assertEquals(receiveName, action.getName());
    /*
     * when AbstractDbTestCase.newTransaction() simply ends the current transaction and begins a new
     * one, the session preserves the objects already loaded; thus, action is still a ReceiveAction
     */
    // assertFalse(action instanceof ReceiveAction);

    // verify proxy reacquisition
    receiveAction = receive.getReceiveAction();
    assertEquals(receiveName, receiveAction.getName());

    action = receive.getAction();
    assertTrue(action instanceof ReceiveAction);
  }

  protected Activity createActivity() {
    return createReceive();
  }

  private Receive createReceive() {
    return new Receive("receive");
  }

  private void putReceive(BpelProcessDefinition processDefinition, Receive receive) {
    processDefinition.getGlobalScope().setActivity(receive);
  }

  private Receive getReceive(BpelProcessDefinition processDefinition) {
    return (Receive) session.load(Receive.class, new Long(processDefinition.getGlobalScope()
        .getActivity()
        .getId()));
  }
}
