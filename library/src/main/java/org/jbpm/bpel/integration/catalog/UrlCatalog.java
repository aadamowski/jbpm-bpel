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
package org.jbpm.bpel.integration.catalog;

import java.util.ArrayList;
import java.util.List;

import javax.wsdl.WSDLException;
import javax.wsdl.xml.WSDLReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.bpel.wsdl.xml.WsdlUtil;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/10/13 02:53:29 $
 */
public class UrlCatalog extends DecoratorCatalog {

  private String contextURL;
  private List locations = new ArrayList();

  private static final Log log = LogFactory.getLog(UrlCatalog.class);

  public String getContextURL() {
    return contextURL;
  }

  public void setContextURL(String contextURL) {
    this.contextURL = contextURL;
  }

  public void addLocation(String location) {
    locations.add(location);
  }

  public List getLocations() {
    return locations;
  }

  protected ServiceCatalog createDelegate() {
    DefinitionCatalog catalog = new DefinitionCatalog();
    WSDLReader reader = WsdlUtil.getFactory().newWSDLReader();

    for (int i = 0, n = locations.size(); i < n; i++) {
      String location = (String) locations.get(i);
      try {
        catalog.addDefinition(reader.readWSDL(contextURL, location));
      }
      catch (WSDLException e) {
        log.debug("skipping wsdl document at '" + location + "' due to '" + e.getMessage() + "'");
      }
    }
    return catalog;
  }
}
