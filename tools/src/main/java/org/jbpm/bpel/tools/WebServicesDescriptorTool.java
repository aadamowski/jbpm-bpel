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

import javax.wsdl.Binding;
import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.WSDLException;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import org.jbpm.bpel.integration.server.SoapHandler;
import org.jbpm.bpel.wsdl.xml.WsdlUtil;
import org.jbpm.bpel.xml.ProblemCounter;
import org.jbpm.bpel.xml.ProblemHandler;
import org.jbpm.bpel.xml.util.XmlUtil;
import org.jbpm.jpdl.xml.Problem;

/**
 * Generates the web services deployment descriptor.
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/11/29 10:14:00 $
 */
public class WebServicesDescriptorTool {

  private File wsdlFile = WscompileTool.DEFAULT_WSDL_FILE;
  private File jaxrpcMappingFile = WscompileTool.DEFAULT_JAXRPC_MAPPING_FILE;
  private File webServicesDescriptorFile = DEFAULT_WEB_SERVICES_FILE;

  private ProblemHandler problemHandler = new ProblemCounter();

  static final String NS_J2EE = "http://java.sun.com/xml/ns/j2ee";
  static final String ELEM_WEBSERVICES = "webservices";
  static final String ATTR_VERSION = "version";
  static final String ELEM_WEBSERVICE_DESCRIPTION = "webservice-description";
  static final String ELEM_WEBSERVICE_DESCRIPTION_NAME = "webservice-description-name";
  static final String ELEM_WSDL_FILE = "wsdl-file";
  static final String ELEM_JAXRPC_MAPPING_FILE = "jaxrpc-mapping-file";
  static final String ELEM_PORT_COMPONENT = "port-component";
  static final String ELEM_PORT_COMPONENT_NAME = "port-component-name";
  static final String ELEM_WSDL_PORT = "wsdl-port";
  static final String ELEM_SERVICE_ENDPOINT_INTERFACE = "service-endpoint-interface";
  static final String ELEM_SERVICE_IMPL_BEAN = "service-impl-bean";
  static final String ELEM_SERVLET_LINK = "servlet-link";
  static final String ELEM_HANDLER = "handler";
  static final String ELEM_HANDLER_NAME = "handler-name";
  static final String ELEM_HANDLER_CLASS = "handler-class";

  static final String WSEE_VERSION = "1.1";

  private static final String ELEM_SERVICE_ENDPOINT_INTERFACE_MAPPING = "service-endpoint-interface-mapping";
  private static final String ELEM_WSDL_BINDING = "wsdl-binding";
  private static final String ELEM_WSDL_PORT_TYPE = "wsdl-port-type";

  static final String DEFAULT_WEB_SERVICES_FILE_NAME = "webservices.xml";
  static final File DEFAULT_WEB_SERVICES_FILE = new File(DEFAULT_WEB_SERVICES_FILE_NAME);

  private static final Log log = LogFactory.getLog(WebServicesDescriptorTool.class);

  /**
   * Returns the input WSDL file.
   * @return the input WSDL file
   */
  public File getWsdlFile() {
    return wsdlFile;
  }

  /**
   * Sets the input WSDL file
   * @param wsdlFile the input WSDL file
   */
  public void setWsdlFile(File wsdlFile) {
    if (wsdlFile == null)
      throw new IllegalArgumentException("wsdl file cannot be null");

    this.wsdlFile = wsdlFile;
  }

  /**
   * Returns the input JAX-RPC mapping file.
   * @return the input JAX-RPC mapping file
   */
  public File getJaxrpcMappingFile() {
    return jaxrpcMappingFile;
  }

  /**
   * Specifies the input JAX-RPC mapping file
   * @param jaxrpcMappingFile the input JAX-RPC mapping file
   */
  public void setJaxrpcMappingFile(File jaxrpcMappingFile) {
    if (jaxrpcMappingFile == null)
      throw new IllegalArgumentException("jax-rpc mapping file cannot be null");

    this.jaxrpcMappingFile = jaxrpcMappingFile;
  }

  /**
   * Returns where to write the web services deployment descriptor.
   * @return the web services deployment descriptor file
   */
  public File getWebServicesDescriptorFile() {
    return webServicesDescriptorFile;
  }

  /**
   * Specifies where to write the web services deployment descriptor
   * @param descriptorFile the web services deployment descriptor file
   */
  public void setWebServicesDescriptorFile(File descriptorFile) {
    if (descriptorFile == null)
      throw new IllegalArgumentException("descriptor file cannot be null");

    this.webServicesDescriptorFile = descriptorFile;
  }

