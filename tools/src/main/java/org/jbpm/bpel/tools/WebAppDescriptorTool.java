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
import java.io.IOException;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import org.jbpm.bpel.integration.server.IntegrationConfigurator;
import org.jbpm.bpel.xml.ProblemCounter;
import org.jbpm.bpel.xml.ProblemHandler;
import org.jbpm.bpel.xml.util.DatatypeUtil;
import org.jbpm.bpel.xml.util.XmlUtil;
import org.jbpm.jpdl.xml.Problem;

/**
 * Generates the web application deployment descriptor.
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/11/29 10:14:00 $
 */
public class WebAppDescriptorTool {

  private File webServicesDescriptorFile = WebServicesDescriptorTool.DEFAULT_WEB_SERVICES_FILE;
  private File webAppDescriptorFile = DEFAULT_WEB_APP_FILE;

  private ProblemHandler problemHandler = new ProblemCounter();

  static final String NS_J2EE = "http://java.sun.com/xml/ns/j2ee";
  static final String ELEM_WEB_APP = "web-app";
  static final String ATTR_VERSION = "version";
  static final String ELEM_SERVLET = "servlet";
  static final String ELEM_SERVLET_NAME = "servlet-name";
  static final String ELEM_SERVLET_CLASS = "servlet-class";
  static final String ELEM_SERVLET_MAPPING = "servlet-mapping";
  static final String ELEM_URL_PATTERN = "url-pattern";
  static final String ELEM_LISTENER = "listener";
  static final String ELEM_LISTENER_CLASS = "listener-class";

  static final String SERVLET_VERSION = "2.4";
  static final String INTEGRATION_CONSOLE_NAME = "integrationConsole";

  static final String DEFAULT_WEB_APP_FILE_NAME = "web.xml";
  static final File DEFAULT_WEB_APP_FILE = new File(DEFAULT_WEB_APP_FILE_NAME);

  private static final Log log = LogFactory.getLog(WebAppDescriptorTool.class);

  /**
   * Returns the input web services deployment descriptor.
   * @return the input web services descriptor
   */
  public File getWebServicesDescriptorFile() {
    return webServicesDescriptorFile;
  }

  /**
   * Specifies the input web services deployment descriptor
   * @param webServicesDescriptorFile the web services deployment descriptor
   */
  public void setWebServicesDescriptorFile(File webServicesDescriptorFile) {
    if (webServicesDescriptorFile == null)
      throw new IllegalArgumentException("web services descriptor file cannot be null");

    this.webServicesDescriptorFile = webServicesDescriptorFile;
  }

  /**
   * Returns where to write the web application deployment descriptor.
   * @return the web application descriptor file
   */
  public File getWebAppDescriptorFile() {
    return webAppDescriptorFile;
  }

  /**
   * Specifies where to write the web application deployment descriptor.
   * @param webAppDescriptorFile the web application descriptor file
   */
  public void setWebAppDescriptorFile(File webAppDescriptorFile) {
    if (webAppDescriptorFile == null)
      throw new IllegalArgumentException("web app descriptor file cannot be null");

    this.webAppDescriptorFile = webAppDescriptorFile;
  }

  /**
   * Generates a {@linkplain #setWebAppDescriptorFile(File) web application descriptor} compatible
   * with the settings in the {@linkplain #setWebServicesDescriptorFile(File) web services
   * descriptor}.
   */
  public void generateWebAppDescriptor() {
    // web services descriptor
    Document webservicesDoc = readWebServicesDescriptor();
    if (webservicesDoc == null)
      return;

    // web app descriptor
    Element webAppElem = XmlUtil.createElement(NS_J2EE, ELEM_WEB_APP);

    // version
    webAppElem.setAttribute(ATTR_VERSION, SERVLET_VERSION);

    // endpoint servlets
    for (Iterator d = XmlUtil.getElements(webservicesDoc.getDocumentElement(), NS_J2EE,
        WebServicesDescriptorTool.ELEM_WEBSERVICE_DESCRIPTION); d.hasNext();) {
      Element descriptionElem = (Element) d.next();

      for (Iterator p = XmlUtil.getElements(descriptionElem, NS_J2EE,
          WebServicesDescriptorTool.ELEM_PORT_COMPONENT); p.hasNext();) {
        Element portComponentElem = (Element) p.next();
        generateEndpointServlet(portComponentElem, webAppElem);
      }
    }

    // integration configurator listener
    generateIntegrationConfiguratorListener(webAppElem);

    try {
      XmlUtil.writeFile(webAppElem, webAppDescriptorFile);
      log.debug("wrote web application descriptor: " + webAppDescriptorFile.getName());
    }
    catch (IOException e) {
      problemHandler.add(new Problem(Problem.LEVEL_ERROR,
          "could not write web application descriptor: " + webAppDescriptorFile, e));
    }
  }

