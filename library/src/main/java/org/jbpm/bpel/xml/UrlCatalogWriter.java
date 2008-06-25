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

import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.jbpm.bpel.integration.catalog.ServiceCatalog;
import org.jbpm.bpel.integration.catalog.UrlCatalog;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/10/13 02:53:27 $
 */
public class UrlCatalogWriter implements ServiceCatalogWriter {

  public void write(ServiceCatalog catalog, Element catalogsElem) {
    Document descriptorDoc = catalogsElem.getOwnerDocument();
    Element catalogElem = descriptorDoc.createElementNS(BpelConstants.NS_DEPLOYMENT_DESCRIPTOR,
        "urlCatalog");
    catalogsElem.appendChild(catalogElem);

    // context url
    UrlCatalog urlCatalog = (UrlCatalog) catalog;
    String contextUrl = urlCatalog.getContextURL();
    if (contextUrl != null)
      catalogElem.setAttribute(BpelConstants.ATTR_CONTEXT_URL, contextUrl);

    // locations
    List locations = urlCatalog.getLocations();
    for (int i = 0, n = locations.size(); i < n; i++) {
      String location = (String) locations.get(i);
      Element wsdlElem = descriptorDoc.createElementNS(BpelConstants.NS_DEPLOYMENT_DESCRIPTOR,
          BpelConstants.ELEM_WSDL);
      catalogElem.appendChild(wsdlElem);
      wsdlElem.setAttribute(BpelConstants.ATTR_LOCATION, location);
    }
  }
}
