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

import org.jboss.ws.metadata.EndpointMetaData;
import org.jboss.ws.metadata.ServiceMetaData;
import org.jboss.ws.soap.SOAPMessageContextImpl;

import org.jbpm.bpel.integration.soap.FaultFormat;

/**
 * The JBossWS 1.0 endpoint metadata lookup queries the
 * {@linkplain EndpointMetaData endpoint} and
 * {@linkplain ServiceMetaData service} metadata referenced from the message
 * context.
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/11/12 00:45:09 $
 */
public class JBoss405EndpointMetadataLookup implements EndpointMetadataLookup {

  public EndpointMetadata lookupMetaData(MessageContext messageContext) {
    ServletEndpointContext endpointContext = (ServletEndpointContext) messageContext.getProperty("javax.xml.ws.servlet.context");

    SOAPMessageContextImpl jbwsMessageContext = (SOAPMessageContextImpl) messageContext;
    EndpointMetaData jbwsEndpointMetadata = jbwsMessageContext.getEndpointMetaData();
    QName portName = jbwsEndpointMetadata.getName();

    ServiceMetaData jbwsServiceMetadata = jbwsEndpointMetadata.getServiceMetaData();
    QName serviceName = jbwsServiceMetadata.getName();
    Definition wsdlDefinition = jbwsServiceMetadata.getWsdlDefinitions()
        .getWsdlOneOneDefinition();

    EndpointMetadata endpointMetadata = new EndpointMetadata();
    endpointMetadata.setServletContext(endpointContext.getServletContext());
    endpointMetadata.setWsdlDefinition(wsdlDefinition);
    endpointMetadata.setServiceName(serviceName);
    endpointMetadata.setPortName(portName.getLocalPart());
    endpointMetadata.setFaultFormat(FaultFormat.DEFAULT);

    return endpointMetadata;
  }
}
