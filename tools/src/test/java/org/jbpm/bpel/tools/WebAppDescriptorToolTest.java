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
package org.jbpm.bpel.tools;

import java.io.File;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.jbpm.bpel.integration.server.IntegrationConfigurator;
import org.jbpm.bpel.xml.util.DatatypeUtil;
import org.jbpm.bpel.xml.util.XmlUtil;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/11/25 13:07:08 $
 */
public class WebAppDescriptorToolTest extends TestCase {

  private static WebAppDescriptorTool tool = new WebAppDescriptorTool();

  private static Document webServicesDoc;
  private static Document webAppDoc;

  private static final String NS_J2EE = WebAppDescriptorTool.NS_J2EE;

  public void testGenerateWebAppDescriptor() {
    Element webAppElem = webAppDoc.getDocumentElement();
    assertEquals(NS_J2EE, webAppElem.getNamespaceURI());
    assertEquals(WebAppDescriptorTool.ELEM_WEB_APP, webAppElem.getLocalName());

    // version
    assertEquals(WebAppDescriptorTool.SERVLET_VERSION,
        webAppElem.getAttribute(WebAppDescriptorTool.ATTR_VERSION));

    // servlets
    int servletCount = 0;
    for (Iterator d = XmlUtil.getElements(webServicesDoc.getDocumentElement(), NS_J2EE,
        WebServicesDescriptorTool.ELEM_WEBSERVICE_DESCRIPTION); d.hasNext();) {
      Element wsDescriptionElem = (Element) d.next();
      for (Iterator p = XmlUtil.getElements(wsDescriptionElem, NS_J2EE,
          WebServicesDescriptorTool.ELEM_PORT_COMPONENT); p.hasNext(); p.next())
        servletCount++;
    }
    assertEquals(servletCount, XmlUtil.countElements(webAppElem, NS_J2EE,
        WebAppDescriptorTool.ELEM_SERVLET));

    // servlet mapping
    assertEquals(servletCount, XmlUtil.countElements(webAppElem, NS_J2EE,
        WebAppDescriptorTool.ELEM_SERVLET_MAPPING));

    // listener
    assertEquals(1, XmlUtil.countElements(webAppElem, NS_J2EE, WebAppDescriptorTool.ELEM_LISTENER));
  }

  public void testGenerateEndpointServlet() {
    Element webAppElem = webAppDoc.getDocumentElement();

    for (Iterator d = XmlUtil.getElements(webServicesDoc.getDocumentElement(), NS_J2EE,
        WebServicesDescriptorTool.ELEM_WEBSERVICE_DESCRIPTION); d.hasNext();) {
      Element wsDescriptionElem = (Element) d.next();

      for (Iterator p = XmlUtil.getElements(wsDescriptionElem, NS_J2EE,
          WebServicesDescriptorTool.ELEM_PORT_COMPONENT); p.hasNext();) {
        Element portComponentElem = (Element) p.next();

        Element sibElem = XmlUtil.getElement(portComponentElem, NS_J2EE,
            WebServicesDescriptorTool.ELEM_SERVICE_IMPL_BEAN);
        Element servletLinkElem = XmlUtil.getElement(sibElem, NS_J2EE,
            WebServicesDescriptorTool.ELEM_SERVLET_LINK);
        String servletName = DatatypeUtil.toString(servletLinkElem);

        // servlet name
        Element servletElem = findServlet(webAppElem, servletName);

        // servlet class
        Element seiElem = XmlUtil.getElement(portComponentElem, NS_J2EE,
            WebServicesDescriptorTool.ELEM_SERVICE_ENDPOINT_INTERFACE);

        Element servletClassElem = XmlUtil.getElement(servletElem, NS_J2EE,
            WebAppDescriptorTool.ELEM_SERVLET_CLASS);
        assertEquals(DatatypeUtil.toString(seiElem) + "_Impl",
            DatatypeUtil.toString(servletClassElem));

        // servlet mapping
        Element servletMappingElem = findServletMapping(webAppElem, servletName);

        // url pattern
        Element urlPatternElem = XmlUtil.getElement(servletMappingElem, NS_J2EE,
            WebAppDescriptorTool.ELEM_URL_PATTERN);
        assertEquals('/' + servletName.substring(0, servletName.length() - "Servlet".length()),
            DatatypeUtil.toString(urlPatternElem));
      }
    }
  }

  private static Element findServlet(Element webAppElem, String servletName) {
    for (Iterator i = XmlUtil.getElements(webAppElem, NS_J2EE, WebAppDescriptorTool.ELEM_SERVLET); i.hasNext();) {
      Element servletElem = (Element) i.next();
      Element servletNameElem = XmlUtil.getElement(servletElem, NS_J2EE,
          WebAppDescriptorTool.ELEM_SERVLET_NAME);
      if (servletName.equals(DatatypeUtil.toString(servletNameElem)))
        return servletElem;
    }
    return null;
  }

  private static Element findServletMapping(Element webAppElem, String servletName) {
    for (Iterator i = XmlUtil.getElements(webAppElem, NS_J2EE,
        WebAppDescriptorTool.ELEM_SERVLET_MAPPING); i.hasNext();) {
      Element servletMappingElem = (Element) i.next();
      Element servletNameElem = XmlUtil.getElement(servletMappingElem, NS_J2EE,
          WebAppDescriptorTool.ELEM_SERVLET_NAME);
      if (servletName.equals(DatatypeUtil.toString(servletNameElem)))
        return servletMappingElem;
    }
    return null;
  }

  public void testGenerateIntegrationConfiguratorListener() {
    // listener
    Element listenerElem = XmlUtil.getElement(webAppDoc.getDocumentElement(), NS_J2EE,
        WebAppDescriptorTool.ELEM_LISTENER);

    // listener class
    Element listenerClassElem = XmlUtil.getElement(listenerElem, NS_J2EE,
        WebAppDescriptorTool.ELEM_LISTENER_CLASS);
    assertEquals(IntegrationConfigurator.class.getName(), DatatypeUtil.toString(listenerClassElem));
  }

  public static Test suite() {
    return new Setup();
  }

  private static class Setup extends TestSetup {

    Setup() {
      super(new TestSuite(WebAppDescriptorToolTest.class));
    }

    protected void setUp() throws Exception {
      DocumentBuilder documentBuilder = XmlUtil.getDocumentBuilder();
      File webServicesFile = FileUtil.toFile(WebAppDescriptorToolTest.class, "webservices.xml");
      webServicesDoc = documentBuilder.parse(webServicesFile);

      tool.setWebServicesDescriptorFile(webServicesFile);
      tool.generateWebAppDescriptor();

      webAppDoc = documentBuilder.parse(tool.getWebAppDescriptorFile());
    }

    protected void tearDown() throws Exception {
      tool.getWebAppDescriptorFile().delete();
    }
  }
}
