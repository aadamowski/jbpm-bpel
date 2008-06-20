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

import java.io.StringReader;

import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.xml.sax.InputSource;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.bpel.deploy.DeploymentDescriptor;
import org.jbpm.bpel.deploy.MyRoleDescriptor;
import org.jbpm.bpel.deploy.PartnerLinkDescriptor;
import org.jbpm.bpel.deploy.PartnerRoleDescriptor;
import org.jbpm.bpel.deploy.ScopeDescriptor;
import org.jbpm.bpel.endpointref.wsa.WsaConstants;
import org.jbpm.bpel.integration.catalog.CompositeCatalog;
import org.jbpm.bpel.integration.catalog.ServiceCatalog;
import org.jbpm.bpel.integration.catalog.UrlCatalog;
import org.jbpm.bpel.xml.util.XmlUtil;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/10/13 02:53:24 $
 */
public class DeploymentDescriptorReaderTest extends TestCase {

  private DeploymentDescriptorReader reader;
  private JbpmContext jbpmContext;

  protected void setUp() throws Exception {
    /*
     * the reader accesses the jbpm configuration, so create a context before creating the reader to
     * avoid loading another configuration from the default resource
     */
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.getInstance("org/jbpm/bpel/graph/exe/test.jbpm.cfg.xml");
    jbpmContext = jbpmConfiguration.createJbpmContext();

    reader = new DeploymentDescriptorReader();
  }

  protected void tearDown() throws Exception {
    jbpmContext.close();
  }

  public void testFullDescriptor() {
    DeploymentDescriptor deploymentDescriptor = new DeploymentDescriptor();
    reader.read(deploymentDescriptor, new InputSource(getClass().getResource("bpelDeploymentSample.xml")
        .toString()));

    assertEquals(0, reader.getProblemHandler().getProblemCount());
  }

  public void testDescriptorName() throws Exception {
    String text = "<bpelApplication name='pro' />";
    DeploymentDescriptor deploymentDescriptor = new DeploymentDescriptor();
    reader.read(deploymentDescriptor, new InputSource(new StringReader(text)));

    assertEquals(0, reader.getProblemHandler().getProblemCount());
    assertEquals("pro", deploymentDescriptor.getName());
  }

  public void testDescriptorTargetNamespace() {
    String text = "<bpelApplication name='pro' targetNamespace='"
        + BpelConstants.NS_EXAMPLES
        + "' />";
    DeploymentDescriptor deploymentDescriptor = new DeploymentDescriptor();
    reader.read(deploymentDescriptor, new InputSource(new StringReader(text)));

    assertEquals(0, reader.getProblemHandler().getProblemCount());
    assertEquals(BpelConstants.NS_EXAMPLES, deploymentDescriptor.getTargetNamespace());
  }

  public void testDescriptorVersion() throws Exception {
    String text = "<bpelApplication name='pro' version='7' />";
    DeploymentDescriptor deploymentDescriptor = new DeploymentDescriptor();
    reader.read(deploymentDescriptor, new InputSource(new StringReader(text)));

    assertEquals(0, reader.getProblemHandler().getProblemCount());
    assertEquals(7, deploymentDescriptor.getVersion().intValue());
  }

  public void testDescriptorServiceCatalogs() throws Exception {
    String text = "<bpelApplication xmlns='"
        + BpelConstants.NS_DEPLOYMENT_DESCRIPTOR
        + "'>"
        + " <serviceCatalogs>"
        + "  <urlCatalog>"
        + "   <wsdl location='bogus.wsdl' />"
        + "  </urlCatalog>"
        + " </serviceCatalogs>"
        + "</bpelApplication>";
    DeploymentDescriptor deploymentDescriptor = new DeploymentDescriptor();
    reader.read(deploymentDescriptor, new InputSource(new StringReader(text)));

    assertEquals(0, reader.getProblemHandler().getProblemCount());
    assertNotNull(deploymentDescriptor.getServiceCatalog());
  }

  public void testScopeName() throws Exception {
    String text = "<scope name='rfpScope'/>";
    ScopeDescriptor scope = new ScopeDescriptor();
    reader.readScope(XmlUtil.parseText(text), scope);

    assertEquals(0, reader.getProblemHandler().getProblemCount());
    assertEquals("rfpScope", scope.getName());
  }

  public void testScopePartnerLinks() throws Exception {
    String text = "<scope xmlns='"
        + BpelConstants.NS_DEPLOYMENT_DESCRIPTOR
        + "'>"
        + " <partnerLinks>"
        + "  <partnerLink name='pl1'/>"
        + "  <partnerLink name='pl2'/>"
        + " </partnerLinks>"
        + "</scope>";
    ScopeDescriptor scope = new ScopeDescriptor();
    reader.readScope(XmlUtil.parseText(text), scope);

    assertEquals(0, reader.getProblemHandler().getProblemCount());
    assertEquals(2, scope.getPartnerLinks().size());
  }

  public void testScopeScopes() throws Exception {
    String text = "<scope xmlns='"
        + BpelConstants.NS_DEPLOYMENT_DESCRIPTOR
        + "'>"
        + " <scopes>"
        + "  <scope name='s1'/>"
        + "  <scope name='s2'/>"
        + " </scopes>"
        + "</scope>";
    ScopeDescriptor scope = new ScopeDescriptor();
    reader.readScope(XmlUtil.parseText(text), scope);

    assertEquals(0, reader.getProblemHandler().getProblemCount());
    assertEquals(2, scope.getScopes().size());
  }

