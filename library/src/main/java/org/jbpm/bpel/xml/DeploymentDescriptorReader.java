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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.jbpm.JbpmConfiguration;
import org.jbpm.bpel.deploy.DeploymentDescriptor;
import org.jbpm.bpel.deploy.MyRoleDescriptor;
import org.jbpm.bpel.deploy.PartnerLinkDescriptor;
import org.jbpm.bpel.deploy.PartnerRoleDescriptor;
import org.jbpm.bpel.deploy.ScopeDescriptor;
import org.jbpm.bpel.deploy.PartnerRoleDescriptor.InitiateMode;
import org.jbpm.bpel.endpointref.EndpointReference;
import org.jbpm.bpel.integration.catalog.CompositeCatalog;
import org.jbpm.bpel.integration.catalog.ServiceCatalog;
import org.jbpm.bpel.xml.util.XmlUtil;
import org.jbpm.jpdl.xml.Problem;
import org.jbpm.util.ClassLoaderUtil;

/**
 * Converts a deployment descriptor in XML format to an {@linkplain DeploymentDescriptor object}.
 * @author Juan Cantú
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/10/13 02:53:27 $
 */
public class DeploymentDescriptorReader {

  private ProblemHandler problemHandler = new ProblemCounter();

  public static final String RESOURCE_CATALOG_READERS = "resource.catalog.readers";

  private static final Log log = LogFactory.getLog(DeploymentDescriptorReader.class);
  private static final Map catalogReaders = readCatalogReaders();

  public void read(DeploymentDescriptor deploymentDescriptor, InputSource source) {
    // get the thread-local parser
    DocumentBuilder builder = XmlUtil.getDocumentBuilder();

    // install our problem handler as document parser's error handler
    builder.setErrorHandler(problemHandler.asSaxErrorHandler());

    try {
      // parse content
      Document descriptorDoc = builder.parse(source);

      // halt on parse errors
      if (problemHandler.getProblemCount() > 0)
        return;

      Element descriptorElem = descriptorDoc.getDocumentElement();

      // global scope
      readScope(descriptorElem, deploymentDescriptor);

      // target namespace
      String targetNamespace = XmlUtil.getAttribute(descriptorElem,
          BpelConstants.ATTR_TARGET_NAMESPACE);
      if (targetNamespace != null)
        deploymentDescriptor.setTargetNamespace(targetNamespace);

      // version
      String version = XmlUtil.getAttribute(descriptorElem, BpelConstants.ATTR_VERSION);
      if (version != null)
        deploymentDescriptor.setVersion(Integer.valueOf(version));

      // service catalogs
      Element catalogsElem = XmlUtil.getElement(descriptorElem, BpelConstants.NS_DEPLOYMENT_DESCRIPTOR,
          BpelConstants.ELEM_SERVICE_CATALOGS);
      if (catalogsElem != null) {
        ServiceCatalog catalog = readServiceCatalogs(catalogsElem, source.getSystemId());
        deploymentDescriptor.setServiceCatalog(catalog);
      }
    }
    catch (SAXException e) {
      problemHandler.add(new Problem(Problem.LEVEL_ERROR,
          "application descriptor contains invalid xml", e));
    }
    catch (IOException e) {
      problemHandler.add(new Problem(Problem.LEVEL_ERROR, "application descriptor is not readable",
          e));
    }
    finally {
      // reset error handling behavior
      builder.setErrorHandler(null);
    }
  }

  // configuration elements
  // //////////////////////////////////////////////////////////////

  protected void readScope(Element scopeElem, ScopeDescriptor scope) {
    // name - there can be unnamed scope descriptors
    scope.setName(XmlUtil.getAttribute(scopeElem, BpelConstants.ATTR_NAME));

    // partner links
    Element partnerLinksElem = XmlUtil.getElement(scopeElem, BpelConstants.NS_DEPLOYMENT_DESCRIPTOR,
        BpelConstants.ELEM_PARTNER_LINKS);
    if (partnerLinksElem != null)
      readPartnerLinks(partnerLinksElem, scope);

    // inner scopes
    Element scopesElem = XmlUtil.getElement(scopeElem, BpelConstants.NS_DEPLOYMENT_DESCRIPTOR,
        BpelConstants.ELEM_SCOPES);
    if (scopesElem != null)
      readScopes(scopesElem, scope);
  }

  protected void readPartnerLinks(Element partnerLinksElem, ScopeDescriptor scope) {
    for (Iterator i = XmlUtil.getElements(partnerLinksElem, BpelConstants.NS_DEPLOYMENT_DESCRIPTOR,
        BpelConstants.ELEM_PARTNER_LINK); i.hasNext();) {
      PartnerLinkDescriptor partnerLink = readPartnerLink((Element) i.next());
      scope.addPartnerLink(partnerLink);
    }
  }

  protected PartnerLinkDescriptor readPartnerLink(Element partnerLinkElem) {
    PartnerLinkDescriptor partnerLink = new PartnerLinkDescriptor();

    // name
    partnerLink.setName(partnerLinkElem.getAttribute(BpelConstants.ATTR_NAME));

    // my role
    Element myRoleElem = XmlUtil.getElement(partnerLinkElem, BpelConstants.NS_DEPLOYMENT_DESCRIPTOR,
        BpelConstants.ELEM_MY_ROLE);
    if (myRoleElem != null)
      partnerLink.setMyRole(readMyRole(myRoleElem));

    // partner role
    Element partnerRoleElem = XmlUtil.getElement(partnerLinkElem, BpelConstants.NS_DEPLOYMENT_DESCRIPTOR,
        BpelConstants.ELEM_PARTNER_ROLE);
    if (partnerRoleElem != null)
      partnerLink.setPartnerRole(readPartnerRole(partnerRoleElem));

    return partnerLink;
  }

