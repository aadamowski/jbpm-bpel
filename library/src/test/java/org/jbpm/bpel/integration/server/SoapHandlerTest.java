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
package org.jbpm.bpel.integration.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.naming.InitialContext;
import javax.naming.LinkRef;
import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.xml.namespace.QName;
import javax.xml.rpc.soap.SOAPFaultException;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPMessage;

import org.w3c.dom.Element;

import com.ibm.wsdl.Constants;

import org.jbpm.bpel.deploy.DeploymentDescriptor;
import org.jbpm.bpel.graph.basic.Empty;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.def.ImportDefinition;
import org.jbpm.bpel.graph.scope.Scope;
import org.jbpm.bpel.integration.def.PartnerLinkDefinition;
import org.jbpm.bpel.integration.jms.IntegrationConstants;
import org.jbpm.bpel.integration.jms.IntegrationControl;
import org.jbpm.bpel.integration.jms.IntegrationControlHelper;
import org.jbpm.bpel.integration.jms.JmsIntegrationServiceFactory;
import org.jbpm.bpel.integration.jms.PartnerLinkEntry;
import org.jbpm.bpel.integration.soap.SoapBindConstants;
import org.jbpm.bpel.integration.soap.SoapUtil;
import org.jbpm.bpel.persistence.db.AbstractDbTestCase;
import org.jbpm.bpel.wsdl.PartnerLinkType;
import org.jbpm.bpel.wsdl.xml.WsdlConstants;
import org.jbpm.bpel.wsdl.xml.WsdlUtil;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.bpel.xml.BpelReader;
import org.jbpm.bpel.xml.WsdlConverterTest;
import org.jbpm.bpel.xml.util.DatatypeUtil;
import org.jbpm.bpel.xml.util.XmlUtil;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2008/06/12 08:18:55 $
 */
public class SoapHandlerTest extends AbstractDbTestCase {

  private IntegrationControl integrationControl;
  private Definition definition;
  private long rpcPartnerLinkId;
  private long docPartnerLinkId;

