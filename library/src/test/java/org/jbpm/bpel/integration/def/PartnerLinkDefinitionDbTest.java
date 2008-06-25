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
import javax.wsdl.PortType;
import javax.xml.namespace.QName;

import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.persistence.db.AbstractDbTestCase;
import org.jbpm.bpel.wsdl.PartnerLinkType;
import org.jbpm.bpel.wsdl.PartnerLinkType.Role;
import org.jbpm.bpel.wsdl.xml.WsdlConstants;
import org.jbpm.bpel.wsdl.xml.WsdlUtil;
import org.jbpm.bpel.xml.BpelConstants;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/11/02 16:46:19 $
 */
public class PartnerLinkDefinitionDbTest extends AbstractDbTestCase {

  private BpelProcessDefinition processDefinition;
  private PartnerLinkType partnerLinkType;
  private PartnerLinkDefinition partnerLink;

  private static final QName TYPE_NAME = new QName(BpelConstants.NS_EXAMPLES, "plt");
  private static final String PARTNER_LINK_NAME = "pl";
  private static final String FIRST_ROLE_NAME = "first";
  private static final String SECOND_ROLE_NAME = "second";

  protected void setUp() throws Exception {
    super.setUp();
    // port type
    PortType portType = WsdlUtil.getSharedDefinition().createPortType();
    portType.setQName(new QName(BpelConstants.NS_EXAMPLES, "pt"));
    // partner link type
    partnerLinkType = (PartnerLinkType) WsdlUtil.getSharedExtensionRegistry().createExtension(
        Definition.class, WsdlConstants.Q_PARTNER_LINK_TYPE);
    partnerLinkType.setQName(TYPE_NAME);
    // first role
    Role role = partnerLinkType.createRole();
    role.setName(FIRST_ROLE_NAME);
    role.setPortType(portType);
    partnerLinkType.setFirstRole(role);
    // second role
    role = partnerLinkType.createRole();
    role.setName(SECOND_ROLE_NAME);
    role.setPortType(portType);
    partnerLinkType.setSecondRole(role);
    // process, create after opening jbpm context
    processDefinition = new BpelProcessDefinition("pd", BpelConstants.NS_EXAMPLES);
    processDefinition.getImportDefinition().addPartnerLinkType(partnerLinkType);
    // partner link
    partnerLink = new PartnerLinkDefinition();
    partnerLink.setName(PARTNER_LINK_NAME);
    partnerLink.setPartnerLinkType(partnerLinkType);
    processDefinition.getGlobalScope().addPartnerLink(partnerLink);
  }

  public void testName() {
    processDefinition = saveAndReload(processDefinition);
    partnerLink = processDefinition.getGlobalScope().getPartnerLink(PARTNER_LINK_NAME);

    assertEquals(PARTNER_LINK_NAME, partnerLink.getName());
  }

  public void testPartnerLinkType() {
    processDefinition = saveAndReload(processDefinition);
    partnerLink = processDefinition.getGlobalScope().getPartnerLink(PARTNER_LINK_NAME);
    partnerLinkType = partnerLink.getPartnerLinkType();

    assertEquals(TYPE_NAME, partnerLinkType.getQName());
  }

  public void testPartnerRole() {
    // partner link
    partnerLink.setPartnerRole(partnerLinkType.getSecondRole());

    processDefinition = saveAndReload(processDefinition);
    partnerLink = processDefinition.getGlobalScope().getPartnerLink(PARTNER_LINK_NAME);

    assertEquals(SECOND_ROLE_NAME, partnerLink.getPartnerRole().getName());
  }

  public void testMyRole() {
    partnerLink.setMyRole(partnerLinkType.getFirstRole());

    processDefinition = saveAndReload(processDefinition);
    partnerLink = processDefinition.getGlobalScope().getPartnerLink(PARTNER_LINK_NAME);

    assertEquals(FIRST_ROLE_NAME, partnerLink.getMyRole().getName());
  }
}
