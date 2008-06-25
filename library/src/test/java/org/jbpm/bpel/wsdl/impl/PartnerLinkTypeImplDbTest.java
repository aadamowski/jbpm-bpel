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
package org.jbpm.bpel.wsdl.impl;

import javax.wsdl.PortType;
import javax.xml.namespace.QName;

import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.persistence.db.AbstractDbTestCase;
import org.jbpm.bpel.wsdl.PartnerLinkType;
import org.jbpm.bpel.wsdl.xml.WsdlUtil;
import org.jbpm.bpel.xml.BpelConstants;

/**
 * @author Juan Cantu
 * @version $Revision$ $Date: 2007/11/02 16:46:20 $
 */
public class PartnerLinkTypeImplDbTest extends AbstractDbTestCase {

  private BpelProcessDefinition processDefinition;
  private PartnerLinkType partnerLinkType;

  private static final QName TYPE_NAME = new QName("plt");

  protected void setUp() throws Exception {
    super.setUp();
    // port type
    PortType portType = WsdlUtil.getSharedDefinition().createPortType();
    portType.setQName(new QName(BpelConstants.NS_EXAMPLES, "pt"));
    // partner link type
    partnerLinkType = new PartnerLinkTypeImpl();
    partnerLinkType.setQName(TYPE_NAME);
    // first role
    PartnerLinkType.Role role = new PartnerLinkTypeImpl.RoleImpl();
    role.setName("first");
    role.setPortType(portType);
    partnerLinkType.setFirstRole(role);
    // second role
    role = new PartnerLinkTypeImpl.RoleImpl();
    role.setName("second");
    role.setPortType(portType);
    partnerLinkType.setSecondRole(role);
    // process definition
    processDefinition = new BpelProcessDefinition("pd", BpelConstants.NS_EXAMPLES);
    processDefinition.getImportDefinition().addPartnerLinkType(partnerLinkType);
  }

  public void testQName() {
    processDefinition = saveAndReload(processDefinition);
    partnerLinkType = processDefinition.getImportDefinition().getPartnerLinkType(TYPE_NAME);

    assertEquals(TYPE_NAME, partnerLinkType.getQName());
  }

  public void testFirstRole() {
    processDefinition = saveAndReload(processDefinition);
    partnerLinkType = processDefinition.getImportDefinition().getPartnerLinkType(TYPE_NAME);

    assertEquals("first", partnerLinkType.getFirstRole().getName());
  }

  public void testSecondRole() {
    processDefinition = saveAndReload(processDefinition);
    partnerLinkType = processDefinition.getImportDefinition().getPartnerLinkType(TYPE_NAME);

    assertEquals("second", partnerLinkType.getSecondRole().getName());
  }
}