  private static final String WSDL_TEXT = "<definitions targetNamespace='"
      + BpelConstants.NS_EXAMPLES
      + "' xmlns:tns='"
      + BpelConstants.NS_EXAMPLES
      + "' xmlns:xsd='"
      + BpelConstants.NS_XML_SCHEMA
      + "' xmlns:bpws='"
      + BpelConstants.NS_BPEL_1_1
      + "' xmlns:plnk='"
      + WsdlConstants.NS_PLNK_1_1
      + "' xmlns:soap='"
      + com.ibm.wsdl.extensions.soap.SOAPConstants.NS_URI_SOAP
      + "' xmlns='"
      + Constants.NS_URI_WSDL
      + "'>"
      + "  <message name='rpcRequest'>"
      + "    <part name='simplePart' type='xsd:string'/>"
      + "    <part name='complexPart' type='tns:complexType'/>"
      + "  </message>"
      + "  <message name='rpcResponse'>"
      + "    <part name='intPart' type='xsd:int'/>"
      + "    <part name='complexPart' type='tns:complexType'/>"
      + "  </message>"
      + "  <message name='docRequest'>"
      + "    <part name='elementPart' element='tns:requestElement'/>"
      + "  </message>"
      + "  <message name='docResponse'>"
      + "    <part name='elementPart' element='tns:responseElement'/>"
      + "  </message>"
      + "  <message name='failure'>"
      + "    <part name='faultPart' element='tns:faultElement'/>"
      + "  </message>"
      + "  <portType name='rpcPt'>"
      + "    <operation name='op'>"
      + "      <input message='tns:rpcRequest'/>"
      + "      <output message='tns:rpcResponse'/>"
      + "      <fault name='flt' message='tns:failure'/>"
      + "    </operation>"
      + "  </portType>"
      + "  <portType name='docPt'>"
      + "    <operation name='op'>"
      + "      <input message='tns:docRequest'/>"
      + "      <output message='tns:docResponse'/>"
      + "    </operation>"
      + "  </portType>"
      + "  <plnk:partnerLinkType name='plt'>"
      + "    <plnk:role name='r1'>"
      + "      <plnk:portType name='tns:rpcPt'/>"
      + "    </plnk:role>"
      + "    <plnk:role name='r2'>"
      + "      <plnk:portType name='tns:docPt'/>"
      + "    </plnk:role>"
      + "  </plnk:partnerLinkType>"
      + "  <bpws:property name='nameProperty' type='xsd:string'/>"
      + "  <bpws:property name='idProperty' type='xsd:int'/>"
      + "  <bpws:propertyAlias propertyName='tns:nameProperty' messageType='tns:rpcRequest' part='complexPart'"
      + "    query='/complexPart/c/@name' />"
      + "  <bpws:propertyAlias propertyName='tns:idProperty' messageType='tns:rpcRequest' part='complexPart'"
      + "    query='/complexPart/e' />"
      + "  <bpws:propertyAlias propertyName='tns:nameProperty' messageType='tns:docRequest' part='elementPart'"
      + "    query='/tns:requestElement/c/@name' />"
      + "  <bpws:propertyAlias propertyName='tns:idProperty' messageType='tns:docRequest' part='elementPart'"
      + "    query='/tns:requestElement/e' />"
      + "  <binding name='rpcB' type='tns:rpcPt'>"
      + "    <soap:binding style='rpc' transport='http://schemas.xmlsoap.org/soap/http' />"
      + "    <operation name='op'>"
      + "      <soap:operation soapAction='"
      + BpelConstants.NS_EXAMPLES
      + "/rpcPt/op' />"
      + "      <input>"
      + "        <soap:body use='literal' namespace='"
      + BpelConstants.NS_EXAMPLES
      + "' />"
      + "      </input>"
      + "      <output>"
      + "        <soap:body use='literal' namespace='"
      + BpelConstants.NS_EXAMPLES
      + "' />"
      + "      </output>"
      + "      <fault name='flt'>"
      + "        <soap:fault name='flt' use='literal' />"
      + "      </fault>"
      + "    </operation>"
      + "  </binding>"
      + "  <binding name='docB' type='tns:docPt'>"
      + "    <soap:binding style='document' transport='http://schemas.xmlsoap.org/soap/http' />"
      + "    <operation name='op'>"
      + "      <soap:operation soapAction='"
      + BpelConstants.NS_EXAMPLES
      + "/docPt/op' />"
      + "      <input>"
      + "        <soap:body use='literal' />"
      + "      </input>"
      + "      <output>"
      + "        <soap:body use='literal' />"
      + "      </output>"
      + "    </operation>"
      + "  </binding>"
      + "  <service name='s'>"
      + "    <port name='rpcP' binding='tns:rpcB' />"
      + "    <port name='docP' binding='tns:docB' />"
      + "  </service>"
      + "</definitions>";

  private static final QName Q_RPC_PORT_TYPE = new QName(BpelConstants.NS_EXAMPLES, "rpcPt");
  private static final String RPC_PORT = "rpcP";

  private static final QName Q_DOC_PORT_TYPE = new QName(BpelConstants.NS_EXAMPLES, "docPt");
  private static final String DOC_PORT = "docP";

  private static final QName Q_SERVICE = new QName(BpelConstants.NS_EXAMPLES, "s");

