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
package org.jbpm.bpel.integration.server;

import javax.wsdl.Definition;
import javax.xml.namespace.QName;
import javax.xml.rpc.handler.MessageContext;
import javax.xml.rpc.server.ServletEndpointContext;

import org.jboss.webservice.PortComponentInfo;
import org.jboss.webservice.deployment.ServiceDescription;
import org.jboss.webservice.server.InvokerProvider;

import org.jbpm.bpel.integration.soap.FaultFormat;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/11/12 00:45:10 $
 */
public class JBoss403EndpointMetadataLookup implements EndpointMetadataLookup {

  public EndpointMetadata lookupMetaData(MessageContext messageContext) {
    ServletEndpointContext endpointContext = (ServletEndpointContext) messageContext.getProperty("servletEndpointContext");

    org.jboss.axis.MessageContext axisMessageContext = (org.jboss.axis.MessageContext) messageContext;
    InvokerProvider invokerProvider = (InvokerProvider) axisMessageContext.getService().getPivotHandler();
    PortComponentInfo portComponentInfo = invokerProvider.getPortComponentInfo();
    QName portName = portComponentInfo.getPortComponentMetaData().getWsdlPort();

    ServiceDescription serviceDescription = portComponentInfo.getServiceDescription();
    QName serviceName = serviceDescription.getWsdlService().getQName();
    Definition wsdlDefinition = serviceDescription.getWsdlDefinition();

    EndpointMetadata endpointMetadata = new EndpointMetadata();
    endpointMetadata.setServletContext(endpointContext.getServletContext());
    endpointMetadata.setWsdlDefinition(wsdlDefinition);
    endpointMetadata.setServiceName(serviceName);
    endpointMetadata.setPortName(portName.getLocalPart());
    endpointMetadata.setFaultFormat(FaultFormat.RAW);
    return endpointMetadata;
  }
}
