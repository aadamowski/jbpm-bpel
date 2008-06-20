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
package org.jbpm.bpel.integration.jms;

import java.util.Iterator;

import javax.jms.JMSException;

import org.jbpm.bpel.graph.basic.Receive;
import org.jbpm.bpel.graph.def.AbstractBpelVisitor;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.struct.Pick;
import org.jbpm.bpel.graph.struct.Sequence;
import org.jbpm.bpel.integration.def.ReceiveAction;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/11/02 16:46:18 $
 */
class StartListenersBuilder extends AbstractBpelVisitor {

  private final IntegrationControl integrationControl;

  private JMSException jmsException;

  StartListenersBuilder(IntegrationControl integrationControl) throws JMSException {
    this.integrationControl = integrationControl;
  }

  public JMSException getJmsException() {
    return jmsException;
  }

  public void visit(Receive receive) {
    if (!receive.isCreateInstance() || jmsException != null)
      return;

    try {
      StartListener startListener = new StartListener(receive.getBpelProcessDefinition(),
          receive.getReceiveAction(), integrationControl);
      startListener.open();
    }
    catch (JMSException e) {
      jmsException = e;
    }
  }

  public void visit(Pick pick) {
    if (!pick.isCreateInstance() || jmsException != null)
      return;

    try {
      BpelProcessDefinition processDefinition = pick.getBpelProcessDefinition();

      for (Iterator i = pick.getOnMessages().iterator(); i.hasNext();) {
        ReceiveAction receiveAction = (ReceiveAction) i.next();
        StartListener startListener = new StartListener(processDefinition, receiveAction,
            integrationControl);
        startListener.open();
      }
    }
    catch (JMSException e) {
      jmsException = e;
    }
  }

  public void visit(Sequence sequence) {
    // visit only the first activity
    Activity activity = (Activity) sequence.getNodes().get(0);
    activity.accept(this);
  }
}
