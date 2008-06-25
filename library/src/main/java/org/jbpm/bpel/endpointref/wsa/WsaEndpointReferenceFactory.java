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

import org.jbpm.bpel.endpointref.EndpointReference;
import org.jbpm.bpel.endpointref.EndpointReferenceFactory;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/08/02 18:56:28 $
 */
public class WsaEndpointReferenceFactory extends EndpointReferenceFactory {

  public boolean acceptsReference(QName endpointRefName, String refScheme) {
    return (refScheme == null || refScheme.length() == 0 || refScheme.equals(WsaConstants.NS_ADDRESSING))
        && endpointRefName.getNamespaceURI().equals(WsaConstants.NS_ADDRESSING)
        && endpointRefName.getLocalPart().equals(WsaConstants.ELEM_ENDPOINT_REFERENCE);
  }

  public EndpointReference createEndpointReference() {
    return new WsaEndpointReference();
  }
}
