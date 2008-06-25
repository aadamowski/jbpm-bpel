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

import javax.wsdl.Definition;
import javax.wsdl.Operation;
import javax.wsdl.PortType;
import javax.xml.namespace.QName;

import org.jbpm.bpel.graph.basic.Reply;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.persistence.db.AbstractDbTestCase;
import org.jbpm.bpel.variable.def.VariableDefinition;
import org.jbpm.bpel.wsdl.xml.WsdlUtil;
import org.jbpm.bpel.xml.BpelConstants;

/**
 * @author Juan Cantu
 * @version $Revision$ $Date: 2007/10/13 02:53:23 $
 */
public class ReplyActionDbTest extends AbstractDbTestCase {

  BpelProcessDefinition process;
  ReplyAction replyAction = new ReplyAction();

  protected void setUp() throws Exception {
    super.setUp();
    // activity
    Reply reply = new Reply("reply");
    reply.setAction(replyAction);
    // process, create after opening jbpm context
    process = new BpelProcessDefinition("pd", BpelConstants.NS_EXAMPLES);
    process.getGlobalScope().setActivity(reply);
  }

  public void testCorrelations() {
    // prepare persistent objects
    replyAction.setCorrelations(new Correlations());

    // save objects and load them back
    process = saveAndReload(process);
    replyAction = getReplyAction(process);

    // verify retrieved objects
    assertNotNull(replyAction.getCorrelations());
  }

  public void testOperation() {
    // prepare persistent objects
    Definition def = WsdlUtil.getSharedDefinition();

    // operation
    Operation operation = def.createOperation();
    operation.setName("o");
    // port type
    PortType portType = def.createPortType();
    portType.setQName(new QName("pt"));
    portType.addOperation(operation);
    // process
    process.getImportDefinition().addPortType(portType);
    // replier
    replyAction.setOperation(operation);

    // save objects and load them back
    process = saveAndReload(process);
    replyAction = getReplyAction(process);

    // verify the retrieved objects
    assertEquals("o", replyAction.getOperation().getName());
  }

  public void testFaultName() {
    // prepare persistent objects
    final QName FAULT_NAME = new QName("calamityFault");
    replyAction.setFaultName(FAULT_NAME);

    // prepare persistent objects
    process = saveAndReload(process);
    replyAction = getReplyAction(process);

    // verify the retrieved objects
    assertEquals(FAULT_NAME, replyAction.getFaultName());
  }

  public void testPartnerLink() {
    // prepare persistent objects
    // partner link
    PartnerLinkDefinition partnerLink = new PartnerLinkDefinition();
    partnerLink.setName("pl");
    // process
    process.getGlobalScope().addPartnerLink(partnerLink);
    // replier
    replyAction.setPartnerLink(partnerLink);

    // save objects and load them back
    process = saveAndReload(process);
    replyAction = getReplyAction(process);

    // verify the retrieved objects
    assertEquals("pl", replyAction.getPartnerLink().getName());

  }

  public void testVariable() {
    // prepare persistent objects
    // variable
    VariableDefinition variable = new VariableDefinition();
    variable.setName("v");
    // process
    process.getGlobalScope().addVariable(variable);
    // replier
    replyAction.setVariable(variable);

    // save objects and load them back
    process = saveAndReload(process);
    replyAction = getReplyAction(process);

    // verify the retrieved objects
    assertEquals("v", replyAction.getVariable().getName());
  }

  public void testMessageExchange() {
    // prepare persistent objects
    replyAction.setMessageExchange("msgExchng");

    // save objects and load them back
    process = saveAndReload(process);
    replyAction = getReplyAction(process);

    // verify the retrieved objects
    assertEquals("msgExchng", replyAction.getMessageExchange());
  }

  private ReplyAction getReplyAction(BpelProcessDefinition process) {
    Reply reply = (Reply) session.load(Reply.class, new Long(process.getGlobalScope()
        .getActivity()
        .getId()));
    return reply.getReplyAction();
  }
}
