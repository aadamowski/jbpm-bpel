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
package org.jbpm.bpel.graph.exe;

import java.util.Date;

import org.jbpm.bpel.graph.basic.Empty;
import org.jbpm.graph.def.Action;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.instantiation.Delegation;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2006/09/27 03:53:01 $
 */
public class EventExeTest extends AbstractExeTestCase {

  static Date endDate;

  public void setUp() throws Exception {
    super.setUp();
    plugInner(new Empty("padding"));
  }

  public void testProcessEnd() {
    ProcessDefinition processDefinition = scope.getProcessDefinition();

    // register the action we want executed at the end
    Action endAction = new Action();
    endAction.setName("endAction");
    processDefinition.addAction(endAction);
    // delegate the action's execution to a handler
    Delegation endDelegation = new Delegation();
    endDelegation.setClassName(EndHandler.class.getName());
    endAction.setActionDelegation(endDelegation);

    // register an event of type process end
    Event event = new Event(Event.EVENTTYPE_PROCESS_END);
    processDefinition.addEvent(event);
    // associate the above action with the event
    event.addAction(endAction);

    // execute the process
    executeInner();

    // verify handler execution
    assertNotNull(endDate);
  }

  public static class EndHandler implements ActionHandler {

    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext exeContext) throws Exception {
      endDate = exeContext.getProcessInstance().getEnd();
    }
  }
}
