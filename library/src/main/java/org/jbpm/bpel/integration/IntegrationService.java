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
package org.jbpm.bpel.integration;

import java.util.List;

import org.jbpm.bpel.endpointref.EndpointReference;
import org.jbpm.bpel.integration.def.InvokeAction;
import org.jbpm.bpel.integration.def.PartnerLinkDefinition;
import org.jbpm.bpel.integration.def.ReceiveAction;
import org.jbpm.bpel.integration.def.ReplyAction;
import org.jbpm.graph.exe.Token;
import org.jbpm.svc.Service;

/**
 * Contract of partner integration.
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2008/01/30 07:18:22 $
 */
public interface IntegrationService extends Service {

  public static final String SERVICE_NAME = "integration";

  public void receive(ReceiveAction receiveAction, Token token, boolean oneShot);

  public void receive(List receiveActions, Token token);

  public void cancelReception(ReceiveAction receiveAction, Token token);

  public void reply(ReplyAction replyAction, Token token);

  public void invoke(InvokeAction invokeAction, Token token);

  public void cancelInvocation(InvokeAction invokeAction, Token token);

  public EndpointReference getMyReference(PartnerLinkDefinition partnerLink, Token token);
}