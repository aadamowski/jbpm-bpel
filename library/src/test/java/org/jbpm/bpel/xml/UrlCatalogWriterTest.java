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

import org.jbpm.bpel.integration.catalog.UrlCatalog;
import org.jbpm.bpel.xml.util.XmlUtil;

import junit.framework.TestCase;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/10/13 02:53:24 $
 */
public class UrlCatalogWriterTest extends TestCase {

  public void testWrite() {
    UrlCatalog catalog = new UrlCatalog();
    catalog.setContextURL(BpelConstants.NS_EXAMPLES);
    catalog.addLocation("partnerLinkTypeSample.wsdl");
    catalog.addLocation("propertyAliasSample.wsdl");

    ServiceCatalogWriter writer = new UrlCatalogWriter();
    Element catalogsElem = XmlUtil.createElement(BpelConstants.NS_DEPLOYMENT_DESCRIPTOR,
        BpelConstants.ELEM_SERVICE_CATALOGS);
    writer.write(catalog, catalogsElem);

    Element catalogElem = XmlUtil.getElement(catalogsElem, BpelConstants.NS_DEPLOYMENT_DESCRIPTOR,
        "urlCatalog");
    // context URL
    assertEquals(BpelConstants.NS_EXAMPLES,
        catalogElem.getAttribute(BpelConstants.ATTR_CONTEXT_URL));
    // locations
    Iterator wsdlElemIt = XmlUtil.getElements(catalogElem, BpelConstants.NS_DEPLOYMENT_DESCRIPTOR,
        BpelConstants.ELEM_WSDL);
    // first location
    Element wsdlElem = (Element) wsdlElemIt.next();
    assertEquals("partnerLinkTypeSample.wsdl", wsdlElem.getAttribute(BpelConstants.ATTR_LOCATION));
    // second location
    wsdlElem = (Element) wsdlElemIt.next();
    assertEquals("propertyAliasSample.wsdl", wsdlElem.getAttribute(BpelConstants.ATTR_LOCATION));

    assertFalse(wsdlElemIt.hasNext());
  }

}
