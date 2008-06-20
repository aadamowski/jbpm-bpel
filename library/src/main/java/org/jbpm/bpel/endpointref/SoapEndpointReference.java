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

import java.util.Iterator;
import java.util.List;

import javax.wsdl.Binding;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.xml.namespace.QName;

import com.ibm.wsdl.extensions.soap.SOAPConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.bpel.BpelException;
import org.jbpm.bpel.integration.catalog.ServiceCatalog;
import org.jbpm.bpel.wsdl.xml.WsdlUtil;

/**
 * Captures enough detail to uniquely identify service endpoints bound to SOAP.
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/08/28 05:42:00 $
 */
public abstract class SoapEndpointReference extends EndpointReference {

  private static final Log log = LogFactory.getLog(SoapEndpointReference.class);

  protected SoapEndpointReference() {
  }

  public Port selectPort(ServiceCatalog catalog) {
    Port port;
    QName serviceName = getServiceName();
    if (serviceName == null) {
      List services = catalog.lookupServices(getPortTypeName());
      port = selectPort(services);
    }
    else {
      Service service = catalog.lookupService(serviceName);
      port = selectPort(service);
    }
    return port;
  }

  protected Port selectPort(List services) {
    QName portTypeName = getPortTypeName();
    String address = getAddress();
    Port selectedPort = null;
    // iterate the available services
    serviceLoop: for (Iterator s = services.iterator(); s.hasNext();) {
      Service service = (Service) s.next();
      // iterate the available ports
      for (Iterator p = service.getPorts().values().iterator(); p.hasNext();) {
        Port port = (Port) p.next();
        Binding binding = port.getBinding();
        // does this port implement the required type and uses a soap binding?
        if (binding.getPortType().getQName().equals(portTypeName) && isSoapBinding(binding)) {
          String portAddress = getSoapAddress(port);
          log.debug("found candidate port: name="
              + port.getName()
              + ", address="
              + portAddress
              + ", service="
              + service.getQName()
              + ", portType="
              + portTypeName);
          // does the port address match the reference address?
          if (address != null && address.equals(portAddress)) {
            // exact match, use the port and stop the search
            selectedPort = port;
            break serviceLoop;
          }
          else if (selectedPort == null) {
            // non-exact match, use the port if no other candidate exists
            selectedPort = port;
          }
        }
      }
    }
    if (selectedPort == null) {
      throw new BpelException("no port implements the required port type: "
          + "portType="
          + portTypeName);
    }
    return selectedPort;
  }

  protected Port selectPort(Service service) {
    QName serviceName = getServiceName();
    if (service == null)
      throw new BpelException("service not found: service=" + serviceName);

    QName portTypeName = getPortTypeName();
    String portName = getPortName();
    Port port;
    if (portName != null) {
      // the reference designates a specific port, go for it
      port = service.getPort(portName);
      if (port == null) {
        throw new BpelException("port not found: service=" + serviceName + ", port=" + portName);
      }
      Binding binding = port.getBinding();
      if (!portTypeName.equals(binding.getPortType().getQName())) {
        throw new BpelException("port does not implement the required port type: "
            + "service="
            + serviceName
            + ", port="
            + portName
            + ", portType="
            + portTypeName);
      }
      // does this port use a soap binding?
      if (!isSoapBinding(binding)) {
        throw new BpelException("non-soap ports not supported: "
            + "service="
            + serviceName
            + ", port="
            + portName
            + ", portType="
            + portTypeName);
      }
    }
    else {
      port = null;
      String address = getAddress();
      // iterate the available ports
      for (Iterator i = service.getPorts().values().iterator(); i.hasNext();) {
        Port aPort = (Port) i.next();
        Binding binding = aPort.getBinding();
        // does this port implement the required port type and is bound to soap?
        if (binding.getPortType().getQName().equals(portTypeName) && isSoapBinding(binding)) {
          // does the port's address match the referenced address?
          if (address != null && address.equals(getSoapAddress(aPort))) {
            // exact match, use the port and stop the search
            port = aPort;
            break;
          }
          else if (port == null) {
            // non-exact match, use the port if no other candidate exists
            port = aPort;
          }
        }
      }
      if (port == null) {
        throw new BpelException("no port implements the required port type"
            + " and is bound to soap: service="
            + serviceName
            + ", portType="
            + portTypeName);
      }
    }
    return port;
  }

  private static boolean isSoapBinding(Binding binding) {
    return WsdlUtil.getExtension(binding.getExtensibilityElements(),
        SOAPConstants.Q_ELEM_SOAP_BINDING) != null;
  }

  static String getSoapAddress(Port port) {
    SOAPAddress soapAddress = (SOAPAddress) WsdlUtil.getExtension(port.getExtensibilityElements(),
        SOAPConstants.Q_ELEM_SOAP_ADDRESS);
    return soapAddress != null ? soapAddress.getLocationURI() : null;
  }
}
