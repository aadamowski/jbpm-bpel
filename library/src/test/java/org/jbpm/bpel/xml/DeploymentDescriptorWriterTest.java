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

import java.util.ArrayList;

import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import org.jbpm.bpel.deploy.DeploymentDescriptor;
import org.jbpm.bpel.deploy.MyRoleDescriptor;
import org.jbpm.bpel.deploy.PartnerLinkDescriptor;
import org.jbpm.bpel.deploy.PartnerRoleDescriptor;
import org.jbpm.bpel.deploy.ScopeDescriptor;
import org.jbpm.bpel.deploy.PartnerRoleDescriptor.InitiateMode;
import org.jbpm.bpel.endpointref.EndpointReference;
import org.jbpm.bpel.endpointref.wsa.WsaConstants;
import org.jbpm.bpel.endpointref.wsa.WsaEndpointReference;
import org.jbpm.bpel.integration.catalog.CompositeCatalog;
import org.jbpm.bpel.integration.catalog.ServiceCatalog;
import org.jbpm.bpel.integration.catalog.UrlCatalog;
import org.jbpm.bpel.xml.util.DatatypeUtil;
import org.jbpm.bpel.xml.util.XmlUtil;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/10/13 02:53:24 $
 */
public class DeploymentDescriptorWriterTest extends TestCase {

  private DeploymentDescriptorWriter writer = DeploymentDescriptorWriter.getInstance();

  public void testFullDescriptor() {
    DeploymentDescriptor deploymentDescriptor = new DeploymentDescriptor();

    DeploymentDescriptorReader reader = new DeploymentDescriptorReader();
    reader.read(deploymentDescriptor, new InputSource(getClass().getResource(
        "bpelDeploymentSample.xml").toString()));

    Element descriptorElem = XmlUtil.createElement(BpelConstants.ELEM_BPEL_DEPLOYMENT);
    writer.write(deploymentDescriptor, descriptorElem);

    assertEquals("ba", descriptorElem.getAttribute(BpelConstants.ATTR_NAME));
  }

  public void testProcessName() {
    final String name = "The Trial";

    DeploymentDescriptor deploymentDescriptor = new DeploymentDescriptor();
    deploymentDescriptor.setName(name);

    Element descriptorElem = XmlUtil.createElement(BpelConstants.ELEM_BPEL_DEPLOYMENT);
    writer.write(deploymentDescriptor, descriptorElem);

    assertEquals(name, descriptorElem.getAttribute(BpelConstants.ATTR_NAME));
  }

  public void testProcessTargetNamespace() {
    DeploymentDescriptor deploymentDescriptor = new DeploymentDescriptor();
    deploymentDescriptor.setTargetNamespace(BpelConstants.NS_EXAMPLES);

    Element descriptorElem = XmlUtil.createElement(BpelConstants.ELEM_BPEL_DEPLOYMENT);
    writer.write(deploymentDescriptor, descriptorElem);

    assertEquals(BpelConstants.NS_EXAMPLES,
        descriptorElem.getAttribute(BpelConstants.ATTR_TARGET_NAMESPACE));
  }

  public void testProcessVersion() {
    final Integer version = new Integer(7);

    DeploymentDescriptor deploymentDescriptor = new DeploymentDescriptor();
    deploymentDescriptor.setVersion(version);

    Element descriptorElem = XmlUtil.createElement(BpelConstants.ELEM_BPEL_DEPLOYMENT);
    writer.write(deploymentDescriptor, descriptorElem);

    assertEquals(version.intValue(),
        Integer.parseInt(descriptorElem.getAttribute(BpelConstants.ATTR_VERSION)));
  }

  public void testScopeName() {
    final String name = "The Arrest";

    ScopeDescriptor scope = new ScopeDescriptor();
    scope.setName(name);

    Element scopeElem = XmlUtil.createElement(BpelConstants.ELEM_SCOPE);
    writer.writeScope(scope, scopeElem);

    assertEquals(name, scopeElem.getAttribute(BpelConstants.ATTR_NAME));
  }

  public void testScopePartnerLinks() {
    PartnerLinkDescriptor partnerLink1 = new PartnerLinkDescriptor();
    partnerLink1.setName("pl1");

    PartnerLinkDescriptor partnerLink2 = new PartnerLinkDescriptor();
    partnerLink2.setName("pl2");

    ScopeDescriptor scope = new ScopeDescriptor();
    scope.addPartnerLink(partnerLink1);
    scope.addPartnerLink(partnerLink2);

    Element scopeElem = XmlUtil.createElement(BpelConstants.ELEM_SCOPE);
    writer.writeScope(scope, scopeElem);

    Element partnerLinksElem = XmlUtil.getElement(scopeElem,
        BpelConstants.NS_DEPLOYMENT_DESCRIPTOR, BpelConstants.ELEM_PARTNER_LINKS);
    assertEquals(2, XmlUtil.countElements(partnerLinksElem, BpelConstants.NS_DEPLOYMENT_DESCRIPTOR,
        BpelConstants.ELEM_PARTNER_LINK));
  }