  protected void setUp() throws Exception {
    // set up db stuff
    super.setUp();

    // create bpel definition
    BpelProcessDefinition processDefinition = new BpelProcessDefinition("testProcess", BpelConstants.NS_EXAMPLES);

    definition = WsdlUtil.getFactory()
        .newWSDLReader()
        .readWSDL("", WsdlConverterTest.transform(WSDL_TEXT));
    ImportDefinition importDefinition = processDefinition.getImportDefinition();
    importDefinition.addImport(WsdlUtil.createImport(definition));
    new BpelReader().registerPropertyAliases(importDefinition);

    // partner link type
    PartnerLinkType partnerLinkType = importDefinition.getPartnerLinkType(new QName(
        BpelConstants.NS_EXAMPLES, "plt"));

    // rpc partner link
    PartnerLinkDefinition rpcPartnerLink = new PartnerLinkDefinition();
    rpcPartnerLink.setName("rpcPl");
    rpcPartnerLink.setPartnerLinkType(partnerLinkType);
    rpcPartnerLink.setMyRole(partnerLinkType.getFirstRole());

    // doc partner link
    PartnerLinkDefinition docPartnerLink = new PartnerLinkDefinition();
    docPartnerLink.setName("docPl");
    docPartnerLink.setPartnerLinkType(partnerLinkType);
    docPartnerLink.setMyRole(partnerLinkType.getSecondRole());

    // global scope
    Scope globalScope = processDefinition.getGlobalScope();
    globalScope.addPartnerLink(rpcPartnerLink);
    globalScope.addPartnerLink(docPartnerLink);
    globalScope.setActivity(new Empty());

    // deploy process definition
    bpelGraphSession.deployProcessDefinition(processDefinition);
    // save generated plink id
    rpcPartnerLinkId = rpcPartnerLink.getId();
    docPartnerLinkId = docPartnerLink.getId();

    // create application descriptor
    DeploymentDescriptor deploymentDescriptor = new DeploymentDescriptor();
    deploymentDescriptor.setName(processDefinition.getName());

    InitialContext initialContext = new InitialContext();
    try {
      // link jms administered objects
      initialContext.rebind(IntegrationControl.CONNECTION_FACTORY_NAME, new LinkRef(
          "ConnectionFactory"));
      initialContext.rebind("rpcPl", new LinkRef("queue/testQueue"));
      initialContext.rebind("docPl", new LinkRef("queue/testQueue"));

      // configure relation context
      integrationControl = JmsIntegrationServiceFactory.getConfigurationInstance(jbpmConfiguration)
          .getIntegrationControl(processDefinition);
      integrationControl.setDeploymentDescriptor(deploymentDescriptor);
      // bind port entries and lookup destinations
      IntegrationControlHelper.setUp(integrationControl, jbpmContext);

      // unlink jms administered objects
      initialContext.unbind(IntegrationControl.CONNECTION_FACTORY_NAME);
      initialContext.unbind("rpcPl");
      initialContext.unbind("docPl");
    }
    finally {
      initialContext.close();
    }
  }

  protected void tearDown() throws Exception {
    // unbind port entries
    IntegrationControlHelper.tearDown(integrationControl);
    // tear down db stuff
    super.tearDown();
  }

  public void testSendRequest_rpc() throws Exception {
    String requestText = "<env:Envelope xmlns:env='"
        + SOAPConstants.URI_NS_SOAP_ENVELOPE
        + "'>"
        + "<env:Body>"
        + "<tns:op xmlns:tns='"
        + BpelConstants.NS_EXAMPLES
        + "'>"
        + "  <simplePart>wazabi</simplePart>"
        + "  <complexPart>"
        + "    <b on='true'>true</b>"
        + "    <c name='venus'/>"
        + "    <d amount='20'/>"
        + "    <e>30</e>"
        + "  </complexPart>"
        + "</tns:op>"
        + "</env:Body>"
        + "</env:Envelope>";
    SOAPMessage soapMessage = MessageFactory.newInstance().createMessage(null,
        new ByteArrayInputStream(requestText.getBytes()));

    Connection connection = integrationControl.getJmsConnection();
    connection.start();

    Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    try {
      SoapHandler soapHandler = createRpcHandler();
      soapHandler.sendRequest(soapMessage, session, jbpmContext);

      PartnerLinkEntry entry = integrationControl.getPartnerLinkEntry(Q_RPC_PORT_TYPE, Q_SERVICE,
          RPC_PORT);
      MessageConsumer consumer = session.createConsumer(entry.getDestination());
      ObjectMessage message = (ObjectMessage) consumer.receiveNoWait();
      Map requestParts = (Map) message.getObject();

      // simple part
      Element simplePart = (Element) requestParts.get("simplePart");
      assertEquals("simplePart", simplePart.getLocalName());
      assertNull(simplePart.getNamespaceURI());
      assertEquals("wazabi", DatatypeUtil.toString(simplePart));

      // complex part
      Element complexPart = (Element) requestParts.get("complexPart");
      assertEquals("complexPart", complexPart.getLocalName());
      assertNull(complexPart.getNamespaceURI());
      assertTrue(complexPart.hasChildNodes());

      // message properties
      assertEquals(rpcPartnerLinkId,
          message.getLongProperty(IntegrationConstants.PARTNER_LINK_ID_PROP));
      assertEquals("op", message.getStringProperty(IntegrationConstants.OPERATION_NAME_PROP));
      assertEquals("venus", message.getStringProperty("nameProperty"));
      assertEquals("30", message.getStringProperty("idProperty"));
    }
    finally {
      session.close();
    }
  }

