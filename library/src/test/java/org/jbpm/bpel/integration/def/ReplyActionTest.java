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
package org.jbpm.bpel.integration.def;

import java.io.StringReader;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.MessageConsumer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.naming.InitialContext;
import javax.naming.LinkRef;
import javax.wsdl.Definition;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import org.jbpm.bpel.deploy.DeploymentDescriptor;
import org.jbpm.bpel.graph.basic.Receive;
import org.jbpm.bpel.graph.basic.Reply;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.def.ImportDefinition;
import org.jbpm.bpel.graph.exe.ScopeInstance;
import org.jbpm.bpel.graph.struct.Sequence;
import org.jbpm.bpel.integration.exe.CorrelationSetInstance;
import org.jbpm.bpel.integration.jms.IntegrationConstants;
import org.jbpm.bpel.integration.jms.IntegrationControl;
import org.jbpm.bpel.integration.jms.IntegrationControlHelper;
import org.jbpm.bpel.integration.jms.JmsIntegrationServiceFactory;
import org.jbpm.bpel.integration.jms.OutstandingRequest;
import org.jbpm.bpel.persistence.db.AbstractDbTestCase;
import org.jbpm.bpel.variable.exe.MessageValue;
import org.jbpm.bpel.wsdl.xml.WsdlConstants;
import org.jbpm.bpel.wsdl.xml.WsdlUtil;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.bpel.xml.BpelReader;
import org.jbpm.bpel.xml.util.DatatypeUtil;
import org.jbpm.bpel.xml.util.XmlUtil;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2008/06/12 08:18:54 $
 */
public class ReplyActionTest extends AbstractDbTestCase {

  private BpelProcessDefinition processDefinition;
  private Token token;

  private IntegrationControl integrationControl;
  private Destination portDestination;

  private static final String NS_DEF = BpelConstants.NS_EXAMPLES + "/wsdl";
  private static final String WSDL_TEXT = "<definitions targetNamespace='"
      + NS_DEF
      + "' xmlns:tns='"
      + NS_DEF
      + "' xmlns:sns='http://jbpm.org/xsd'"
      + " xmlns:xsd='http://www.w3.org/2001/XMLSchema'"
      + " xmlns:vprop='"
      + WsdlConstants.NS_VPROP
      + "' xmlns:plnk='"
      + WsdlConstants.NS_PLNK
      + "' xmlns='http://schemas.xmlsoap.org/wsdl/'>"
      + "  <message name='request'>"
      + "    <part name='simplePart' type='xsd:string'/>"
      + "    <part name='elementPart' element='sns:surpriseElement'/>"
      + "  </message>"
      + "  <message name='response'>"
      + "    <part name='intPart' type='xsd:int'/>"
      + "    <part name='complexPart' type='sns:complexType'/>"
      + "  </message>"
      + "  <message name='failure'>"
      + "    <part name='faultPart' element='sns:faultElement'/>"
      + "  </message>"
      + "  <portType name='pt'>"
      + "    <operation name='op'>"
      + "      <input message='tns:request'/>"
      + "      <output message='tns:response'/>"
      + "      <fault name='flt' message='tns:failure'/>"
      + "    </operation>"
      + "  </portType>"
      + "  <plnk:partnerLinkType name='plt'>"
      + "    <plnk:role name='r1' portType='tns:pt'/>"
      + "  </plnk:partnerLinkType>"
      + "  <vprop:property name='nameProperty' type='xsd:string'/>"
      + "  <vprop:property name='idProperty' type='xsd:int'/>"
      + "  <vprop:propertyAlias propertyName='tns:nameProperty' messageType='tns:request' part='elementPart'>"
      + "    <vprop:query>c/@name</vprop:query>"
      + "  </vprop:propertyAlias>"
      + "  <vprop:propertyAlias propertyName='tns:idProperty' messageType='tns:request' part='elementPart'>"
      + "    <vprop:query>e</vprop:query>"
      + "  </vprop:propertyAlias>"
      + "  <vprop:propertyAlias propertyName='tns:nameProperty' messageType='tns:response' part='complexPart'>"
      + "    <vprop:query>c/@name</vprop:query>"
      + "  </vprop:propertyAlias>"
      + "  <vprop:propertyAlias propertyName='tns:idProperty' messageType='tns:response' part='intPart' />"
      + "</definitions>";
  private static final String BPEL_TEXT = "<process name='testProcess' targetNamespace='"
      + BpelConstants.NS_EXAMPLES
      + "' xmlns:def='"
      + NS_DEF
      + "' xmlns='"
      + BpelConstants.NS_BPEL
      + "'>"
      + " <partnerLinks>"
      + "  <partnerLink name='pl' partnerLinkType='def:plt' myRole='r1'/>"
      + " </partnerLinks>"
      + " <variables>"
      + "  <variable name='req' messageType='def:request'/>"
      + "  <variable name='rsp' messageType='def:response'/>"
      + "  <variable name='flt' messageType='def:failure'/>"
      + " </variables>"
      + " <correlationSets>"
      + "  <correlationSet name='csId' properties='def:idProperty'/>"
      + "  <correlationSet name='csName' properties='def:nameProperty'/>"
      + " </correlationSets>"
      + " <sequence>"
      + "  <receive name='rec' partnerLink='pl' operation='op' variable='req'"
      + "   messageExchange='swing' createInstance='yes'>"
      + "   <correlations>"
      + "    <correlation set='csId' initiate='yes'/>"
      + "   </correlations>"
      + "  </receive>"
      + "  <reply name='rep-out' partnerLink='pl' operation='op' variable='rsp' "
      + "   messageExchange='swing'>"
      + "   <correlations>"
      + "    <correlation set='csId'/>"
      + "    <correlation set='csName' initiate='yes'/>"
      + "   </correlations>"
      + "  </reply>"
      + "  <reply name='rep-flt' partnerLink='pl' operation='op' variable='flt'"
      + "   messageExchange='swing' faultName='def:flt'/>"
      + " </sequence>"
      + "</process>";