  public void testScopeScopes() {
    ScopeDescriptor scope = new ScopeDescriptor();
    scope.addScope(new ScopeDescriptor());
    scope.addScope(new ScopeDescriptor());

    Element scopeElem = XmlUtil.createElement(BpelConstants.ELEM_SCOPE);
    writer.writeScope(scope, scopeElem);

    Element scopesElem = XmlUtil.getElement(scopeElem, BpelConstants.NS_DEPLOYMENT_DESCRIPTOR,
        BpelConstants.ELEM_SCOPES);
    assertEquals(2, XmlUtil.countElements(scopesElem, BpelConstants.NS_DEPLOYMENT_DESCRIPTOR,
        BpelConstants.ELEM_SCOPE));
  }

  public void testPartnerLinkName() {
    final String name = "Crime and Punishment";

    PartnerLinkDescriptor partnerLink = new PartnerLinkDescriptor();
    partnerLink.setName(name);

    Element partnerLinksElem = XmlUtil.createElement(BpelConstants.ELEM_PARTNER_LINKS);
    writer.writePartnerLink(partnerLink, partnerLinksElem);

    Element partnerLinkElem = XmlUtil.getElement(partnerLinksElem,
        BpelConstants.NS_DEPLOYMENT_DESCRIPTOR, BpelConstants.ELEM_PARTNER_LINK);
    assertEquals(name, partnerLinkElem.getAttribute(BpelConstants.ATTR_NAME));
  }

  public void testPartnerLinkMyRole() {
    PartnerLinkDescriptor partnerLink = new PartnerLinkDescriptor();
    partnerLink.setMyRole(new MyRoleDescriptor());

    Element partnerLinksElem = XmlUtil.createElement(BpelConstants.ELEM_PARTNER_LINKS);
    writer.writePartnerLink(partnerLink, partnerLinksElem);

    Element partnerLinkElem = XmlUtil.getElement(partnerLinksElem,
        BpelConstants.NS_DEPLOYMENT_DESCRIPTOR, BpelConstants.ELEM_PARTNER_LINK);
    assertNotNull(XmlUtil.getElement(partnerLinkElem, BpelConstants.NS_DEPLOYMENT_DESCRIPTOR,
        BpelConstants.ELEM_MY_ROLE));
  }

  public void testPartnerLinkPartnerRole() {
    PartnerLinkDescriptor partnerLink = new PartnerLinkDescriptor();
    partnerLink.setPartnerRole(new PartnerRoleDescriptor());

    Element partnerLinksElem = XmlUtil.createElement(BpelConstants.ELEM_PARTNER_LINKS);
    writer.writePartnerLink(partnerLink, partnerLinksElem);

    Element partnerLinkElem = XmlUtil.getElement(partnerLinksElem,
        BpelConstants.NS_DEPLOYMENT_DESCRIPTOR, BpelConstants.ELEM_PARTNER_LINK);
    assertNotNull(XmlUtil.getElement(partnerLinkElem, BpelConstants.NS_DEPLOYMENT_DESCRIPTOR,
        BpelConstants.ELEM_PARTNER_ROLE));
  }

  public void testMyRoleHandle() {
    final String handle = "CrimeAndPunishment";

    MyRoleDescriptor myRole = new MyRoleDescriptor();
    myRole.setHandle(handle);

    Element partnerLinkElem = XmlUtil.createElement(BpelConstants.ELEM_PARTNER_LINK);
    writer.writeMyRole(myRole, partnerLinkElem);

    Element myRoleElem = XmlUtil.getElement(partnerLinkElem,
        BpelConstants.NS_DEPLOYMENT_DESCRIPTOR, BpelConstants.ELEM_MY_ROLE);
    assertEquals(handle, myRoleElem.getAttribute(BpelConstants.ATTR_HANDLE));
  }

  public void testMyRoleService() {
    final QName service = new QName(BpelConstants.NS_EXAMPLES, "Kurt Wolff Verlag");

    MyRoleDescriptor myRole = new MyRoleDescriptor();
    myRole.setService(service);

    Element partnerLinkElem = XmlUtil.createElement(BpelConstants.ELEM_PARTNER_LINK);
    writer.writeMyRole(myRole, partnerLinkElem);

    Element myRoleElem = XmlUtil.getElement(partnerLinkElem,
        BpelConstants.NS_DEPLOYMENT_DESCRIPTOR, BpelConstants.ELEM_MY_ROLE);
    assertEquals(service,
        XmlUtil.getQNameValue(myRoleElem.getAttributeNode(BpelConstants.ATTR_SERVICE)));
  }