  public void testSendRequest_rpc_nil() throws Exception {
    String requestText = "<env:Envelope xmlns:env='"
        + SOAPConstants.URI_NS_SOAP_ENVELOPE
        + "'>"
        + "<env:Body>"
        + "<tns:op xmlns:tns='"
        + BpelConstants.NS_EXAMPLES
        + "' xmlns:xsi='"
        + BpelConstants.NS_XML_SCHEMA_INSTANCE
        + "'>"
        + "  <simplePart xsi:nil='true'>wazabi</simplePart>"
        + "  <complexPart xsi:nil='1'>"
        + "    <b on='true'>true</b>"
        + "    <c name='venus'/>"
        + "    <d amount='20'/>"
        + "    <e>30</e>"
        + "  </complexPart>"
        + "</tns:op>"
        + "</env:Body>"
        + "</env:Envelope>";

    SOAPMessage soapMessage = MessageFactory.newInstance().createMessage(null,
        new ByteArrayInputStream(requestText.getBytes()));

    Connection connection = integrationControl.getJmsConnection();
    connection.start();

    Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    try {
      SoapHandler soapHandler = createRpcHandler();
      soapHandler.sendRequest(soapMessage, session, jbpmContext);

      PartnerLinkEntry entry = integrationControl.getPartnerLinkEntry(Q_RPC_PORT_TYPE, Q_SERVICE,
          RPC_PORT);
      MessageConsumer consumer = session.createConsumer(entry.getDestination());
      ObjectMessage message = (ObjectMessage) consumer.receiveNoWait();
      Map requestParts = (Map) message.getObject();

      // simple part
      Element simplePart = (Element) requestParts.get("simplePart");
      assertTrue(DatatypeUtil.toBoolean(simplePart.getAttributeNS(
          BpelConstants.NS_XML_SCHEMA_INSTANCE, BpelConstants.ATTR_NIL)));
      assertFalse(simplePart.hasChildNodes());

      // complex part
      Element complexPart = (Element) requestParts.get("complexPart");
      assertTrue(DatatypeUtil.toBoolean(complexPart.getAttributeNS(
          BpelConstants.NS_XML_SCHEMA_INSTANCE, BpelConstants.ATTR_NIL)));
      assertFalse(complexPart.hasChildNodes());
    }
    finally {
      session.close();
    }
  }

  private SoapHandler createRpcHandler() {
    return createHandler(RPC_PORT);
  }

  public void testSendRequest_doc() throws Exception {
    String requestText = "<env:Envelope xmlns:env='"
        + SOAPConstants.URI_NS_SOAP_ENVELOPE
        + "'>"
        + "<env:Body>"
        + "<tns:requestElement a='mars' xmlns:tns='"
        + BpelConstants.NS_EXAMPLES
        + "'>"
        + "  <b on='true'>true</b>"
        + "  <c name='venus'/>"
        + "  <d amount='20'/>"
        + "  <e>30</e>"
        + "</tns:requestElement>"
        + "</env:Body>"
        + "</env:Envelope>";

    SOAPMessage soapMessage = MessageFactory.newInstance().createMessage(null,
        new ByteArrayInputStream(requestText.getBytes()));

    Connection connection = integrationControl.getJmsConnection();
    connection.start();

    Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    try {
      SoapHandler soapHandler = createDocHandler();
      soapHandler.sendRequest(soapMessage, session, jbpmContext);

      PartnerLinkEntry entry = integrationControl.getPartnerLinkEntry(Q_DOC_PORT_TYPE, Q_SERVICE,
          DOC_PORT);
      MessageConsumer consumer = session.createConsumer(entry.getDestination());
      ObjectMessage message = (ObjectMessage) consumer.receiveNoWait();
      Map requestParts = (Map) message.getObject();

      // element part
      Element elementPart = (Element) requestParts.get("elementPart");
      assertEquals("requestElement", elementPart.getLocalName());
      assertEquals(BpelConstants.NS_EXAMPLES, elementPart.getNamespaceURI());
      assertTrue(elementPart.hasAttribute("a"));
      assertTrue(elementPart.hasChildNodes());

      // message properties
      assertEquals(docPartnerLinkId,
          message.getLongProperty(IntegrationConstants.PARTNER_LINK_ID_PROP));
      assertEquals("op", message.getStringProperty(IntegrationConstants.OPERATION_NAME_PROP));
      assertEquals("venus", message.getStringProperty("nameProperty"));
      assertEquals("30", message.getStringProperty("idProperty"));
    }
    finally {
      session.close();
    }
  }

