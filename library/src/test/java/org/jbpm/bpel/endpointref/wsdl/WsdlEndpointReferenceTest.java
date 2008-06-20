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
package org.jbpm.bpel.endpointref.wsdl;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ibm.wsdl.Constants;
import com.ibm.wsdl.extensions.soap.SOAPConstants;

import org.jbpm.bpel.endpointref.EndpointReference;
import org.jbpm.bpel.endpointref.SoapEndpointReferenceTestCase;
import org.jbpm.bpel.endpointref.wsdl.WsdlEndpointReference;
import org.jbpm.bpel.xml.util.XmlUtil;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/08/28 05:41:59 $
 */
public class WsdlEndpointReferenceTest extends SoapEndpointReferenceTestCase {

  protected EndpointReference getReference() {
    return new WsdlEndpointReference();
  }

  public void testReadEndpointRef() throws Exception {
    String text = "<wsdl:service name='pizzaService' xmlns='urn:pizzas:srv'"
        + " xmlns:wsdl='http://schemas.xmlsoap.org/wsdl/'>"
        + " <wsdl:port name='pizzaPort'>"
        + "  <soap:address location='http://example.com/pizzaShop/pizzas'"
        + "   xmlns:soap='http://schemas.xmlsoap.org/wsdl/soap/' />"
        + " </wsdl:port>"
        + "</wsdl:service>";
    Element eprElem = XmlUtil.parseText(text);

    WsdlEndpointReference reference = (WsdlEndpointReference) getReference();
    reference.readEndpointRef(eprElem);

    assertEquals(new QName("urn:pizzas:srv", "pizzaService"),
        reference.getServiceName());
    assertEquals("pizzaPort", reference.getPortName());
    assertEquals("http://example.com/pizzaShop/pizzas", reference.getAddress());
  }

  public void testWriteEndpointRef() {
    QName serviceName = new QName("urn:pizzas:srv", "pizzaService");
    String portName = "pizzaPort";
    String address = "http://example.com/pizzaShop/pizzas";

    WsdlEndpointReference reference = (WsdlEndpointReference) getReference();
    reference.setServiceName(serviceName);
    reference.setPortName(portName);
    reference.setAddress(address);

    Document nodeFactory = XmlUtil.createDocument();
    Element eprElem = reference.writeEndpointRef(nodeFactory);

    assertEquals(serviceName.getLocalPart(),
        eprElem.getAttribute(Constants.ATTR_NAME));
    assertEquals(serviceName.getNamespaceURI(), eprElem.getAttribute("xmlns"));

    Element portElem = XmlUtil.getElement(eprElem, Constants.NS_URI_WSDL,
        Constants.ELEM_PORT);
    assertEquals(portName, portElem.getAttribute(Constants.ATTR_NAME));

    Element addressElem = XmlUtil.getElement(portElem,
        SOAPConstants.NS_URI_SOAP, SOAPConstants.ELEM_ADDRESS);
    assertEquals(address, addressElem.getAttribute(Constants.ATTR_LOCATION));
  }
}