  public void testMyRolePort() {
    final String port = "Munich";

    MyRoleDescriptor myRole = new MyRoleDescriptor();
    myRole.setPort(port);

    Element partnerLinkElem = XmlUtil.createElement(BpelConstants.ELEM_PARTNER_LINK);
    writer.writeMyRole(myRole, partnerLinkElem);

    Element myRoleElem = XmlUtil.getElement(partnerLinkElem,
        BpelConstants.NS_DEPLOYMENT_DESCRIPTOR, BpelConstants.ELEM_MY_ROLE);
    assertEquals(port, myRoleElem.getAttribute(BpelConstants.ATTR_PORT));
  }

  public void testPartnerRoleInitiate() {
    PartnerRoleDescriptor partnerRole = new PartnerRoleDescriptor();
    partnerRole.setInitiateMode(InitiateMode.PULL);

    Element partnerLinkElem = XmlUtil.createElement(BpelConstants.ELEM_PARTNER_LINK);
    writer.writePartnerRole(partnerRole, partnerLinkElem);

    Element partnerRoleElem = XmlUtil.getElement(partnerLinkElem,
        BpelConstants.NS_DEPLOYMENT_DESCRIPTOR, BpelConstants.ELEM_PARTNER_ROLE);
    assertEquals(InitiateMode.PULL.getName(),
        partnerRoleElem.getAttribute(BpelConstants.ATTR_INITIATE));
  }

  public void testPartnerRoleServiceRef() {
    EndpointReference endpointRef = new WsaEndpointReference();
    endpointRef.setAddress(BpelConstants.NS_EXAMPLES);

    PartnerRoleDescriptor partnerRole = new PartnerRoleDescriptor();
    partnerRole.setEndpointReference(endpointRef);

    Element partnerLinkElem = XmlUtil.createElement(BpelConstants.ELEM_PARTNER_LINK);
    writer.writePartnerRole(partnerRole, partnerLinkElem);

    Element partnerRoleElem = XmlUtil.getElement(partnerLinkElem,
        BpelConstants.NS_DEPLOYMENT_DESCRIPTOR, BpelConstants.ELEM_PARTNER_ROLE);
    Element serviceRefElem = XmlUtil.getElement(partnerRoleElem, BpelConstants.NS_SERVICE_REF,
        BpelConstants.ELEM_SERVICE_REF);
    Element endpointRefElem = XmlUtil.getElement(serviceRefElem, WsaConstants.NS_ADDRESSING,
        WsaConstants.ELEM_ENDPOINT_REFERENCE);
    Element addressElem = XmlUtil.getElement(endpointRefElem, WsaConstants.NS_ADDRESSING,
        WsaConstants.ELEM_ADDRESS);
    assertEquals(BpelConstants.NS_EXAMPLES, DatatypeUtil.toString(addressElem));
  }

  public void testSingleServiceCatalog() {
    ServiceCatalog catalog = new UrlCatalog();

    Element descriptorElem = XmlUtil.createElement(BpelConstants.NS_DEPLOYMENT_DESCRIPTOR,
        BpelConstants.ELEM_BPEL_DEPLOYMENT);
    writer.writeServiceCatalogs(catalog, descriptorElem);

    Element catalogsElem = XmlUtil.getElement(descriptorElem,
        BpelConstants.NS_DEPLOYMENT_DESCRIPTOR, BpelConstants.ELEM_SERVICE_CATALOGS);
    assertEquals(1, XmlUtil.countElements(catalogsElem, BpelConstants.NS_DEPLOYMENT_DESCRIPTOR,
        "urlCatalog"));
  }

  public void testCompositeServiceCatalog() {
    ArrayList catalogs = new ArrayList();
    catalogs.add(new UrlCatalog());
    catalogs.add(new UrlCatalog());
    catalogs.add(new UrlCatalog());

    CompositeCatalog catalog = new CompositeCatalog();
    catalog.setCatalogs(catalogs);

    Element descriptorElem = XmlUtil.createElement(BpelConstants.NS_DEPLOYMENT_DESCRIPTOR,
        BpelConstants.ELEM_BPEL_DEPLOYMENT);
    writer.writeServiceCatalogs(catalog, descriptorElem);

    Element catalogsElem = XmlUtil.getElement(descriptorElem,
        BpelConstants.NS_DEPLOYMENT_DESCRIPTOR, BpelConstants.ELEM_SERVICE_CATALOGS);
    assertEquals(3, XmlUtil.countElements(catalogsElem, BpelConstants.NS_DEPLOYMENT_DESCRIPTOR,
        "urlCatalog"));
  }
}