  public void testSendRequest_doc_nil() throws Exception {
    String requestText = "<env:Envelope xmlns:env='"
        + SOAPConstants.URI_NS_SOAP_ENVELOPE
        + "'>"
        + "<env:Body>"
        + "<tns:requestElement a='mars' xsi:nil='1' xmlns:tns='"
        + BpelConstants.NS_EXAMPLES
        + "' xmlns:xsi='"
        + BpelConstants.NS_XML_SCHEMA_INSTANCE
        + "'>"
        + "  <b on='true'>true</b>"
        + "  <c name='venus'/>"
        + "  <d amount='20'/>"
        + "  <e>30</e>"
        + "</tns:requestElement>"
        + "</env:Body>"
        + "</env:Envelope>";

    SOAPMessage soapMessage = MessageFactory.newInstance().createMessage(null,
        new ByteArrayInputStream(requestText.getBytes()));

    Connection connection = integrationControl.getJmsConnection();
    connection.start();

    Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    try {
      SoapHandler soapHandler = createDocHandler();
      soapHandler.sendRequest(soapMessage, session, jbpmContext);

      PartnerLinkEntry entry = integrationControl.getPartnerLinkEntry(Q_DOC_PORT_TYPE, Q_SERVICE,
          DOC_PORT);
      MessageConsumer consumer = session.createConsumer(entry.getDestination());
      ObjectMessage message = (ObjectMessage) consumer.receiveNoWait();
      Map requestParts = (Map) message.getObject();

      // element part
      Element elementPart = (Element) requestParts.get("elementPart");
      assertTrue(DatatypeUtil.toBoolean(elementPart.getAttributeNS(
          BpelConstants.NS_XML_SCHEMA_INSTANCE, BpelConstants.ATTR_NIL)));
      assertTrue(elementPart.hasAttribute("a"));
      assertFalse(elementPart.hasChildNodes());
    }
    finally {
      session.close();
    }
  }

  private SoapHandler createDocHandler() {
    return createHandler(DOC_PORT);
  }

  public void testReceiveResponse_output_rpc() throws Exception {
    // prepare messaging objects
    Connection connection = integrationControl.getJmsConnection();
    Session session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
    try {
      Destination replyTo = integrationControl.getIntegrationServiceFactory().getResponseDestination();
      MessageProducer producer = session.createProducer(replyTo);
      // create parts map
      Map responseParts = createOutputRpcParts();
      // send message to queue
      ObjectMessage jmsResponse = session.createObjectMessage((Serializable) responseParts);
      // use current time in place of the request id
      String requestId = Long.toString(System.currentTimeMillis());
      jmsResponse.setJMSCorrelationID(requestId);
      producer.send(jmsResponse);

      // receive the above message
      SoapHandler soapHandler = createRpcHandler();
      jmsResponse = soapHandler.receiveResponse(session, replyTo, requestId, jbpmContext);
      responseParts = (Map) jmsResponse.getObject();

      // simple part
      Element intPart = (Element) responseParts.get("intPart");
      assertEquals("intPart", intPart.getLocalName());
      assertNull(intPart.getNamespaceURI());
      // value
      assertEquals("2020", DatatypeUtil.toString(intPart));

      // complex part
      Element complexPart = (Element) responseParts.get("complexPart");
      assertEquals("complexPart", complexPart.getLocalName());
      assertNull(complexPart.getNamespaceURI());
      // attributes
      assertEquals("hi", complexPart.getAttribute("attributeOne"));
      assertEquals("ho", complexPart.getAttribute("attributeTwo"));
      // child elements
      Element one = XmlUtil.getElement(complexPart, "urn:uriOne", "elementOne");
      assertEquals("ram", DatatypeUtil.toString(one));
      Element two = XmlUtil.getElement(complexPart, "urn:uriTwo", "elementTwo");
      assertEquals("ones", DatatypeUtil.toString(two));
    }
    finally {
      session.close();
    }
  }

