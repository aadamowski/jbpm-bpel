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

import com.ibm.wsdl.Constants;

import org.jbpm.bpel.endpointref.EndpointReference;
import org.jbpm.bpel.endpointref.EndpointReferenceFactory;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/08/02 18:56:28 $
 */
public class WsdlEndpointReferenceFactory extends EndpointReferenceFactory {

  public EndpointReference createEndpointReference() {
    return new WsdlEndpointReference();
  }

  public boolean acceptsReference(QName endpointRefName, String refScheme) {
    return (refScheme == null || refScheme.length() == 0 || refScheme.equals(Constants.NS_URI_WSDL))
        && endpointRefName.equals(Constants.Q_ELEM_SERVICE);
  }
}