  protected MyRoleDescriptor readMyRole(Element myRoleElem) {
    MyRoleDescriptor myRole = new MyRoleDescriptor();

    // partner link handle
    String handle = XmlUtil.getAttribute(myRoleElem, BpelConstants.ATTR_HANDLE);
    if (handle != null)
      myRole.setHandle(handle);

    // service and port
    Attr service = myRoleElem.getAttributeNode(BpelConstants.ATTR_SERVICE);
    String port = XmlUtil.getAttribute(myRoleElem, BpelConstants.ATTR_PORT);

    if (service != null) {
      myRole.setService(XmlUtil.getQNameValue(service));
      if (port != null)
        myRole.setPort(port);
    }
    else if (port != null)
      problemHandler.add(new ParseProblem("port is not combined with service", myRoleElem));

    return myRole;
  }

  protected PartnerRoleDescriptor readPartnerRole(Element partnerRoleElem) {
    PartnerRoleDescriptor partnerRole = new PartnerRoleDescriptor();

    // initiate
    String initiateValue = XmlUtil.getAttribute(partnerRoleElem, BpelConstants.ATTR_INITIATE);
    InitiateMode initiateMode = InitiateMode.valueOf(initiateValue);
    partnerRole.setInitiateMode(initiateMode);

    // service reference
    Element referenceElem = XmlUtil.getElement(partnerRoleElem);
    if (referenceElem != null) {
      if (InitiateMode.STATIC.equals(initiateMode)) {
        EndpointReference endpointRef = EndpointReference.readServiceRef(referenceElem);
        partnerRole.setEndpointReference(endpointRef);
      }
      else {
        problemHandler.add(new ParseProblem("not treating element as endpoint reference, "
            + "since initiate mode is not static", referenceElem));
      }
    }
    else if (InitiateMode.STATIC.equals(initiateMode))
      problemHandler.add(new ParseProblem("missing endpoint reference", partnerRoleElem));

    return partnerRole;
  }

  protected void readScopes(Element scopesElem, ScopeDescriptor parentScope) {
    for (Iterator i = XmlUtil.getElements(scopesElem, BpelConstants.NS_DEPLOYMENT_DESCRIPTOR,
        BpelConstants.ELEM_SCOPE); i.hasNext();) {
      ScopeDescriptor scope = new ScopeDescriptor();
      parentScope.addScope(scope);

      readScope((Element) i.next(), scope);
    }
  }

  protected ServiceCatalog readServiceCatalogs(Element catalogsElem, String documentBaseURI) {
    ArrayList catalogs = new ArrayList();
    for (Iterator i = XmlUtil.getElements(catalogsElem, BpelConstants.NS_DEPLOYMENT_DESCRIPTOR); i.hasNext();) {
      Element catalogElem = (Element) i.next();
      ServiceCatalogReader catalogReader = getCatalogReader(catalogElem.getLocalName());
      if (catalogReader != null) {
        ServiceCatalog catalog = catalogReader.read(catalogElem, documentBaseURI);
        catalogs.add(catalog);
      }
      else
        problemHandler.add(new ParseProblem("unrecognized service catalog", catalogElem));
    }

    ServiceCatalog resultingCatalog;
    switch (catalogs.size()) {
    case 0:
      resultingCatalog = null;
      break;
    case 1:
      resultingCatalog = (ServiceCatalog) catalogs.get(0);
      break;
    default:
      resultingCatalog = new CompositeCatalog(catalogs);
    }
    return resultingCatalog;
  }

  public ProblemHandler getProblemHandler() {
    return problemHandler;
  }

  public void setProblemHandler(ProblemHandler problemHandler) {
    if (problemHandler == null)
      throw new IllegalArgumentException("problem handler cannot be null");

    this.problemHandler = problemHandler;
  }

  protected static ServiceCatalogReader getCatalogReader(String name) {
    return (ServiceCatalogReader) catalogReaders.get(name);
  }

  private static Map readCatalogReaders() {
    // get catalog readers resource
    String resource = JbpmConfiguration.Configs.getString(RESOURCE_CATALOG_READERS);

    // parse catalog readers document
    Element readersElem;
    try {
      // parse xml document
      readersElem = XmlUtil.parseResource(resource);
    }
    catch (SAXException e) {
      log.error("catalog readers document contains invalid xml: " + resource, e);
      return Collections.EMPTY_MAP;
    }
    catch (IOException e) {
      log.error("could not read catalog readers document: " + resource, e);
      return Collections.EMPTY_MAP;
    }

    // walk through catalogReader elements
    HashMap catalogReaders = new HashMap();
    for (Iterator i = XmlUtil.getElements(readersElem, null, "catalogReader"); i.hasNext();) {
      Element readerElem = (Element) i.next();
      String name = readerElem.getAttribute("name");

      // load reader class
      String readerClassName = readerElem.getAttribute("class");
      Class readerClass = ClassLoaderUtil.loadClass(readerClassName);

      // validate reader class
      if (!ServiceCatalogReader.class.isAssignableFrom(readerClass)) {
        log.warn("not a catalog reader: " + readerClassName);
        continue;
      }

      try {
        // instantiate reader
        ServiceCatalogReader reader = (ServiceCatalogReader) readerClass.newInstance();

        // register reader instance
        catalogReaders.put(name, reader);
        log.debug("registered catalog reader: name=" + name + ", class=" + readerClassName);
      }
      catch (InstantiationException e) {
        log.warn("reader class not instantiable: " + readerClassName, e);
      }
      catch (IllegalAccessException e) {
        log.warn("reader class or constructor not public: " + readerClassName, e);
      }
    }
    return catalogReaders;
  }
}