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
package org.jbpm.bpel.graph.scope;

import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.scope.OnEvent;
import org.jbpm.bpel.graph.scope.Handler;
import org.jbpm.bpel.integration.def.PartnerLinkDefinition;
import org.jbpm.bpel.integration.def.ReceiveAction;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/03/16 00:04:38 $
 */
public class OnEventDbTest extends AbstractHandlerDbTestCase {

  protected Handler createHandler(BpelProcessDefinition process) {
    OnEvent onEvent = new OnEvent();
    process.getGlobalScope().addOnEvent(onEvent);
    return onEvent;
  }

  protected Handler getHandler(BpelProcessDefinition process) {
    return (OnEvent) process.getGlobalScope().getOnEvents().iterator().next();
  }

  public void testReceiver() {
    // prepare persistent objects
    BpelProcessDefinition process = handler.getBpelProcessDefinition();
    // partner link
    String partnerLinkName = "partnerLink";
    PartnerLinkDefinition partnerLink = new PartnerLinkDefinition();
    partnerLink.setName(partnerLinkName);
    process.getGlobalScope().addPartnerLink(partnerLink);
    // receiver
    ReceiveAction receiveAction = new ReceiveAction();
    receiveAction.setPartnerLink(partnerLink);
    // handler
    OnEvent onEvent = (OnEvent) handler;
    onEvent.setAction(receiveAction);

    // save objects and load them back
    process = saveAndReload(process);
    onEvent = (OnEvent) getHandler(process);

    // verify retrieved objects
    assertEquals(partnerLinkName, onEvent.getReceiveAction()
        .getPartnerLink()
        .getName());
  }
}
