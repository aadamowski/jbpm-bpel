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

import javax.servlet.ServletContext;
import javax.wsdl.Definition;
import javax.xml.namespace.QName;
import javax.xml.rpc.handler.MessageContext;

import org.jboss.ws.core.jaxrpc.handler.MessageContextJAXRPC;
import org.jboss.ws.core.jaxrpc.handler.SOAPMessageContextJAXRPC;
import org.jboss.ws.metadata.umdm.EndpointMetaData;
import org.jboss.ws.metadata.umdm.ServiceMetaData;

import org.jbpm.bpel.integration.soap.FaultFormat;

/**
 * The JBossWS 1.2 endpoint metadata lookup queries the
 * {@linkplain EndpointMetaData endpoint} and
 * {@linkplain ServiceMetaData service} metadata referenced from the message
 * context.
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/11/12 00:45:09 $
 */
public class JBoss420EndpointMetadataLookup implements EndpointMetadataLookup {

  public EndpointMetadata lookupMetaData(MessageContext messageContext) {
    SOAPMessageContextJAXRPC jbwsMessageContext = (SOAPMessageContextJAXRPC) messageContext;
    ServletContext servletContext = (ServletContext) messageContext.getProperty(MessageContextJAXRPC.SERVLET_CONTEXT);

    EndpointMetaData jbwsEndpointMetadata = jbwsMessageContext.getEndpointMetaData();
    QName portName = jbwsEndpointMetadata.getPortName();

    ServiceMetaData jbwsServiceMetadata = jbwsEndpointMetadata.getServiceMetaData();
    QName serviceName = jbwsServiceMetadata.getServiceName();
    Definition wsdlDefinition = jbwsServiceMetadata.getWsdlDefinitions()
        .getWsdlOneOneDefinition();

    EndpointMetadata endpointMetadata = new EndpointMetadata();
    endpointMetadata.setServletContext(servletContext);
    endpointMetadata.setWsdlDefinition(wsdlDefinition);
    endpointMetadata.setServiceName(serviceName);
    endpointMetadata.setPortName(portName.getLocalPart());
    endpointMetadata.setFaultFormat(FaultFormat.DEFAULT);

    return endpointMetadata;
  }
}
