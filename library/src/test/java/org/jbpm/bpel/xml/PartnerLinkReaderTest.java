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
package org.jbpm.bpel.xml;

import org.w3c.dom.Element;

import org.jbpm.bpel.integration.def.PartnerLinkDefinition;
import org.jbpm.bpel.wsdl.PartnerLinkType.Role;

/**
 * @author Juan Cantu
 * @version $Revision$ $Date: 2007/10/13 02:53:24 $
 */
public class PartnerLinkReaderTest extends AbstractReaderTestCase {

  protected void setUp() throws Exception {
    super.setUp();
    initMessageProperties();
  }

  public void testMyRole() throws Exception {
    String xml = "<partnerLinks xmlns:tns='http://manufacturing.org/wsdl/purchase'>"
        + " <partnerLink name='pl' partnerLinkType='tns:aPartnerLinkType'"
        + "  myRole='role2' partnerRole='role1'/>"
        + "</partnerLinks>";
    PartnerLinkDefinition pl = parsePartnerLink(xml);
    Role myRole = pl.getMyRole();
    assertNotNull(myRole);
    assertEquals("role2", myRole.getName());
    assertSame(myPortType, myRole.getPortType());
  }

  public void testPartnerRole() throws Exception {
    String xml = "<partnerLinks xmlns:tns='http://manufacturing.org/wsdl/purchase'>"
        + " <partnerLink name='pl' partnerLinkType='tns:aPartnerLinkType'"
        + "  myRole='role2' partnerRole='role1'/>"
        + "</partnerLinks>";
    PartnerLinkDefinition pl = parsePartnerLink(xml);
    Role partnerRole = pl.getPartnerRole();
    assertNotNull(partnerRole);
    assertEquals("role1", partnerRole.getName());
    assertSame(partnerPortType, partnerRole.getPortType());
  }

  private PartnerLinkDefinition parsePartnerLink(String xml) throws Exception {
    Element element = parseAsBpelElement(xml);
    return (PartnerLinkDefinition) reader.readPartnerLinks(element,
        processDefinition.getGlobalScope()).get("pl");
  }
}
