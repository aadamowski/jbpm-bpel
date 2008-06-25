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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
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
import org.jbpm.util.ClassLoaderUtil;

/**
 * Converts a deployment descriptor {@linkplain DeploymentDescriptor object} to XML format.
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/10/13 02:53:27 $
 */
public class DeploymentDescriptorWriter {

  public static final String RESOURCE_CATALOG_WRITERS = "resource.catalog.writers";

  private static final Log log = LogFactory.getLog(DeploymentDescriptorWriter.class);
  private static final Map catalogWriters = readCatalogWriters();

  private static final DeploymentDescriptorWriter instance = new DeploymentDescriptorWriter();

  protected DeploymentDescriptorWriter() {
  }

  public void write(DeploymentDescriptor deploymentDescriptor, Element descriptorElem) {
    // global scope attributes
    writeScope(deploymentDescriptor, descriptorElem);

    // target namespace
    descriptorElem.setAttribute(BpelConstants.ATTR_TARGET_NAMESPACE,
        deploymentDescriptor.getTargetNamespace());

    // version
    Integer version = deploymentDescriptor.getVersion();
    if (version != null)
      descriptorElem.setAttribute(BpelConstants.ATTR_VERSION, version.toString());

    // service catalogs
    ServiceCatalog serviceCatalog = deploymentDescriptor.getServiceCatalog();
    if (serviceCatalog != null)
      writeServiceCatalogs(serviceCatalog, descriptorElem);
  }

  protected void writeScope(ScopeDescriptor scope, Element scopeElem) {
    // name - there can be unnamed scope descriptors
    String name = scope.getName();
    if (name != null)
      scopeElem.setAttribute(BpelConstants.ATTR_NAME, name);

    // partner links
    Map partnerLinks = scope.getPartnerLinks();
    if (!partnerLinks.isEmpty())
      writePartnerLinks(partnerLinks, scopeElem);

    // inner scopes
    List scopes = scope.getScopes();
    if (!scopes.isEmpty())
      writeScopes(scopes, scopeElem);
  }

  protected void writePartnerLinks(Map partnerLinks, Element scopeElem) {
    Element partnerLinksElem = scopeElem.getOwnerDocument().createElementNS(
        BpelConstants.NS_DEPLOYMENT_DESCRIPTOR, BpelConstants.ELEM_PARTNER_LINKS);
    scopeElem.appendChild(partnerLinksElem);

    for (Iterator i = partnerLinks.values().iterator(); i.hasNext();)
      writePartnerLink((PartnerLinkDescriptor) i.next(), partnerLinksElem);
  }

  protected void writePartnerLink(PartnerLinkDescriptor partnerLink, Element partnerLinksElem) {
    Element partnerLinkElem = partnerLinksElem.getOwnerDocument().createElementNS(
        BpelConstants.NS_DEPLOYMENT_DESCRIPTOR, BpelConstants.ELEM_PARTNER_LINK);
    partnerLinksElem.appendChild(partnerLinkElem);

    // name
    partnerLinkElem.setAttribute(BpelConstants.ATTR_NAME, partnerLink.getName());

    // partner role
    PartnerRoleDescriptor partnerRole = partnerLink.getPartnerRole();
    if (partnerRole != null)
      writePartnerRole(partnerRole, partnerLinkElem);

    // my role
    MyRoleDescriptor myRole = partnerLink.getMyRole();
    if (myRole != null)
      writeMyRole(myRole, partnerLinkElem);
  }

  protected void writePartnerRole(PartnerRoleDescriptor partnerRole, Element partnerLinkElem) {
    Element partnerRoleElem = partnerLinkElem.getOwnerDocument().createElementNS(
        BpelConstants.NS_DEPLOYMENT_DESCRIPTOR, BpelConstants.ELEM_PARTNER_ROLE);
    partnerLinkElem.appendChild(partnerRoleElem);

    // initiate
    InitiateMode initiateMode = partnerRole.getInitiateMode();
    if (initiateMode != null)
      partnerRoleElem.setAttribute(BpelConstants.ATTR_INITIATE, initiateMode.getName());

    // service reference
    EndpointReference serviceRef = partnerRole.getEndpointReference();
    if (serviceRef != null) {
      Element referenceElem = partnerRoleElem.getOwnerDocument().createElementNS(
          BpelConstants.NS_SERVICE_REF, BpelConstants.ELEM_SERVICE_REF);
      partnerRoleElem.appendChild(referenceElem);

      serviceRef.writeServiceRef(referenceElem);
    }
  }

