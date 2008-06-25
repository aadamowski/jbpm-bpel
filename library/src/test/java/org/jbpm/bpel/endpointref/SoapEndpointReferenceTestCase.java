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
package org.jbpm.bpel.endpointref;

import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.jbpm.bpel.endpointref.SoapEndpointReference;
import org.jbpm.bpel.integration.catalog.ServiceCatalog;
import org.jbpm.bpel.integration.catalog.UrlCatalog;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/10/13 02:53:27 $
 */
public abstract class SoapEndpointReferenceTestCase extends TestCase {

  private ServiceCatalog catalog;

  protected void setUp() throws Exception {
    UrlCatalog urlCatalog = new UrlCatalog();
    urlCatalog.addLocation(getResource("atm.wsdl"));
    urlCatalog.addLocation(getResource("translator.wsdl"));
    this.catalog = urlCatalog;
  }

  public void testSelectPort_portType() {
    QName portTypeName = new QName("http://example.com/translator",
        "textTranslator");

    EndpointReference reference = getReference();
    reference.setPortTypeName(portTypeName);
    Port port = reference.selectPort(catalog);

    assertEquals(portTypeName, port.getBinding().getPortType().getQName());
  }

  public void testSelectPort_portType_address() {
    QName portTypeName = new QName("http://example.com/translator",
        "textTranslator");
    String address = "http://mirror.example.com/translator/text";

    EndpointReference reference = getReference();
    reference.setPortTypeName(portTypeName);
    reference.setAddress(address);
    Port port = reference.selectPort(catalog);

    assertEquals(portTypeName, port.getBinding().getPortType().getQName());
    assertEquals(address, SoapEndpointReference.getSoapAddress(port));
  }

  public void testSelectPort_service() {
    QName portTypeName = new QName("urn:samples:ATMService", "atm");
    QName serviceName = new QName("urn:samples:ATMService", "atmService");

    EndpointReference reference = getReference();
    reference.setPortTypeName(portTypeName);
    reference.setServiceName(serviceName);
    Port port = reference.selectPort(catalog);

    assertEquals(portTypeName, port.getBinding().getPortType().getQName());
    assertTrue(hasPort(serviceName, port));
  }

  public void testSelectPort_service_address() {
    QName portTypeName = new QName("http://example.com/translator",
        "textTranslator");
    QName serviceName = new QName("http://example.com/translator",
        "translatorServiceMirror");
    String address = "http://mirror.example.com/translator/text";

    EndpointReference reference = getReference();
    reference.setPortTypeName(portTypeName);
    reference.setServiceName(serviceName);
    reference.setAddress(address);
    Port port = reference.selectPort(catalog);

    assertEquals(portTypeName, port.getBinding().getPortType().getQName());
    assertTrue(hasPort(serviceName, port));
    assertEquals(address, SoapEndpointReference.getSoapAddress(port));
  }

  public void testSelectPort_service_port() {
    QName portTypeName = new QName("http://example.com/translator",
        "documentTranslator");
    QName serviceName = new QName("http://example.com/translator",
        "translatorService");
    String portName = "documentTranslatorPort";

    EndpointReference reference = getReference();
    reference.setPortTypeName(portTypeName);
    reference.setServiceName(serviceName);
    reference.setPortName(portName);
    Port port = reference.selectPort(catalog);

    assertEquals(portTypeName, port.getBinding().getPortType().getQName());
    assertTrue(hasPort(serviceName, port));
    assertEquals(portName, port.getName());
  }

  protected abstract EndpointReference getReference();

  private boolean hasPort(QName serviceName, Port port) {
    Service service = catalog.lookupService(serviceName);
    return service.getPorts().containsKey(port.getName());
  }

  private static String getResource(String name) {
    return SoapEndpointReferenceTestCase.class.getResource(name).toString();
  }
}