  protected void setUp() throws Exception {
    // set up db
    super.setUp();

    // create process definition
    processDefinition = new BpelProcessDefinition("pd", BpelConstants.NS_EXAMPLES);
    BpelReader bpelReader = new BpelReader();
    // read wsdl
    Definition def = WsdlUtil.readText(WSDL_TEXT);
    ImportDefinition importDefinition = processDefinition.getImportDefinition();
    importDefinition.addImport(WsdlUtil.createImport(def));
    bpelReader.registerPropertyAliases(importDefinition);
    // read bpel
    bpelReader.read(processDefinition, new InputSource(new StringReader(BPEL_TEXT)));

    // deploy process definition and commit changes
    bpelGraphSession.deployProcessDefinition(processDefinition);
    newTransaction();

    // create process instance
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    token = processInstance.getRootToken();
    // initialize global data
    ScopeInstance scopeInstance = processDefinition.getGlobalScope().createInstance(token);
    scopeInstance.initializeData();

    // app descriptor
    DeploymentDescriptor deploymentDescriptor = new DeploymentDescriptor();
    deploymentDescriptor.setName(processDefinition.getName());
    deploymentDescriptor.setTargetNamespace(processDefinition.getTargetNamespace());

    InitialContext initialContext = new InitialContext();
    try {
      // link jms administered objects
      initialContext.rebind(IntegrationControl.CONNECTION_FACTORY_NAME, new LinkRef(
          "ConnectionFactory"));
      initialContext.rebind("pl", new LinkRef("queue/testQueue"));

      // configure relation context
      integrationControl = JmsIntegrationServiceFactory.getConfigurationInstance(jbpmConfiguration)
          .getIntegrationControl(processDefinition);
      integrationControl.setDeploymentDescriptor(deploymentDescriptor);
      IntegrationControlHelper.setUp(integrationControl, jbpmContext);

      // unlink jms administered objects
      initialContext.unbind(IntegrationControl.CONNECTION_FACTORY_NAME);
      initialContext.unbind("pl");
    }
    finally {
      initialContext.close();
    }

    // retrieve the partner link destination
    PartnerLinkDefinition partnerLink = processDefinition.getGlobalScope().getPartnerLink(
        "pl");
    portDestination = integrationControl.getPartnerLinkEntry(partnerLink).getDestination();
  }

  protected void tearDown() throws Exception {
    // unbind port entries
    IntegrationControlHelper.tearDown(integrationControl);
    // tear down db
    super.tearDown();
  }

