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
package org.jbpm.bpel.wsdl.xml;

import javax.wsdl.Definition;
import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.jbpm.bpel.wsdl.PartnerLinkType;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/06/09 23:36:38 $
 */
public class PartnerLinkTypeTest extends TestCase {

  private Definition definition;

  private static final String NS_POS = "http://manufacturing.org/wsdl/purchase";

  protected void setUp() throws Exception {
    definition = WsdlUtil.readResource(getClass(), "partnerLinkTypeSample.wsdl");
  }

  public void testGetPartnerLinkType() {
    String tns = definition.getTargetNamespace();
    PartnerLinkType plinkType = WsdlUtil.getPartnerLinkType(definition,
        new QName(tns, "purchasingLT"));
    assertNotNull(plinkType);
    assertEquals("purchaseService", plinkType.getFirstRole().getName());

    plinkType = WsdlUtil.getPartnerLinkType(definition, new QName(tns,
        "shippingLT"));
    assertNotNull(plinkType);
    assertEquals("shippingService", plinkType.getFirstRole().getName());
  }

  public void testUnmarshall() {
    String tns = definition.getTargetNamespace();
    // purchasing partner link
    PartnerLinkType plinkType = WsdlUtil.getPartnerLinkType(definition,
        new QName(tns, "purchasingLT"));
    assertNotNull(plinkType);
    // First role
    PartnerLinkType.Role role = plinkType.getFirstRole();
    assertEquals("purchaseService", role.getName());
    assertEquals(new QName(NS_POS, "purchaseOrderPT"), role.getPortType()
        .getQName());
    // Second role
    assertNull(plinkType.getSecondRole());

    // invoicing partner link
    plinkType = WsdlUtil.getPartnerLinkType(definition, new QName(tns,
        "invoicingLT"));
    assertNotNull(plinkType);
    // First role
    role = plinkType.getFirstRole();
    assertEquals("invoiceService", role.getName());
    assertEquals(new QName(NS_POS, "computePricePT"), role.getPortType()
        .getQName());
    // Second role
    role = plinkType.getSecondRole();
    assertEquals("invoiceRequester", role.getName());
    assertEquals(new QName(NS_POS, "invoiceCallbackPT"), role.getPortType()
        .getQName());
  }

  public void testMarshall() throws Exception {
    definition = WsdlUtil.writeAndRead(definition);
    // test the definition works the same
    testUnmarshall();
  }
}