  public ProblemHandler getProblemHandler() {
    return problemHandler;
  }

  public void setProblemHandler(ProblemHandler problemHandler) {
    if (problemHandler == null)
      throw new IllegalArgumentException("problem handler cannot be null");

    this.problemHandler = problemHandler;
  }

  /**
   * Generates the web services deployment descriptor.
   */
  public void generateWebServicesDescriptor() {
    // wsdl service
    Definition serviceDef = readWsdlServiceDefinition();
    if (serviceDef == null)
      return;

    // jax-rpc mapping
    Document mappingDoc = readJaxrpcMappingDocument();
    if (mappingDoc == null)
      return;
    Element mappingElem = mappingDoc.getDocumentElement();

    // web services descriptor
    Element webservicesElem = XmlUtil.createElement(NS_J2EE, ELEM_WEBSERVICES);

    // version
    webservicesElem.setAttribute(ATTR_VERSION, WSEE_VERSION);

    // webservice descriptions
    for (Iterator i = serviceDef.getServices().values().iterator(); i.hasNext();) {
      Service service = (Service) i.next();
      generateWebserviceDescription(service, mappingElem, webservicesElem);
    }

    try {
      XmlUtil.writeFile(webservicesElem, webServicesDescriptorFile);
      log.debug("wrote web services descriptor: " + webServicesDescriptorFile.getName());
    }
    catch (IOException e) {
      problemHandler.add(new Problem(Problem.LEVEL_ERROR,
          "could not write web services descriptor: " + webServicesDescriptorFile, e));
    }
  }

  private Definition readWsdlServiceDefinition() {
    Definition definition = null;
    try {
      definition = WsdlUtil.getFactory().newWSDLReader().readWSDL(wsdlFile.getPath());
    }
    catch (WSDLException e) {
      problemHandler.add(new Problem(Problem.LEVEL_ERROR,
          "service document contains invalid wsdl: " + wsdlFile, e));
    }
    return definition;
  }

  private Document readJaxrpcMappingDocument() {
    DocumentBuilder documentBuilder = XmlUtil.getDocumentBuilder();

    // capture parse errors in our problem handler
    documentBuilder.setErrorHandler(problemHandler.asSaxErrorHandler());

    Document document = null;
    try {
      document = documentBuilder.parse(jaxrpcMappingFile);
    }
    catch (SAXException e) {
      problemHandler.add(new Problem(Problem.LEVEL_ERROR,
          "jax-rpc mapping document contains invalid xml: " + jaxrpcMappingFile, e));
    }
    catch (IOException e) {
      problemHandler.add(new Problem(Problem.LEVEL_ERROR,
          "jax-rpc mapping document is not readable: " + jaxrpcMappingFile, e));
    }
    finally {
      // reset error handling behavior
      documentBuilder.setErrorHandler(null);
    }
    return document;
  }

  protected void generateWebserviceDescription(Service service, Element jaxrpcMappingElem,
      Element webservicesElem) {
    Document webservicesDoc = webservicesElem.getOwnerDocument();
    Element descriptionElem = webservicesDoc.createElementNS(NS_J2EE, ELEM_WEBSERVICE_DESCRIPTION);
    webservicesElem.appendChild(descriptionElem);

    // webservice description name
    Element descriptionNameElem = webservicesDoc.createElementNS(NS_J2EE,
        ELEM_WEBSERVICE_DESCRIPTION_NAME);
    XmlUtil.setStringValue(descriptionNameElem, generateWebserviceDescriptionName(service));
    descriptionElem.appendChild(descriptionNameElem);

    // wsdl file
    Element wsdlFileElem = webservicesDoc.createElementNS(NS_J2EE, ELEM_WSDL_FILE);
    XmlUtil.setStringValue(wsdlFileElem, "WEB-INF/wsdl/" + wsdlFile.getName());
    descriptionElem.appendChild(wsdlFileElem);

    // jax-rpc mapping file
    Element jaxrpcMappingFileElem = webservicesDoc.createElementNS(NS_J2EE,
        ELEM_JAXRPC_MAPPING_FILE);
    XmlUtil.setStringValue(jaxrpcMappingFileElem, "WEB-INF/" + jaxrpcMappingFile.getName());
    descriptionElem.appendChild(jaxrpcMappingFileElem);

    // port components
    for (Iterator i = service.getPorts().values().iterator(); i.hasNext();) {
      Port port = (Port) i.next();
      generatePortComponent(service, port, jaxrpcMappingElem, descriptionElem);
    }
  }