  private static Map createOutputRpcParts() throws Exception {
    // create content
    // simple
    Element simpleElem = XmlUtil.parseText("<intPart>2020</intPart>");
    // complex
    Element complexElem = XmlUtil.parseText("<complexPart attributeOne='hi' attributeTwo='ho'>"
        + " <ns1:elementOne xmlns:ns1='urn:uriOne'>ram</ns1:elementOne>"
        + " <elementTwo xmlns='urn:uriTwo'>ones</elementTwo>"
        + "</complexPart>");
    // populate parts map
    HashMap responseParts = new HashMap();
    responseParts.put("intPart", simpleElem);
    responseParts.put("complexPart", complexElem);
    return responseParts;
  }

  public void testReceiveResponse_output_doc() throws Exception {
    // prepare messaging objects
    Connection connection = integrationControl.getJmsConnection();
    Session session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
    try {
      Destination replyTo = integrationControl.getIntegrationServiceFactory().getResponseDestination();
      MessageProducer producer = session.createProducer(replyTo);
      // create parts map
      Map responseParts = createOutputDocParts();
      // send message to queue
      ObjectMessage jmsResponse = session.createObjectMessage((Serializable) responseParts);
      // use current time in place of the request id
      String requestId = Long.toString(System.currentTimeMillis());
      jmsResponse.setJMSCorrelationID(requestId);
      producer.send(jmsResponse);

      // receive the above message
      SoapHandler soapHandler = createRpcHandler();
      jmsResponse = soapHandler.receiveResponse(session, replyTo, requestId, jbpmContext);
      responseParts = (Map) jmsResponse.getObject();

      // element part
      Element elementPart = (Element) responseParts.get("elementPart");
      assertEquals("responseElement", elementPart.getLocalName());
      assertEquals(BpelConstants.NS_EXAMPLES, elementPart.getNamespaceURI());
      // attributes
      assertEquals("hi", elementPart.getAttribute("attributeOne"));
      assertEquals("ho", elementPart.getAttribute("attributeTwo"));
      // child elements
      Element one = XmlUtil.getElement(elementPart, "urn:uriOne", "elementOne");
      assertEquals("ram", DatatypeUtil.toString(one));
      Element two = XmlUtil.getElement(elementPart, "urn:uriTwo", "elementTwo");
      assertEquals("ones", DatatypeUtil.toString(two));
    }
    finally {
      session.close();
    }
  }

  private static Map createOutputDocParts() throws Exception {
    // create content
    Element elem = XmlUtil.parseText("<tns:responseElement attributeOne='hi' attributeTwo='ho' xmlns:tns='"
        + BpelConstants.NS_EXAMPLES
        + "'>"
        + " <ns1:elementOne xmlns:ns1='urn:uriOne'>ram</ns1:elementOne>"
        + " <elementTwo xmlns='urn:uriTwo'>ones</elementTwo>"
        + "</tns:responseElement>");
    // populate parts map
    return Collections.singletonMap("elementPart", elem);
  }

  public void testWriteOutput_saaj_rpc() throws Exception {
    SOAPMessage soapMessage = writeRpcOutput();

    // soap body
    SOAPBody bodyElem = soapMessage.getSOAPBody();
    // operation wrapper
    SOAPElement operationElem = SoapUtil.getElement(bodyElem, BpelConstants.NS_EXAMPLES,
        "opResponse");

    // simple part
    SOAPElement intPart = SoapUtil.getElement(operationElem, "intPart");
    // value
    assertEquals("2020", intPart.getValue());

    // complex part
    SOAPElement complexPart = SoapUtil.getElement(operationElem, "complexPart");
    // attributes
    assertEquals("hi", complexPart.getAttribute("attributeOne"));
    assertEquals("ho", complexPart.getAttribute("attributeTwo"));
    // child elements
    SOAPElement one = SoapUtil.getElement(complexPart, "urn:uriOne", "elementOne");
    assertEquals("ram", one.getValue());
    SOAPElement two = SoapUtil.getElement(complexPart, "urn:uriTwo", "elementTwo");
    assertEquals("ones", two.getValue());
  }