  protected void writeMyRole(MyRoleDescriptor myRole, Element partnerLinkElem) {
    Element myRoleElem = partnerLinkElem.getOwnerDocument().createElementNS(
        BpelConstants.NS_DEPLOYMENT_DESCRIPTOR, BpelConstants.ELEM_MY_ROLE);
    partnerLinkElem.appendChild(myRoleElem);

    // partner link handle
    String handle = myRole.getHandle();
    if (handle != null)
      myRoleElem.setAttribute(BpelConstants.ATTR_HANDLE, handle);

    // service
    QName service = myRole.getService();
    if (service != null) {
      Attr serviceAttr = myRoleElem.getOwnerDocument().createAttribute(BpelConstants.ATTR_SERVICE);
      myRoleElem.setAttributeNode(serviceAttr);
      XmlUtil.setQNameValue(serviceAttr, service);
    }

    // port
    String port = myRole.getPort();
    if (port != null)
      myRoleElem.setAttribute(BpelConstants.ATTR_PORT, port);
  }

  protected void writeScopes(List scopes, Element scopeElem) {
    Document descriptorDoc = scopeElem.getOwnerDocument();
    Element scopesElem = descriptorDoc.createElementNS(BpelConstants.NS_DEPLOYMENT_DESCRIPTOR,
        BpelConstants.ELEM_SCOPES);
    scopeElem.appendChild(scopesElem);

    for (int i = 0, n = scopes.size(); i < n; i++) {
      scopeElem = descriptorDoc.createElementNS(BpelConstants.NS_DEPLOYMENT_DESCRIPTOR,
          BpelConstants.ELEM_SCOPE);
      scopesElem.appendChild(scopeElem);

      writeScope((ScopeDescriptor) scopes.get(i), scopeElem);
    }
  }

  protected void writeServiceCatalogs(ServiceCatalog catalog, Element descriptorElem) {
    Element catalogsElem = descriptorElem.getOwnerDocument().createElementNS(
        BpelConstants.NS_DEPLOYMENT_DESCRIPTOR, BpelConstants.ELEM_SERVICE_CATALOGS);
    descriptorElem.appendChild(catalogsElem);

    if (catalog instanceof CompositeCatalog) {
      List catalogs = ((CompositeCatalog) catalog).getCatalogs();

      for (int i = 0, n = catalogs.size(); i < n; i++) {
        ServiceCatalog leafCatalog = (ServiceCatalog) catalogs.get(i);
        writeCatalog(leafCatalog, catalogsElem);
      }
    }
    else
      writeCatalog(catalog, catalogsElem);
  }

  private void writeCatalog(ServiceCatalog catalog, Element catalogsElem) {
    ServiceCatalogWriter catalogWriter = getCatalogWriter(catalog.getClass());
    if (catalogWriter != null)
      catalogWriter.write(catalog, catalogsElem);
    else
      log.warn("unrecognized service catalog: " + catalog);
  }

  public static DeploymentDescriptorWriter getInstance() {
    return instance;
  }

  protected static ServiceCatalogWriter getCatalogWriter(Class catalogClass) {
    return (ServiceCatalogWriter) catalogWriters.get(catalogClass);
  }

  private static Map readCatalogWriters() {
    // get catalog writers resource
    String resource = JbpmConfiguration.Configs.getString(RESOURCE_CATALOG_WRITERS);

    // parse catalog writers document
    Element writersElem;
    try {
      // parse xml document
      writersElem = XmlUtil.parseResource(resource);
    }
    catch (SAXException e) {
      log.error("catalog writers document contains invalid xml: " + resource, e);
      return Collections.EMPTY_MAP;
    }
    catch (IOException e) {
      log.error("could not read catalog writers document: " + resource, e);
      return Collections.EMPTY_MAP;
    }

    // walk through catalogWriter elements
    HashMap catalogWriters = new HashMap();
    for (Iterator i = XmlUtil.getElements(writersElem, null, "catalogWriter"); i.hasNext();) {
      Element writerElem = (Element) i.next();

      // load catalog class
      String catalogClassName = writerElem.getAttribute("catalog");
      Class catalogClass = ClassLoaderUtil.loadClass(catalogClassName);

      // load writer class
      String writerClassName = writerElem.getAttribute("class");
      Class writerClass = ClassLoaderUtil.loadClass(writerClassName);

      // validate writer class
      if (!ServiceCatalogWriter.class.isAssignableFrom(writerClass)) {
        log.warn("not a catalog writer: " + writerClassName);
        continue;
      }

      try {
        // instantiate writer
        ServiceCatalogWriter writer = (ServiceCatalogWriter) writerClass.newInstance();

        // register writer instance
        catalogWriters.put(catalogClass, writer);
        log.debug("registered catalog writer: catalog="
            + catalogClassName
            + ", class="
            + writerClassName);
      }
      catch (InstantiationException e) {
        log.warn("writer class not instantiable: " + writerClassName, e);
      }
      catch (IllegalAccessException e) {
        log.warn("writer class or constructor not public: " + writerClassName, e);
      }
    }
    return catalogWriters;

  }
}
