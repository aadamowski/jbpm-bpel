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

import junit.framework.TestCase;

import org.jbpm.bpel.integration.catalog.UrlCatalog;
import org.jbpm.bpel.xml.util.XmlUtil;

public class UrlCatalogReaderTest extends TestCase {

  private static final String CATALOG_TEXT = "<urlCatalog xmlns='"
      + BpelConstants.NS_DEPLOYMENT_DESCRIPTOR
      + "'>"
      + " <wsdl location='partnerLinkTypeSample.wsdl' />"
      + " <wsdl location='propertyAliasSample.wsdl' />"
      + "</urlCatalog>";

  public void testRead() throws Exception {
    ServiceCatalogReader reader = new UrlCatalogReader();
    UrlCatalog catalog = (UrlCatalog) reader.read(XmlUtil.parseText(CATALOG_TEXT),
        BpelConstants.NS_EXAMPLES);

    // context URL
    assertEquals(BpelConstants.NS_EXAMPLES, catalog.getContextURL());

    // locations
    List locations = catalog.getLocations();
    assertEquals(2, locations.size());
    // first location
    String location = (String) locations.get(0);
    assertEquals("partnerLinkTypeSample.wsdl", location);
    // second location
    location = (String) locations.get(1);
    assertEquals("propertyAliasSample.wsdl", location);
  }
}