  private Document readWebServicesDescriptor() {
    DocumentBuilder documentBuilder = XmlUtil.getDocumentBuilder();

    // capture parse errors in our problem handler
    documentBuilder.setErrorHandler(problemHandler.asSaxErrorHandler());

    Document document = null;
    try {
      document = documentBuilder.parse(webServicesDescriptorFile);
    }
    catch (SAXException e) {
      problemHandler.add(new Problem(Problem.LEVEL_ERROR,
          "web services descriptor contains invalid xml: " + webServicesDescriptorFile, e));
    }
    catch (IOException e) {
      problemHandler.add(new Problem(Problem.LEVEL_ERROR,
          "web services descriptor is not readable: " + webServicesDescriptorFile, e));
    }
    finally {
      // reset error handling behavior
      documentBuilder.setErrorHandler(null);
    }
    return document;
  }

  protected void generateEndpointServlet(Element portComponentElem, Element webAppElem) {
    // servlet name
    Element sibElem = XmlUtil.getElement(portComponentElem, NS_J2EE,
        WebServicesDescriptorTool.ELEM_SERVICE_IMPL_BEAN);
    Element servletLinkElem = XmlUtil.getElement(sibElem, NS_J2EE,
        WebServicesDescriptorTool.ELEM_SERVLET_LINK);
    String servletName = DatatypeUtil.toString(servletLinkElem);

    // servlet class
    Element seiElem = XmlUtil.getElement(portComponentElem, NS_J2EE,
        WebServicesDescriptorTool.ELEM_SERVICE_ENDPOINT_INTERFACE);
    String seiName = DatatypeUtil.toString(seiElem);

    generateServlet(webAppElem, servletName, generateServiceImplBeanName(seiName));
  }

  private void generateServlet(Element webAppElem, String servletName, String servletClass) {
    Document webAppDoc = webAppElem.getOwnerDocument();

    // servlet
    Element servletElem = webAppDoc.createElementNS(NS_J2EE, ELEM_SERVLET);
    webAppElem.appendChild(servletElem);

    // servlet name
    Element servletNameElem = webAppDoc.createElementNS(NS_J2EE, ELEM_SERVLET_NAME);
    XmlUtil.setStringValue(servletNameElem, servletName);
    servletElem.appendChild(servletNameElem);

    // servlet class
    Element servletClassElem = webAppDoc.createElementNS(NS_J2EE, ELEM_SERVLET_CLASS);
    XmlUtil.setStringValue(servletClassElem, servletClass);
    servletElem.appendChild(servletClassElem);

    // servlet mapping
    Element servletMappingElem = webAppDoc.createElementNS(NS_J2EE, ELEM_SERVLET_MAPPING);
    webAppElem.appendChild(servletMappingElem);

    // servlet name
    servletMappingElem.appendChild(servletNameElem.cloneNode(true));

    // url pattern
    Element urlPatternElem = webAppDoc.createElementNS(NS_J2EE, ELEM_URL_PATTERN);
    XmlUtil.setStringValue(urlPatternElem, generateUrlPattern(servletName));
    servletMappingElem.appendChild(urlPatternElem);
  }

  protected String generateServiceImplBeanName(String seiName) {
    return seiName + "_Impl";
  }

  protected String generateUrlPattern(String servletName) {
    if (servletName.endsWith("Servlet"))
      servletName = servletName.substring(0, servletName.length() - "Servlet".length());
    return '/' + servletName;
  }

  protected void generateIntegrationConfiguratorListener(Element webAppElem) {
    Document webAppDoc = webAppElem.getOwnerDocument();

    // listener
    Element listenerElem = webAppDoc.createElementNS(NS_J2EE, ELEM_LISTENER);
    webAppElem.appendChild(listenerElem);

    // listener class
    Element listenerClassElem = webAppDoc.createElementNS(NS_J2EE, ELEM_LISTENER_CLASS);
    XmlUtil.setStringValue(listenerClassElem, IntegrationConfigurator.class.getName());
    listenerElem.appendChild(listenerClassElem);
  }
}
