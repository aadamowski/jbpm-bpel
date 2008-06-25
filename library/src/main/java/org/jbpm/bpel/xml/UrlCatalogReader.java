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
package org.jbpm.bpel.xml;

import java.util.Iterator;

import org.w3c.dom.Element;

import org.jbpm.bpel.integration.catalog.ServiceCatalog;
import org.jbpm.bpel.integration.catalog.UrlCatalog;
import org.jbpm.bpel.xml.util.XmlUtil;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/10/13 02:53:27 $
 */
public class UrlCatalogReader implements ServiceCatalogReader {

  public ServiceCatalog read(Element catalogElem, String documentBaseURI) {
    UrlCatalog catalog = new UrlCatalog();

    // context url
    String contextUrl = XmlUtil.getAttribute(catalogElem, BpelConstants.ATTR_CONTEXT_URL);
    catalog.setContextURL(contextUrl != null ? contextUrl : documentBaseURI);

    // locations
    for (Iterator i = XmlUtil.getElements(catalogElem, BpelConstants.NS_DEPLOYMENT_DESCRIPTOR,
        BpelConstants.ELEM_WSDL); i.hasNext();) {
      Element wsdlElem = (Element) i.next();
      String location = wsdlElem.getAttribute(BpelConstants.ATTR_LOCATION);
      catalog.addLocation(location);
    }
    return catalog;
  }
}
