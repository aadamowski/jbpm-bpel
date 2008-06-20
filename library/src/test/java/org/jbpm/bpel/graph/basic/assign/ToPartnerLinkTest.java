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
package org.jbpm.bpel.graph.basic.assign;

import javax.wsdl.Definition;
import javax.wsdl.PortType;
import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.jbpm.bpel.endpointref.EndpointReference;
import org.jbpm.bpel.endpointref.wsa.WsaEndpointReference;
import org.jbpm.bpel.graph.basic.Assign;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.scope.Scope;
import org.jbpm.bpel.integration.def.PartnerLinkDefinition;
import org.jbpm.bpel.wsdl.PartnerLinkType;
import org.jbpm.bpel.wsdl.PartnerLinkType.Role;
import org.jbpm.bpel.wsdl.xml.WsdlConstants;
import org.jbpm.bpel.wsdl.xml.WsdlUtil;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2008/02/04 14:35:48 $
 */
public class ToPartnerLinkTest extends TestCase {

  private ToPartnerLink to = new ToPartnerLink();
  private Token token;

  protected void setUp() throws Exception {
    // port type 1
    Definition sharedDefinition = WsdlUtil.getSharedDefinition();
    PortType portType1 = sharedDefinition.createPortType();
    portType1.setQName(new QName("pt1"));

    // port type 2
    PortType portType2 = sharedDefinition.createPortType();
    portType2.setQName(new QName("pt2"));

    // partner link type
    PartnerLinkType partnerLinkType = (PartnerLinkType) WsdlUtil.getSharedExtensionRegistry()
        .createExtension(Definition.class, WsdlConstants.Q_PARTNER_LINK_TYPE);
    partnerLinkType.setQName(new QName("plt"));

    // role 1
    Role role1 = partnerLinkType.createRole();
    role1.setName("r1");
    role1.setPortType(portType1);
    partnerLinkType.setFirstRole(role1);

    // role 2
    Role role2 = partnerLinkType.createRole();
    role2.setName("r2");
    role2.setPortType(portType2);
    partnerLinkType.setSecondRole(role2);

    // partner link
    PartnerLinkDefinition partnerLink = new PartnerLinkDefinition();
    partnerLink.setName("pl1");
    partnerLink.setPartnerLinkType(partnerLinkType);
    partnerLink.setMyRole(role1);
    partnerLink.setPartnerRole(role2);

    // to
    to.setPartnerLink(partnerLink);

    // copy
    Copy copy = new Copy();
    copy.setTo(to);

    // assign
    Assign assign = new Assign("main");
    assign.addOperation(copy);

    // process definition
    BpelProcessDefinition processDefinition = new BpelProcessDefinition("pd",
        BpelConstants.NS_EXAMPLES);

    // global scope
    Scope globalScope = processDefinition.getGlobalScope();
    globalScope.addPartnerLink(partnerLink);
    globalScope.setActivity(assign);

    // instantiate process
    token = new ProcessInstance(processDefinition).getRootToken();

    // initialize global scope
    globalScope.createInstance(token).initializeData();
  }

  public void testAssign() {
    // endpoint ref
    EndpointReference endpointRef = new WsaEndpointReference();

    to.assign(token, endpointRef);
    assertSame(endpointRef, to.getPartnerLink().getInstance(token).getPartnerReference());
  }

}
