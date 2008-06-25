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

import java.io.Serializable;

import javax.xml.namespace.QName;

import org.jbpm.bpel.endpointref.EndpointReference;
import org.jbpm.bpel.persistence.db.AbstractDbTestCase;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/08/28 05:42:00 $
 */
public abstract class EndpointReferenceDbTestCase extends AbstractDbTestCase {

  public void testScheme() {
    EndpointReference reference = createReference();
    String scheme = "http://schemas.xmlsoap.org/ws/2004/08/addressing";
    reference.setScheme(scheme);
    reference = saveAndReload(reference);

    assertEquals(scheme, reference.getScheme());
  }

  public void testPortTypeName() {
    EndpointReference reference = createReference();
    QName portTypeName = new QName("urn:pizzas:pt", "pizzasPT");
    reference.setPortTypeName(portTypeName);
    reference = saveAndReload(reference);

    assertEquals(portTypeName, reference.getPortTypeName());
  }

  public void testServiceName() {
    EndpointReference reference = createReference();
    QName serviceName = new QName("urn:pizzas:srv", "pizzaService");
    reference.setServiceName(serviceName);
    reference = saveAndReload(reference);

    assertEquals(serviceName, reference.getServiceName());
  }

  public void testPortName() {
    EndpointReference reference = createReference();
    String portName = "pizzaPort";
    reference.setPortName(portName);
    reference = saveAndReload(reference);

    assertEquals(portName, reference.getPortName());
  }

  public void testAddress() {
    EndpointReference reference = createReference();
    String address = "http://example.com/pizzaShop/pizzas";
    reference.setAddress(address);
    reference = saveAndReload(reference);

    assertEquals(address, reference.getAddress());
  }

  protected abstract EndpointReference createReference();

  protected EndpointReference saveAndReload(EndpointReference reference) {
    Serializable id = session.save(reference);
    newTransaction();
    return (EndpointReference) session.load(reference.getClass(), id);
  }
}
