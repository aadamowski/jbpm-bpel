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

import javax.wsdl.Definition;
import javax.wsdl.Operation;
import javax.wsdl.PortType;
import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.def.CompositeActivity;
import org.jbpm.bpel.graph.def.ImportDefinition;
import org.jbpm.bpel.graph.scope.Scope;
import org.jbpm.bpel.integration.def.PartnerLinkDefinition;
import org.jbpm.bpel.variable.def.VariableDefinition;
import org.jbpm.bpel.wsdl.PartnerLinkType;
import org.jbpm.bpel.wsdl.Property;
import org.jbpm.bpel.wsdl.xml.WsdlConstants;
import org.jbpm.bpel.wsdl.xml.WsdlUtil;
import org.jbpm.bpel.xml.util.XmlUtil;

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/10/13 02:53:24 $
 */
public abstract class AbstractReaderTestCase extends TestCase {

  protected BpelReader reader;
  protected BpelProcessDefinition processDefinition;
  protected TestScope scope;
  protected PartnerLinkDefinition partnerLink;
  protected Operation operation;
  protected PortType partnerPortType;
  protected PortType myPortType;
  protected VariableDefinition messageVariable;
  protected Property p1, p2, p3;

  private JbpmContext jbpmContext;

  private static final String WSDL_TEXT = "<definitions targetNamespace='http://manufacturing.org/wsdl/purchase'"
      + " xmlns:plnk='"
      + WsdlConstants.NS_PLNK
      + "' xmlns:vprop='"
      + WsdlConstants.NS_VPROP
      + "' xmlns:xsd='http://www.w3.org/2001/XMLSchema'"
      + " xmlns:tns='http://manufacturing.org/wsdl/purchase'"
      + " xmlns='http://schemas.xmlsoap.org/wsdl/'>"
      + " <message name='aQName'>"
      + "  <part name='p' type='xsd:int' />"
      + " </message>"
      + " <portType name='ppt'>"
      + "  <operation name='o'>"
      + "   <input message='tns:aQName'/>"
      + "  </operation>"
      + "  <operation name='o2'>"
      + "   <input message='tns:aQName'/>"
      + "   <output message='tns:aQName'/>"
      + "  </operation>"
      + " </portType>"
      + " <portType name='mpt'>"
      + "  <operation name='o'>"
      + "   <input message='tns:aQName'/>"
      + "   <output message='tns:aQName'/>"
      + "  </operation>"
      + " </portType>"
      + " <plnk:partnerLinkType name='aPartnerLinkType'>"
      + "  <plnk:role name='role1' portType='tns:ppt'/>"
      + "  <plnk:role name='role2' portType='tns:mpt'/>"
      + " </plnk:partnerLinkType>"
      + " <vprop:property name='p1' type='xsd:int'/>"
      + " <vprop:property name='p2' type='xsd:string'/>"
      + " <vprop:property name='p3' type='xsd:string'/>"
      + " <vprop:propertyAlias propertyName='tns:p1' "
      + "   messageType='tns:aQName' part='p'/>"
      + "</definitions>";
  protected static final String NS_TNS = "http://manufacturing.org/wsdl/purchase";

  protected void setUp() throws Exception {
    /*
     * the reader accesses the jbpm configuration, so create a context before
     * creating the reader to avoid loading another configuration from the
     * default resource
     */
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.getInstance("org/jbpm/bpel/graph/exe/test.jbpm.cfg.xml");
    jbpmContext = jbpmConfiguration.createJbpmContext();

    reader = new BpelReader();
    processDefinition = new BpelProcessDefinition();
    scope = new TestScope();
    processDefinition.addNode(scope);
  }

  protected void tearDown() throws Exception {
    jbpmContext.close();
  }

  protected void initMessageProperties() throws Exception {
    // read wsdl
    Definition def = WsdlUtil.readText(WSDL_TEXT);
    // set up importDefinition
    ImportDefinition importDefinition = processDefinition.getImportDefinition();
    importDefinition.addImport(WsdlUtil.createImport(def));
    reader.registerPropertyAliases(importDefinition);
    // partner link types & port types
    PartnerLinkType plinkType = importDefinition.getPartnerLinkType(new QName(NS_TNS,
        "aPartnerLinkType"));
    partnerPortType = plinkType.getFirstRole().getPortType();
    myPortType = plinkType.getSecondRole().getPortType();
    operation = partnerPortType.getOperation("o", null, null);
    // message properties
    p1 = importDefinition.getProperty(new QName(NS_TNS, "p1"));
    p2 = importDefinition.getProperty(new QName(NS_TNS, "p2"));
    p3 = importDefinition.getProperty(new QName(NS_TNS, "p3"));
    // message variable
    messageVariable = new VariableDefinition();
    messageVariable.setName("iv");
    messageVariable.setType(importDefinition.getMessageType(new QName(NS_TNS, "aQName")));
    scope.addVariable(messageVariable);
    // partner link
    partnerLink = new PartnerLinkDefinition();
    partnerLink.setName("aPartner");
    partnerLink.setPartnerLinkType(plinkType);
    partnerLink.setMyRole(plinkType.getSecondRole());
    scope.addPartnerLink(partnerLink);
  }

  protected Element parseAsBpelElement(String text) throws SAXException {
    String textToParse = "<parent xmlns='" + BpelConstants.NS_BPEL + "'>" + text + "</parent>";
    return (Element) XmlUtil.parseText(textToParse).getFirstChild();
  }

  protected Activity readActivity(String xml) throws SAXException {
    Element element = parseAsBpelElement(xml);
    return readActivity(element, scope);
  }

  protected Activity readActivity(Element element, CompositeActivity parent) {
    ActivityReader activityReader = reader.getActivityReader(element.getLocalName());
    Activity activity = activityReader.read(element, parent);
    return activity;
  }

  static class TestScope extends Scope {

    public boolean initial = false;

    private static final long serialVersionUID = 1L;

    public boolean isChildInitial(Activity child) {
      return initial;
    }
  }
}