  protected String generateWebserviceDescriptionName(Service service) {
    return service.getQName().getLocalPart();
  }

  protected void generatePortComponent(Service service, Port port, Element jaxrpcMappingElem,
      Element descriptionElem) {
    Document webservicesDoc = descriptionElem.getOwnerDocument();
    Element portComponentElem = webservicesDoc.createElementNS(NS_J2EE, ELEM_PORT_COMPONENT);
    descriptionElem.appendChild(portComponentElem);

    // port component name
    Element portComponentNameElem = webservicesDoc.createElementNS(NS_J2EE,
        ELEM_PORT_COMPONENT_NAME);
    XmlUtil.setStringValue(portComponentNameElem, generatePortComponentName(port));
    portComponentElem.appendChild(portComponentNameElem);

    // wsdl port
    Element wsdlPortElem = webservicesDoc.createElementNS(NS_J2EE, ELEM_WSDL_PORT);
    XmlUtil.setQNameValue(wsdlPortElem, new QName(service.getQName().getNamespaceURI(),
        port.getName(), "portNS"));
    portComponentElem.appendChild(wsdlPortElem);

    // service endpoint interface
    Element seiMappingElem = findSeiMapping(port, jaxrpcMappingElem);
    Element seiElem = XmlUtil.getElement(seiMappingElem, NS_J2EE, ELEM_SERVICE_ENDPOINT_INTERFACE);
    portComponentElem.appendChild(webservicesDoc.importNode(seiElem, true));

    // service implementation bean
    Element sibElem = webservicesDoc.createElementNS(NS_J2EE, ELEM_SERVICE_IMPL_BEAN);
    portComponentElem.appendChild(sibElem);

    // servlet link
    Element servletLinkElem = webservicesDoc.createElementNS(NS_J2EE, ELEM_SERVLET_LINK);
    XmlUtil.setStringValue(servletLinkElem, generateServletName(port));
    sibElem.appendChild(servletLinkElem);

    // handler
    Element handlerElem = webservicesDoc.createElementNS(NS_J2EE, ELEM_HANDLER);
    portComponentElem.appendChild(handlerElem);

    // handler name
    Element handlerNameElem = webservicesDoc.createElementNS(NS_J2EE, ELEM_HANDLER_NAME);
    XmlUtil.setStringValue(handlerNameElem, generateHandlerName(port));
    handlerElem.appendChild(handlerNameElem);

    // handler class
    Element handlerClassElem = webservicesDoc.createElementNS(NS_J2EE, ELEM_HANDLER_CLASS);
    XmlUtil.setStringValue(handlerClassElem, SoapHandler.class.getName());
    handlerElem.appendChild(handlerClassElem);
  }

  protected String generatePortComponentName(Port port) {
    return port.getName();
  }

  static Element findSeiMapping(Port port, Element jaxrpcMappingElem) {
    Binding binding = port.getBinding();
    QName bindingName = binding.getQName();
    QName portTypeName = binding.getPortType().getQName();

    for (Iterator i = XmlUtil.getElements(jaxrpcMappingElem, NS_J2EE,
        ELEM_SERVICE_ENDPOINT_INTERFACE_MAPPING); i.hasNext();) {
      Element seiMappingElem = (Element) i.next();
      // binding
      Element wsdlBindingElem = XmlUtil.getElement(seiMappingElem, NS_J2EE, ELEM_WSDL_BINDING);
      if (!bindingName.equals(XmlUtil.getQNameValue(wsdlBindingElem)))
        continue;
      // port type
      Element wsdlPortTypeElem = XmlUtil.getElement(seiMappingElem, NS_J2EE, ELEM_WSDL_PORT_TYPE);
      if (portTypeName.equals(XmlUtil.getQNameValue(wsdlPortTypeElem)))
        return seiMappingElem;
    }
    return null;
  }

  protected String generateServletName(Port port) {
    String portName = port.getName();
    StringBuffer servletName = new StringBuffer(portName);
    // remove "Port" suffix, if any
    if (portName.endsWith("Port"))
      servletName.setLength(servletName.length() - 4);
    // append "Servlet" suffix
    servletName.append("Servlet");
    return servletName.toString();
  }

  protected String generateHandlerName(Port port) {
    String portName = port.getName();
    StringBuffer handlerName = new StringBuffer(portName);
    // remove "Port" suffix, if any
    if (portName.endsWith("Port"))
      handlerName.setLength(handlerName.length() - 4);
    // append "Handler" suffix
    handlerName.append("Handler");
    return handlerName.toString();
  }
}
