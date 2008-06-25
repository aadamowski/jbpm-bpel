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

import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.jbpm.bpel.integration.server.SoapHandler;
import org.jbpm.bpel.wsdl.xml.WsdlUtil;
import org.jbpm.bpel.xml.util.DatatypeUtil;
import org.jbpm.bpel.xml.util.XmlUtil;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/11/06 22:06:25 $
 */
public class WebServicesDescriptorToolTest extends TestCase {

  private static WebServicesDescriptorTool tool = new WebServicesDescriptorTool();

  private static Definition serviceDefinition;
  private static Document webServicesDoc;
  private static Document jaxrpcMappingDoc;

  private static final String NS_J2EE = WebServicesDescriptorTool.NS_J2EE;

  public void testGenerateWebservicesDescriptor() {
    Element webservicesElem = webServicesDoc.getDocumentElement();
    assertEquals(NS_J2EE, webservicesElem.getNamespaceURI());
    assertEquals(WebServicesDescriptorTool.ELEM_WEBSERVICES, webservicesElem.getLocalName());

    // version
    assertEquals(WebServicesDescriptorTool.WSEE_VERSION,
        webservicesElem.getAttribute(WebServicesDescriptorTool.ATTR_VERSION));

    // webservice descriptions
    assertEquals(serviceDefinition.getServices().size(), XmlUtil.countElements(webservicesElem,
        NS_J2EE, WebServicesDescriptorTool.ELEM_WEBSERVICE_DESCRIPTION));
  }

  public void testGenerateWebserviceDescription() {
    Service service = (Service) serviceDefinition.getServices().values().iterator().next();
    Element descriptionElem = XmlUtil.getElement(webServicesDoc.getDocumentElement(), NS_J2EE,
        WebServicesDescriptorTool.ELEM_WEBSERVICE_DESCRIPTION);

    // webservice description name
    Element descriptionNameElem = XmlUtil.getElement(descriptionElem, NS_J2EE,
        WebServicesDescriptorTool.ELEM_WEBSERVICE_DESCRIPTION_NAME);
    assertEquals(service.getQName().getLocalPart(), DatatypeUtil.toString(descriptionNameElem));

    // wsdl file
    Element wsdlFileElem = XmlUtil.getElement(descriptionElem, NS_J2EE,
        WebServicesDescriptorTool.ELEM_WSDL_FILE);
    assertEquals("WEB-INF/wsdl/" + tool.getWsdlFile().getName(),
        DatatypeUtil.toString(wsdlFileElem));

    // jax-rpc mapping file
    Element jaxrpcMappingFileElem = XmlUtil.getElement(descriptionElem, NS_J2EE,
        WebServicesDescriptorTool.ELEM_JAXRPC_MAPPING_FILE);
    assertEquals("WEB-INF/" + tool.getJaxrpcMappingFile().getName(),
        DatatypeUtil.toString(jaxrpcMappingFileElem));

    // port components
    assertEquals(service.getPorts().size(), XmlUtil.countElements(descriptionElem, NS_J2EE,
        WebServicesDescriptorTool.ELEM_PORT_COMPONENT));
  }

  public void testGeneratePortComponent() {
    Service service = (Service) serviceDefinition.getServices().values().iterator().next();
    Element jaxrpcMappingElem = jaxrpcMappingDoc.getDocumentElement();
    Element descriptionElem = XmlUtil.getElement(webServicesDoc.getDocumentElement(), NS_J2EE,
        WebServicesDescriptorTool.ELEM_WEBSERVICE_DESCRIPTION);

    for (Iterator i = XmlUtil.getElements(descriptionElem, NS_J2EE,
        WebServicesDescriptorTool.ELEM_PORT_COMPONENT); i.hasNext();) {
      Element portComponentElem = (Element) i.next();

      // wsdl port
      Element wsdlPortElem = XmlUtil.getElement(portComponentElem, NS_J2EE,
          WebServicesDescriptorTool.ELEM_WSDL_PORT);
      QName portQName = XmlUtil.getQNameValue(wsdlPortElem);
      Port port = service.getPort(portQName.getLocalPart());
      assertEquals(service.getQName().getNamespaceURI(), portQName.getNamespaceURI());
      assertNotNull(port);

      // port component name
      Element portComponentNameElem = XmlUtil.getElement(portComponentElem, NS_J2EE,
          WebServicesDescriptorTool.ELEM_PORT_COMPONENT_NAME);
      String portName = port.getName();
      assertEquals(portName, DatatypeUtil.toString(portComponentNameElem));

      // service endpoint interface
      Element seiMappingElem = WebServicesDescriptorTool.findSeiMapping(port, jaxrpcMappingElem);
      Element mappingSeiElem = XmlUtil.getElement(seiMappingElem, NS_J2EE,
          WebServicesDescriptorTool.ELEM_SERVICE_ENDPOINT_INTERFACE);
      Element seiElem = XmlUtil.getElement(portComponentElem, NS_J2EE,
          WebServicesDescriptorTool.ELEM_SERVICE_ENDPOINT_INTERFACE);
      assertEquals(DatatypeUtil.toString(mappingSeiElem), DatatypeUtil.toString(seiElem));

      // service implementation bean
      Element sibElem = XmlUtil.getElement(portComponentElem, NS_J2EE,
          WebServicesDescriptorTool.ELEM_SERVICE_IMPL_BEAN);

      // servlet link
      Element servletLinkElem = XmlUtil.getElement(sibElem, NS_J2EE,
          WebServicesDescriptorTool.ELEM_SERVLET_LINK);
      assertEquals(portName.substring(0, portName.length() - 4) + "Servlet",
          DatatypeUtil.toString(servletLinkElem));

      // handler
      Element handlerElem = XmlUtil.getElement(portComponentElem, NS_J2EE,
          WebServicesDescriptorTool.ELEM_HANDLER);

      // handler name
      Element handlerNameElem = XmlUtil.getElement(handlerElem, NS_J2EE,
          WebServicesDescriptorTool.ELEM_HANDLER_NAME);
      assertEquals(portName.substring(0, portName.length() - 4) + "Handler",
          DatatypeUtil.toString(handlerNameElem));

      // handler class
      Element handlerClassElem = XmlUtil.getElement(handlerElem, NS_J2EE,
          WebServicesDescriptorTool.ELEM_HANDLER_CLASS);
      assertEquals(SoapHandler.class.getName(), DatatypeUtil.toString(handlerClassElem));
    }
  }

  public static Test suite() {
    return new Setup();
  }

  private static class Setup extends TestSetup {

    Setup() {
      super(new TestSuite(WebServicesDescriptorToolTest.class));
    }

    protected void setUp() throws Exception {
      File wsdlFile = FileUtil.toFile(WebServicesDescriptorToolTest.class, "service.wsdl");
      serviceDefinition = WsdlUtil.getFactory().newWSDLReader().readWSDL(wsdlFile.getPath());

      DocumentBuilder documentBuilder = XmlUtil.getDocumentBuilder();
      File jaxrpcMappingFile = FileUtil.toFile(WebServicesDescriptorToolTest.class,
          "jaxrpc-mapping.xml");
      jaxrpcMappingDoc = documentBuilder.parse(jaxrpcMappingFile);

      tool.setWsdlFile(wsdlFile);
      tool.setJaxrpcMappingFile(jaxrpcMappingFile);
      tool.generateWebServicesDescriptor();

      webServicesDoc = documentBuilder.parse(tool.getWebServicesDescriptorFile());
    }

    protected void tearDown() throws Exception {
      tool.getWebServicesDescriptorFile().delete();
    }
  }
}