  public void testPartnerLinkName() throws Exception {
    String text = "<partnerLink name='schedulerPL'/>";
    PartnerLinkDescriptor partnerLink = reader.readPartnerLink(XmlUtil.parseText(text));

    assertEquals(0, reader.getProblemHandler().getProblemCount());
    assertEquals("schedulerPL", partnerLink.getName());
  }

  public void testPartnerLinkMyRole() throws Exception {
    String text = "<partnerLink xmlns='"
        + BpelConstants.NS_DEPLOYMENT_DESCRIPTOR
        + "'>"
        + " <myRole/>"
        + "</partnerLink>";
    PartnerLinkDescriptor partnerLink = reader.readPartnerLink(XmlUtil.parseText(text));

    assertEquals(0, reader.getProblemHandler().getProblemCount());
    assertNotNull(partnerLink.getMyRole());
  }

  public void testPartnerLinkPartnerRole() throws Exception {
    String text = "<partnerLink xmlns='"
        + BpelConstants.NS_DEPLOYMENT_DESCRIPTOR
        + "'>"
        + " <partnerRole initiate='push' />"
        + "</partnerLink>";
    PartnerLinkDescriptor partnerLink = reader.readPartnerLink(XmlUtil.parseText(text));

    assertEquals(0, reader.getProblemHandler().getProblemCount());
    assertNotNull(partnerLink.getPartnerRole());
  }

  public void testMyRoleHandle() throws Exception {
    String text = "<myRole handle='alt'/>";
    MyRoleDescriptor myRole = reader.readMyRole(XmlUtil.parseText(text));

    assertEquals(0, reader.getProblemHandler().getProblemCount());
    assertEquals("alt", myRole.getHandle());
  }

  public void testMyRoleService() throws Exception {
    String text = "<myRole xmlns:ex='"
        + BpelConstants.NS_EXAMPLES
        + "' service='ex:roomservice' />";
    MyRoleDescriptor myRole = reader.readMyRole(XmlUtil.parseText(text));

    assertEquals(0, reader.getProblemHandler().getProblemCount());
    assertEquals(new QName(BpelConstants.NS_EXAMPLES, "roomservice"), myRole.getService());
  }

  public void testMyRolePort() throws Exception {
    String text = "<myRole service='roomservice' port='phone'/>";
    MyRoleDescriptor myRole = reader.readMyRole(XmlUtil.parseText(text));

    assertEquals(0, reader.getProblemHandler().getProblemCount());
    assertEquals("phone", myRole.getPort());
  }

  public void testPartnerRoleInitiate() throws Exception {
    String text = "<partnerRole initiate='pull'/>";
    PartnerRoleDescriptor partnerRole = reader.readPartnerRole(XmlUtil.parseText(text));

    assertEquals(0, reader.getProblemHandler().getProblemCount());
    assertEquals(PartnerRoleDescriptor.InitiateMode.PULL, partnerRole.getInitiateMode());
  }

  public void testPartnerRoleServiceRef() throws Exception {
    String text = "<partnerRole>"
        + " <sref:service-ref xmlns:sref='"
        + BpelConstants.NS_SERVICE_REF
        + "'>"
        + "  <wsa:EndpointReference xmlns:wsa='"
        + WsaConstants.NS_ADDRESSING
        + "'>"
        + "   <wsa:Address>"
        + BpelConstants.NS_EXAMPLES
        + "</wsa:Address>"
        + "  </wsa:EndpointReference>"
        + " </sref:service-ref>"
        + "</partnerRole>";
    PartnerRoleDescriptor partnerRole = reader.readPartnerRole(XmlUtil.parseText(text));

    assertEquals(0, reader.getProblemHandler().getProblemCount());
    assertEquals(BpelConstants.NS_EXAMPLES, partnerRole.getEndpointReference().getAddress());
  }

  public void testSingleServiceCatalog() throws Exception {
    String text = " <serviceCatalogs xmlns='"
        + BpelConstants.NS_DEPLOYMENT_DESCRIPTOR
        + "'>"
        + "  <urlCatalog />"
        + " </serviceCatalogs>";
    ServiceCatalog catalog = reader.readServiceCatalogs(XmlUtil.parseText(text), null);

    assertEquals(0, reader.getProblemHandler().getProblemCount());
    assertTrue(catalog instanceof UrlCatalog);
  }

  public void testCompositeServiceCatalog() throws Exception {
    String text = " <serviceCatalogs xmlns='"
        + BpelConstants.NS_DEPLOYMENT_DESCRIPTOR
        + "'>"
        + "  <urlCatalog />"
        + "  <urlCatalog />"
        + "  <urlCatalog />"
        + " </serviceCatalogs>";
    CompositeCatalog catalog = (CompositeCatalog) reader.readServiceCatalogs(
        XmlUtil.parseText(text), null);

    assertEquals(0, reader.getProblemHandler().getProblemCount());
    assertEquals(3, catalog.getCatalogs().size());
  }
}
