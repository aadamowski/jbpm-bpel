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

import org.jbpm.bpel.wsdl.Property;
import org.jbpm.bpel.xml.BpelConstants;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/06/09 23:36:38 $
 */
public class PropertyTest extends TestCase {

  private Definition definition;

  protected void setUp() throws Exception {
    definition = WsdlUtil.readResource(getClass(), "propertySample.wsdl");
  }

  public void testGetProperty() {
    String tns = definition.getTargetNamespace();
    Property property = WsdlUtil.getProperty(definition, new QName(tns,
        "customerID"));
    assertNotNull(property);
    assertEquals(new QName(BpelConstants.NS_XML_SCHEMA, "string"),
        property.getType());

    property = WsdlUtil.getProperty(definition, new QName(tns, "invoiceNumber"));
    assertNotNull(property);
    assertEquals(new QName(BpelConstants.NS_XML_SCHEMA, "int"),
        property.getType());
  }

  public void testUnmarshall() {
    // find properties within extensibility elements
    String tns = definition.getTargetNamespace();
    // First property
    Property property = WsdlUtil.getProperty(definition, new QName(tns,
        "customerID"));
    assertNotNull(property);
    assertEquals(new QName(BpelConstants.NS_XML_SCHEMA, "string"),
        property.getType());
    // Second property
    property = WsdlUtil.getProperty(definition, new QName(tns, "orderNumber"));
    assertNotNull(property);
    assertEquals(new QName(BpelConstants.NS_XML_SCHEMA, "int"),
        property.getType());
  }

  public void testMarshall() throws Exception {
    definition = WsdlUtil.writeAndRead(definition);
    // test things work the same
    testUnmarshall();
  }
}
