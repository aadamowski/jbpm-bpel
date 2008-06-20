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
import javax.xml.namespace.QName;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ibm.wsdl.util.xml.QNameUtils;

import org.jbpm.bpel.graph.exe.BpelFaultException;
import org.jbpm.bpel.integration.catalog.ServiceCatalog;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.bpel.xml.util.XmlUtil;

/**
 * Endpoint references are the means for dynamic communication of port-specific data for services.
 * This class represents the least common denominator of data needed to identify a service endpoint.
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/08/28 05:42:00 $
 */
public abstract class EndpointReference {

  long id;

  private String scheme;
  private QName portTypeName;
  private QName serviceName;
  private String portName;
  private String address;

  protected EndpointReference() {
  }

  public String getScheme() {
    return scheme;
  }

  public void setScheme(String scheme) {
    this.scheme = scheme;
  }

  public QName getPortTypeName() {
    return portTypeName;
  }

  public void setPortTypeName(QName portTypeName) {
    this.portTypeName = portTypeName;
  }

  public QName getServiceName() {
    return serviceName;
  }

  public void setServiceName(QName serviceName) {
    this.serviceName = serviceName;
  }

  public String getPortName() {
    return portName;
  }

  public void setPortName(String portName) {
    this.portName = portName;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public static EndpointReference readServiceRef(Element referenceElem) {
    String scheme;
    Element endpointRefElem;
    // is the given element a service reference container?
    if (BpelConstants.NS_SERVICE_REF.equals(referenceElem.getNamespaceURI())
        && BpelConstants.ELEM_SERVICE_REF.equals(referenceElem.getLocalName())) {
      // read element following the schema of bpel:service-ref
      scheme = referenceElem.getAttribute(BpelConstants.ATTR_REFERENCE_SCHEME);
      endpointRefElem = XmlUtil.getElement(referenceElem);
    }
    else {
      // assume the given element is the actual endpoint reference value
      scheme = null;
      endpointRefElem = referenceElem;
    }
    // locate a factory that understands this reference
    QName endpointRefName = QNameUtils.newQName(endpointRefElem);
    EndpointReferenceFactory factory = EndpointReferenceFactory.getInstance(endpointRefName, scheme);
    if (factory == null)
      throw new BpelFaultException(BpelConstants.FAULT_UNSUPPORTED_REFERENCE);
    // produce the endpoint reference
    EndpointReference endpointRef = factory.createEndpointReference();
    endpointRef.setScheme(scheme);
    endpointRef.readEndpointRef(endpointRefElem);
    return endpointRef;
  }

  public void writeServiceRef(Element referenceElem) {
    // write the endpoint reference value
    Element endpointRefElem = writeEndpointRef(referenceElem.getOwnerDocument());
    // is the given element a service reference container?
    if (BpelConstants.NS_SERVICE_REF.equals(referenceElem.getNamespaceURI())
        && BpelConstants.ELEM_SERVICE_REF.equals(referenceElem.getLocalName())) {
      // clean the container element
      XmlUtil.removeAttributes(referenceElem);
      XmlUtil.removeChildNodes(referenceElem);
      // set reference scheme attribute
      if (scheme == null || scheme.length() == 0)
        referenceElem.setAttribute(BpelConstants.ATTR_REFERENCE_SCHEME, scheme);
      // add endpoint reference child element
      referenceElem.appendChild(endpointRefElem);
    }
    else {
      // copy the reference value directly to the given element
      XmlUtil.copy(referenceElem, endpointRefElem);
    }
  }

  public abstract Port selectPort(ServiceCatalog catalog);

  protected abstract void readEndpointRef(Element endpointRefElem);

  protected abstract Element writeEndpointRef(Document nodeFactory);

  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    // address; required
    builder.append("address", address);
    // scheme
    String scheme = getScheme();
    if (scheme != null)
      builder.append("scheme", scheme);
    // port type
    QName portTypeName = getPortTypeName();
    if (portTypeName != null)
      builder.append("portType", portTypeName);
    // service
    QName serviceName = getServiceName();
    if (serviceName != null) {
      builder.append("service", serviceName);
      // port; meaningful only with a service name
      String portName = getPortName();
      if (portName != null)
        builder.append("port", portName);
    }
    return builder.toString();
  }
}