  public void testWriteOutput_dom_rpc() throws Exception {
    SOAPMessage soapMessage = writeRpcOutput();

    Element envelopeElem = writeAndRead(soapMessage);
    // soap body
    Element bodyElem = XmlUtil.getElement(envelopeElem, SOAPConstants.URI_NS_SOAP_ENVELOPE, "Body");
    // operation wrapper
    Element operationElem = XmlUtil.getElement(bodyElem, BpelConstants.NS_EXAMPLES, "opResponse");

    // simple part
    Element intPart = XmlUtil.getElement(operationElem, "intPart");
    // value
    assertEquals("2020", DatatypeUtil.toString(intPart));

    // complex part
    Element complexPart = XmlUtil.getElement(operationElem, "complexPart");
    // attributes
    assertEquals("hi", complexPart.getAttribute("attributeOne"));
    assertEquals("ho", complexPart.getAttribute("attributeTwo"));
    // child elements
    Element one = XmlUtil.getElement(complexPart, "urn:uriOne", "elementOne");
    assertEquals("ram", DatatypeUtil.toString(one));
    Element two = XmlUtil.getElement(complexPart, "urn:uriTwo", "elementTwo");
    assertEquals("ones", DatatypeUtil.toString(two));
  }

  private SOAPMessage writeRpcOutput() throws Exception {
    SOAPMessage soapMessage = MessageFactory.newInstance().createMessage();
    Map outputParts = createOutputRpcParts();

    SoapHandler soapHandler = createRpcHandler();
    soapHandler.writeOutput("op", soapMessage, outputParts);
    return soapMessage;
  }

  public void testWriteOutput_saaj_doc() throws Exception {
    SOAPMessage soapMessage = writeDocOutput();

    // soap body
    SOAPBody bodyElem = soapMessage.getSOAPBody();

    // element part
    SOAPElement elementPart = SoapUtil.getElement(bodyElem, BpelConstants.NS_EXAMPLES,
        "responseElement");
    // attributes
    assertEquals("hi", elementPart.getAttribute("attributeOne"));
    assertEquals("ho", elementPart.getAttribute("attributeTwo"));
    // child elements
    SOAPElement one = SoapUtil.getElement(elementPart, "urn:uriOne", "elementOne");
    assertEquals("ram", one.getValue());
    SOAPElement two = SoapUtil.getElement(elementPart, "urn:uriTwo", "elementTwo");
    assertEquals("ones", two.getValue());
  }

  public void testWriteOutput_dom_doc() throws Exception {
    SOAPMessage soapMessage = writeDocOutput();

    Element envelopeElem = writeAndRead(soapMessage);
    // soap body
    Element bodyElem = XmlUtil.getElement(envelopeElem, SOAPConstants.URI_NS_SOAP_ENVELOPE, "Body");

    // element part
    Element elementPart = XmlUtil.getElement(bodyElem, BpelConstants.NS_EXAMPLES, "responseElement");
    // attributes
    assertEquals("hi", elementPart.getAttribute("attributeOne"));
    assertEquals("ho", elementPart.getAttribute("attributeTwo"));
    // child elements
    Element one = XmlUtil.getElement(elementPart, "urn:uriOne", "elementOne");
    assertEquals("ram", DatatypeUtil.toString(one));
    Element two = XmlUtil.getElement(elementPart, "urn:uriTwo", "elementTwo");
    assertEquals("ones", DatatypeUtil.toString(two));
  }

  private SOAPMessage writeDocOutput() throws Exception {
    SOAPMessage soapMessage = MessageFactory.newInstance().createMessage();
    Map responseParts = createOutputDocParts();

    SoapHandler soapHandler = createDocHandler();
    soapHandler.writeOutput("op", soapMessage, responseParts);
    return soapMessage;
  }

  public void testWriteFault_saaj() throws Exception {
    SOAPMessage soapMessage = writeFault();

    SOAPBody body = soapMessage.getSOAPBody();
    // SOAPFault fault = body.getFault();
    SOAPElement fault = SoapUtil.getElement(body, SOAPConstants.URI_NS_SOAP_ENVELOPE, "Fault");

    testFaultCode(fault);
    testFaultString(fault);
    testDetail(fault);
  }

