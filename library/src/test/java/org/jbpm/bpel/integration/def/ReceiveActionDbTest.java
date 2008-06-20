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

import org.jbpm.bpel.graph.basic.Receive;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.persistence.db.AbstractDbTestCase;
import org.jbpm.bpel.variable.def.VariableDefinition;
import org.jbpm.bpel.wsdl.xml.WsdlUtil;
import org.jbpm.bpel.xml.BpelConstants;

/**
 * @author Juan Cantu
 * @version $Revision$ $Date: 2007/10/13 02:53:23 $
 */
public class ReceiveActionDbTest extends AbstractDbTestCase {

  BpelProcessDefinition process;
  ReceiveAction receiveAction = new ReceiveAction();

  static final String RECEIVE_NAME = "r";

  protected void setUp() throws Exception {
    super.setUp();
    // activity
    Receive receive = new Receive(RECEIVE_NAME);
    receive.setAction(receiveAction);
    // process, create after opening jbpm context
    process = new BpelProcessDefinition("pd", BpelConstants.NS_EXAMPLES);
    process.getGlobalScope().setActivity(receive);
  }

  public void testCorrelations() {
    // prepare persistent objects
    receiveAction.setCorrelations(new Correlations());

    // save objects and load them back
    process = saveAndReload(process);
    receiveAction = getReceiveAction(process);

    // verify retrieved objects
    assertNotNull(receiveAction.getCorrelations());
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
    // receiver
    receiveAction.setOperation(operation);

    // save objects and load them back
    process = saveAndReload(process);
    receiveAction = getReceiveAction(process);

    // verify the retrieved objects
    assertEquals("o", receiveAction.getOperation().getName());
  }

  public void testPartnerLink() {
    // prepare persistent objects
    // partner link
    PartnerLinkDefinition partnerLink = new PartnerLinkDefinition();
    partnerLink.setName("pl");
    // process
    process.getGlobalScope().addPartnerLink(partnerLink);
    // receiver
    receiveAction.setPartnerLink(partnerLink);

    // save objects and load them back
    process = saveAndReload(process);
    receiveAction = getReceiveAction(process);

    // verify the retrieved objects
    assertEquals("pl", receiveAction.getPartnerLink().getName());
  }

  public void testVariable() {
    // prepare persistent objects
    // variable
    VariableDefinition variable = new VariableDefinition();
    variable.setName("v");
    // process
    process.getGlobalScope().addVariable(variable);
    // receiver
    receiveAction.setVariable(variable);

    // save objects and load them back
    process = saveAndReload(process);
    receiveAction = getReceiveAction(process);

    // verify the retrieved objects
    assertEquals("v", receiveAction.getVariable().getName());
  }

  public void testInboundMessageListener() {
    // save objects and load them back
    process = saveAndReload(process);
    receiveAction = getReceiveAction(process);

    // verify the retrieved objects
    assertEquals(process.getGlobalScope().getActivity(), receiveAction.getInboundMessageActivity());
  }

  public void testMessageExchange() {
    // prepare persistent objects
    receiveAction.setMessageExchange("msgExchng");

    // save objects and load them back
    process = saveAndReload(process);
    receiveAction = getReceiveAction(process);

    // verify the retrieved objects
    assertEquals("msgExchng", receiveAction.getMessageExchange());
  }

  private ReceiveAction getReceiveAction(BpelProcessDefinition process) {
    Receive receive = (Receive) session.load(Receive.class, new Long(process.getGlobalScope()
        .getActivity()
        .getId()));
    return receive.getReceiveAction();
  }
}
