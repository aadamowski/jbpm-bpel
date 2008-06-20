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

import org.jbpm.bpel.endpointref.SoapEndpointReference;
import org.jbpm.bpel.xml.util.XmlUtil;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/01/22 17:27:03 $
 */
public class WsdlEndpointReference extends SoapEndpointReference {

  private static final String PREFIX_WSDL = "wsdl";
  private static final String PREFIX_SOAP = "soap";

  protected void readEndpointRef(Element endpointRefElem) {
    // service name
    String serviceLocalName = endpointRefElem.getAttribute(Constants.ATTR_NAME);
    String defaultNamespace = XmlUtil.getNamespaceURI(null, endpointRefElem);
    setServiceName(new QName(defaultNamespace, serviceLocalName));
    // port name
    Element portElem = XmlUtil.getElement(endpointRefElem,
        Constants.NS_URI_WSDL, Constants.ELEM_PORT);
    if (portElem != null) {
      setPortName(portElem.getAttribute(Constants.ATTR_NAME));
      // address
      Element addressElem = XmlUtil.getElement(portElem,
          SOAPConstants.NS_URI_SOAP, SOAPConstants.ELEM_ADDRESS);
      if (addressElem != null) {
        setAddress(addressElem.getAttribute(Constants.ATTR_LOCATION));
      }
    }
  }

  protected Element writeEndpointRef(Document nodeFactory) {
    Element serviceElem = nodeFactory.createElementNS(Constants.NS_URI_WSDL,
        PREFIX_WSDL + ':' + Constants.ELEM_SERVICE);
    XmlUtil.addNamespaceDeclaration(serviceElem, Constants.NS_URI_WSDL,
        PREFIX_WSDL);
    // service name
    QName serviceName = getServiceName();
    serviceElem.setAttribute(Constants.ATTR_NAME, serviceName.getLocalPart());
    XmlUtil.addNamespaceDeclaration(serviceElem, serviceName.getNamespaceURI());
    // port name
    String portName = getPortName();
    if (portName != null) {
      Element portElem = nodeFactory.createElementNS(Constants.NS_URI_WSDL,
          PREFIX_WSDL + ':' + Constants.ELEM_PORT);
      portElem.setAttribute(Constants.ATTR_NAME, portName);
      serviceElem.appendChild(portElem);
      // address
      String address = getAddress();
      if (address != null) {
        Element addressElem = nodeFactory.createElementNS(
            SOAPConstants.NS_URI_SOAP, PREFIX_SOAP
                + ':'
                + SOAPConstants.ELEM_ADDRESS);
        XmlUtil.addNamespaceDeclaration(addressElem, SOAPConstants.NS_URI_SOAP,
            PREFIX_SOAP);
        addressElem.setAttribute(Constants.ATTR_LOCATION, address);
        portElem.appendChild(addressElem);
      }
    }
    return serviceElem;
  }
}
