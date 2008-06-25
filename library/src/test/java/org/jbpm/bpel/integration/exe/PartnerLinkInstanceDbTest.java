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
package org.jbpm.bpel.integration.exe;

import javax.wsdl.Definition;
import javax.wsdl.PortType;
import javax.wsdl.WSDLException;
import javax.xml.namespace.QName;

import org.jbpm.bpel.endpointref.EndpointReference;
import org.jbpm.bpel.endpointref.wsa.WsaEndpointReference;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.integration.def.PartnerLinkDefinition;
import org.jbpm.bpel.persistence.db.AbstractDbTestCase;
import org.jbpm.bpel.wsdl.PartnerLinkType;
import org.jbpm.bpel.wsdl.PartnerLinkType.Role;
import org.jbpm.bpel.wsdl.xml.WsdlConstants;
import org.jbpm.bpel.wsdl.xml.WsdlUtil;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/08/08 11:10:31 $
 */
public class PartnerLinkInstanceDbTest extends AbstractDbTestCase {

  public void testDefinition() {
    // partner link definition
    PartnerLinkDefinition plinkDefinition = new PartnerLinkDefinition();
    plinkDefinition.setName("plink");

    // process definition
    BpelProcessDefinition processDefinition = new BpelProcessDefinition(
        "definition", BpelConstants.NS_EXAMPLES);
    processDefinition.getGlobalScope().addPartnerLink(plinkDefinition);

    graphSession.saveProcessDefinition(processDefinition);

    // process instance
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    Token token = processInstance.getRootToken();

    // partner link instance
    PartnerLinkInstance plinkInstance = plinkDefinition.createInstance(token);

    processInstance = saveAndReload(processInstance);
    processDefinition = getProcessDefinition(processInstance);
    plinkDefinition = processDefinition.getGlobalScope()
        .getPartnerLink("plink");
    plinkInstance = plinkDefinition.getInstance(processInstance.getRootToken());

    assertEquals(plinkDefinition, plinkInstance.getDefinition());
  }

  public void testEndpointReference() throws WSDLException {
    // port type
    QName portTypeName = new QName(BpelConstants.NS_EXAMPLES, "pt");
    PortType portType = WsdlUtil.getSharedDefinition().createPortType();
    portType.setQName(portTypeName);

    // partner link type
    PartnerLinkType plinkType = (PartnerLinkType) WsdlUtil.getSharedExtensionRegistry()
        .createExtension(Definition.class, WsdlConstants.Q_PARTNER_LINK_TYPE);

    // role
    Role role = plinkType.createRole();
    role.setPortType(portType);
    plinkType.setFirstRole(role);

    // partner link
    PartnerLinkDefinition plink = new PartnerLinkDefinition();
    plink.setName("plink");
    plink.setPartnerLinkType(plinkType);
    plink.setPartnerRole(role);

    BpelProcessDefinition processDefinition = new BpelProcessDefinition(
        "definition", BpelConstants.NS_EXAMPLES);
    processDefinition.getImportDefinition().addPartnerLinkType(plinkType);
    processDefinition.getGlobalScope().addPartnerLink(plink);

    graphSession.saveProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    Token token = processInstance.getRootToken();

    // endpoint reference
    EndpointReference endpointRef = new WsaEndpointReference();
    endpointRef.setScheme("someReferenceScheme");
    endpointRef.setPortTypeName(portTypeName);

    // partner link instance
    PartnerLinkInstance plinkInstance = plink.createInstance(token);
    plinkInstance.setPartnerReference(endpointRef);

    processInstance = saveAndReload(processInstance);
    processDefinition = getProcessDefinition(processInstance);
    plink = processDefinition.getGlobalScope().getPartnerLink("plink");
    plinkInstance = plink.getInstance(processInstance.getRootToken());
    endpointRef = plinkInstance.getPartnerReference();

    assertEquals("someReferenceScheme", endpointRef.getScheme());
    assertEquals(portTypeName, endpointRef.getPortTypeName());
  }

  private BpelProcessDefinition getProcessDefinition(
      ProcessInstance processInstance) {
    long processDefinitionId = processInstance.getProcessDefinition().getId();
    return (BpelProcessDefinition) session.load(BpelProcessDefinition.class,
        new Long(processDefinitionId));
  }
}
