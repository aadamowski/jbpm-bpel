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
package org.jbpm.bpel.endpointref.wsa;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.jbpm.bpel.endpointref.EndpointReference;
import org.jbpm.bpel.endpointref.SoapEndpointReferenceTestCase;
import org.jbpm.bpel.endpointref.wsa.WsaConstants;
import org.jbpm.bpel.endpointref.wsa.WsaEndpointReference;
import org.jbpm.bpel.xml.util.DatatypeUtil;
import org.jbpm.bpel.xml.util.XmlUtil;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/09/04 06:42:27 $
 */
public class WsaEndpointReferenceTest extends SoapEndpointReferenceTestCase {

  protected EndpointReference getReference() {
    return new WsaEndpointReference();
  }

  public void testReadEndpointRef() throws Exception {
    String text = "<EndpointReference xmlns='http://schemas.xmlsoap.org/ws/2004/08/addressing'>"
        + " <Address>http://example.com/pizzaShop/pizzas</Address>"
        + " <PortType xmlns:pt='urn:pizzas:pt'>pt:pizzasPT</PortType>"
        + " <ServiceName PortName='pizzaPort' xmlns:srv='urn:pizzas:srv'>srv:pizzaService</ServiceName>"
        + "</EndpointReference>";
    Element eprElem = XmlUtil.parseText(text);

    WsaEndpointReference reference = (WsaEndpointReference) getReference();
    reference.readEndpointRef(eprElem);

    assertEquals("http://example.com/pizzaShop/pizzas", reference.getAddress());
    assertEquals(new QName("urn:pizzas:pt", "pizzasPT"), reference.getPortTypeName());
    assertEquals(new QName("urn:pizzas:srv", "pizzaService"), reference.getServiceName());
    assertEquals("pizzaPort", reference.getPortName());
  }

  public void testWriteEndpointRef() {
    QName portTypeName = new QName("urn:pizzas:pt", "pizzasPT");
    QName serviceName = new QName("urn:pizzas:srv", "pizzaService");
    String portName = "pizzaPort";
    String address = "http://example.com/pizzaShop/pizzas";

    WsaEndpointReference reference = (WsaEndpointReference) getReference();
    reference.setAddress(address);
    reference.setPortTypeName(portTypeName);
    reference.setServiceName(serviceName);
    reference.setPortName(portName);

    Document nodeFactory = XmlUtil.createDocument();
    Element eprElem = reference.writeEndpointRef(nodeFactory);

    Element addressElem = XmlUtil.getElement(eprElem, WsaConstants.NS_ADDRESSING,
        WsaConstants.ELEM_ADDRESS);
    assertEquals(address, DatatypeUtil.toString(addressElem));

    Element portTypeElem = XmlUtil.getElement(eprElem, WsaConstants.NS_ADDRESSING,
        WsaConstants.ELEM_PORT_TYPE);
    assertEquals(portTypeName, XmlUtil.getQNameValue(portTypeElem));

    Element serviceElem = XmlUtil.getElement(eprElem, WsaConstants.NS_ADDRESSING,
        WsaConstants.ELEM_SERVICE_NAME);
    assertEquals(serviceName, XmlUtil.getQNameValue(serviceElem));

    assertEquals(portName, serviceElem.getAttribute(WsaConstants.ATTR_PORT_NAME));
  }
}