  public void testReply_response() throws Exception {
    // get the replier
    Sequence seq = (Sequence) processDefinition.getGlobalScope().getActivity();
    Reply reply = (Reply) seq.getNode("rep-out");
    ReplyAction replyAction = reply.getReplyAction();
    // init message variable
    String complexPartValue = "<complexPart>"
        + " <c name='venus'/>"
        + " <e>30</e>"
        + "</complexPart>";
    MessageValue responseValue = (MessageValue) replyAction.getVariable().getValueForAssign(token);
    responseValue.setPart("intPart", "30");
    responseValue.setPart("complexPart", XmlUtil.parseText(complexPartValue));
    // init correlation set
    CorrelationSetInstance idSet = replyAction.getCorrelations()
        .getCorrelation("csId")
        .getSet()
        .getInstance(token);
    idSet.initialize(responseValue);
    // create outstanding request
    OutstandingRequest request = new OutstandingRequest(portDestination, null);
    ReceiveAction receiveAction = ((Receive) seq.getNode("rec")).getReceiveAction();
    integrationControl.addOutstandingRequest(receiveAction, token, request);

    // send reply
    ReceiveAction.getIntegrationService(jbpmContext).reply(replyAction, token);
    // response content
    Map outputParts = (Map) receiveResponse().getObject();
    // simple part
    Element intPart = (Element) outputParts.get("intPart");
    assertNull(intPart.getNamespaceURI());
    assertEquals("intPart", intPart.getLocalName());
    assertEquals("30", DatatypeUtil.toString(intPart));
    // complex part
    Element complexPart = (Element) outputParts.get("complexPart");
    assertEquals(null, complexPart.getNamespaceURI());
    assertEquals("complexPart", complexPart.getLocalName());
    assertNotNull(XmlUtil.getElement(complexPart, "c"));
    assertNotNull(XmlUtil.getElement(complexPart, "e"));
    // correlation sets
    Correlations correlations = replyAction.getCorrelations();
    // id set
    idSet = correlations.getCorrelation("csId").getSet().getInstance(token);
    Map properties = idSet.getProperties();
    assertEquals(1, properties.size());
    assertEquals("30", properties.get(new QName(NS_DEF, "idProperty")));
    // name set
    idSet = correlations.getCorrelation("csName").getSet().getInstance(token);
    properties = idSet.getProperties();
    assertEquals(1, properties.size());
    assertEquals("venus", properties.get(new QName(NS_DEF, "nameProperty")));
  }

  public void testReply_fault() throws Exception {
    // get the replier
    Sequence seq = (Sequence) processDefinition.getGlobalScope().getActivity();
    Reply reply = (Reply) seq.getNode("rep-flt");
    ReplyAction replyAction = reply.getReplyAction();
    // init message variable
    String faultPartValue = "<sns:faultElement xmlns:sns='http://jbpm.org/xsd'>"
        + " <code>100</code>"
        + " <description>unknown problem</description>"
        + "</sns:faultElement>";
    MessageValue faultValue = (MessageValue) replyAction.getVariable().getValueForAssign(token);
    faultValue.setPart("faultPart", faultPartValue);
    // create outstanding request
    OutstandingRequest request = new OutstandingRequest(portDestination, null);
    ReceiveAction receiveAction = ((Receive) seq.getNode("rec")).getReceiveAction();
    integrationControl.addOutstandingRequest(receiveAction, token, request);

    // send reply
    ReceiveAction.getIntegrationService(jbpmContext).reply(replyAction, token);
    // response content
    ObjectMessage response = receiveResponse();
    Map outputParts = (Map) response.getObject();
    // fault part
    Element faultPart = (Element) outputParts.get("faultPart");
    assertEquals("http://jbpm.org/xsd", faultPart.getNamespaceURI());
    assertEquals("faultElement", faultPart.getLocalName());
    // fault name
    assertEquals("flt", response.getStringProperty(IntegrationConstants.FAULT_NAME_PROP));
  }

  private ObjectMessage receiveResponse() throws Exception {
    Session jmsSession = integrationControl.getJmsConnection().createSession(false,
        Session.CLIENT_ACKNOWLEDGE);
    try {
      MessageConsumer consumer = jmsSession.createConsumer(portDestination);
      ObjectMessage response = (ObjectMessage) consumer.receiveNoWait();
      response.acknowledge();
      return response;
    }
    finally {
      jmsSession.close();
    }
  }
}
