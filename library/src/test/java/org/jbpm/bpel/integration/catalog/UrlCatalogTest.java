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
package org.jbpm.bpel.integration.catalog;

import java.util.Iterator;

import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.xml.namespace.QName;

import org.jbpm.bpel.integration.catalog.UrlCatalog;

import junit.framework.TestCase;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/10/13 02:53:23 $
 */
public class UrlCatalogTest extends TestCase {

  private UrlCatalog catalog = new UrlCatalog();

  public UrlCatalogTest(String name) {
    super(name);
  }

  protected void setUp() throws Exception {
    catalog.addLocation(getResource("atm.wsdl"));
    catalog.addLocation(getResource("translator.wsdl"));
  }

  public void testLookupService() {
    QName serviceName = new QName("urn:samples:ATMService", "atmService");
    Service service = catalog.lookupService(serviceName);

    assertEquals(serviceName, service.getQName());
  }

  public void testLookupServices() {
    QName portTypeName = new QName("http://example.com/translator",
        "textTranslator");
    Iterator serviceIt = catalog.lookupServices(portTypeName).iterator();
    while (serviceIt.hasNext()) {
      Service service = (Service) serviceIt.next();
      assertTrue(implementsPortType(service, portTypeName));
    }
  }

  private static boolean implementsPortType(Service service, QName portTypeName) {
    Iterator portIt = service.getPorts().values().iterator();
    while (portIt.hasNext()) {
      Port port = (Port) portIt.next();
      if (portTypeName.equals(port.getBinding().getPortType().getQName()))
        return true;
    }
    return false;
  }

  private static String getResource(String name) {
    return UrlCatalogTest.class.getResource(name).toString();
  }
}