  private static void testFaultCode(SOAPElement fault) {
    SOAPElement faultcodeElem = SoapUtil.getElement(fault, "faultcode");
    String codeNamespace = SoapBindConstants.CLIENT_FAULTCODE.getNamespaceURI();
    assertEquals(SoapUtil.getPrefix(codeNamespace, faultcodeElem)
        + ':'
        + SoapBindConstants.CLIENT_FAULTCODE.getLocalPart(), faultcodeElem.getValue());
  }

  private static void testFaultString(SOAPElement fault) {
    SOAPElement faultstringElem = SoapUtil.getElement(fault, "faultstring");
    assertEquals(SoapBindConstants.BUSINESS_FAULTSTRING, faultstringElem.getValue());
  }

  private static void testDetail(SOAPElement fault) {
    SOAPElement detail = SoapUtil.getElement(fault, "detail");
    SOAPElement detailEntry = SoapUtil.getElement(detail, BpelConstants.NS_EXAMPLES, "faultElement");
    testDetailEntry(detailEntry);
  }

  private static void testDetailEntry(SOAPElement detailEntry) {
    SOAPElement code = SoapUtil.getElement(detailEntry, "code");
    assertEquals("100", DatatypeUtil.toString(code));

    SOAPElement description = SoapUtil.getElement(detailEntry, "description");
    assertEquals("unknown problem", DatatypeUtil.toString(description));
  }

  public void testWriteFault_dom() throws Exception {
    SOAPMessage soapMessage = writeFault();

    Element envelope = writeAndRead(soapMessage);
    Element body = XmlUtil.getElement(envelope, SOAPConstants.URI_NS_SOAP_ENVELOPE, "Body");
    Element fault = XmlUtil.getElement(body, SOAPConstants.URI_NS_SOAP_ENVELOPE, "Fault");

    Element faultcode = XmlUtil.getElement(fault, "faultcode");
    assertEquals(XmlUtil.getPrefix(SoapBindConstants.CLIENT_FAULTCODE.getNamespaceURI(), faultcode)
        + ':'
        + SoapBindConstants.CLIENT_FAULTCODE.getLocalPart(), DatatypeUtil.toString(faultcode));

    Element faultstring = XmlUtil.getElement(fault, "faultstring");
    assertEquals(SoapBindConstants.BUSINESS_FAULTSTRING, DatatypeUtil.toString(faultstring));

    Element detail = XmlUtil.getElement(fault, "detail");
    Element faultElement = XmlUtil.getElement(detail, BpelConstants.NS_EXAMPLES, "faultElement");

    Element code = XmlUtil.getElement(faultElement, "code");
    assertEquals("100", DatatypeUtil.toString(code));

    Element description = XmlUtil.getElement(faultElement, "description");
    assertEquals("unknown problem", DatatypeUtil.toString(description));
  }

  private SoapHandler createHandler(String portName) {
    Port port = definition.getService(Q_SERVICE).getPort(portName);
    return new SoapHandler(integrationControl, Q_SERVICE, port);
  }

  private SOAPMessage writeFault() throws Exception {
    // soap message
    SOAPMessage soapMessage = MessageFactory.newInstance().createMessage();

    // fault exception
    SOAPFaultException faultException = new SOAPFaultException(SoapBindConstants.CLIENT_FAULTCODE,
        SoapBindConstants.BUSINESS_FAULTSTRING, null, null);

    // fault parts
    Element faultElem = XmlUtil.parseText("<faultElement xmlns='"
        + BpelConstants.NS_EXAMPLES
        + "'>"
        + " <code xmlns=''>100</code>"
        + " <description xmlns=''>unknown problem</description>"
        + "</faultElement>");
    Map faultParts = Collections.singletonMap("faultPart", faultElem);

    SoapHandler soapHandler = createRpcHandler();
    soapHandler.writeFault("op", soapMessage, "flt", faultParts, faultException);
    return soapMessage;
  }

  private static Element writeAndRead(SOAPMessage soapMessage) throws Exception {
    ByteArrayOutputStream sink = new ByteArrayOutputStream();
    soapMessage.writeTo(sink);
    sink.writeTo(System.out);
    return XmlUtil.parseText(sink.toString());
  }
}
