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
package org.jbpm.bpel.integration.jms;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.naming.InitialContext;
import javax.naming.LinkRef;
import javax.wsdl.Definition;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import org.jbpm.bpel.deploy.DeploymentDescriptor;
import org.jbpm.bpel.graph.basic.Receive;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.integration.def.CorrelationSetDefinition;
import org.jbpm.bpel.integration.def.Correlations;
import org.jbpm.bpel.integration.def.PartnerLinkDefinition;
import org.jbpm.bpel.integration.def.ReceiveAction;
import org.jbpm.bpel.persistence.db.AbstractDbTestCase;
import org.jbpm.bpel.variable.def.VariableDefinition;
import org.jbpm.bpel.variable.exe.MessageValue;
import org.jbpm.bpel.wsdl.xml.WsdlConstants;
import org.jbpm.bpel.wsdl.xml.WsdlUtil;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.bpel.xml.BpelReader;
import org.jbpm.bpel.xml.util.XmlUtil;
import org.jbpm.graph.def.Action;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;
import org.jbpm.instantiation.Delegation;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2008/06/12 08:18:54 $
 */
public abstract class AbstractListenerTestCase extends AbstractDbTestCase {

  protected BpelProcessDefinition processDefinition;
  protected ReceiveAction receiveAction;
  protected IntegrationControl integrationControl;
  protected Session jmsSession;

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
      + "  <portType name='pt'>"
      + "    <operation name='op'>"
      + "      <input message='tns:request'/>"
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
      + " </variables>"
      + " <correlationSets>"
      + "  <correlationSet name='csId' properties='def:idProperty'/>"
      + "  <correlationSet name='csName' properties='def:nameProperty'/>"
      + " </correlationSets>"
      + " <receive partnerLink='pl' operation='op' variable='req' messageExchange='swing'"
      + "  createInstance='yes'>"
      + "  <correlations>"
      + "   <correlation set='csId' initiate='join'/>"
      + "   <correlation set='csName' initiate='yes'/>"
      + "  </correlations>"
      + " </receive>"
      + "</process>";

  protected static final QName ID_PROP = new QName(NS_DEF, "idProperty");
  protected static final String ID_VALUE = "30";

  private static final Object lock = ID_VALUE;
  private static Thread listenerThread;

  protected AbstractListenerTestCase() {
  }

  protected void setUp() throws Exception {
    // set up db
    super.setUp();

    // create process definition
    processDefinition = new BpelProcessDefinition("pd", BpelConstants.NS_EXAMPLES);
    // read wsdl
    Definition def = WsdlUtil.readText(WSDL_TEXT);
    processDefinition.getImportDefinition().addImport(WsdlUtil.createImport(def));
    // read bpel
    BpelReader bpelReader = new BpelReader();
    bpelReader.read(processDefinition, new InputSource(new StringReader(BPEL_TEXT)));
    assertEquals(0, bpelReader.getProblemHandler().getProblemCount());

    // get the receiver
    Receive receive = (Receive) processDefinition.getGlobalScope().getActivity();
    receiveAction = receive.getReceiveAction();
    // intercept reception activity termination
    Event event = new Event(Event.EVENTTYPE_NODE_LEAVE);
    event.addAction(new Action(new Delegation(ReceptionVerifier.class.getName())));
    receive.addEvent(event);

    // deploy process definition and commit changes
    bpelGraphSession.deployProcessDefinition(processDefinition);
    newTransaction();

    // create application descriptor
    DeploymentDescriptor deploymentDescriptor = new DeploymentDescriptor();
    deploymentDescriptor.setName(processDefinition.getName());
    deploymentDescriptor.setTargetNamespace(processDefinition.getTargetNamespace());

    InitialContext initialContext = new InitialContext();
    try {
      // link jms administered objects
      initialContext.bind(IntegrationControl.CONNECTION_FACTORY_NAME, new LinkRef(
          "ConnectionFactory"));
      initialContext.rebind("pl", new LinkRef("queue/testQueue"));

      // configure relation service factory
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

    jmsSession = integrationControl.getJmsConnection().createSession(false,
        Session.CLIENT_ACKNOWLEDGE);
  }

  protected void tearDown() throws Exception {
    // wait until message listener thread ends
    if (listenerThread != null)
      listenerThread.join();

    jmsSession.close();

    // finalize relation service factory
    IntegrationControlHelper.tearDown(integrationControl);

    // tear down db
    super.tearDown();
  }

  public void testReceiveBeforeSend() throws Exception {
    // prepare the listener for reception
    openListener();
    // now send the request
    sendRequest();
    // wait until the reception is verified
    waitForReception();
  }

  public void testReceiveAfterSend() throws Exception {
    // send the request
    sendRequest();
    // now prepare the listener for reception
    openListener();
    // wait until the reception is verified
    waitForReception();
  }

  public void testClose() throws Exception {
    // prepare the listener for reception
    openListener();
    // then close it
    closeListener();
    // send request - the listener should not receive it
    sendRequest();
    // reopen the listener - it should now receive the message
    openListener();
    newTransaction();
    // wait until the reception is verified
    waitForReception();
  }

  protected abstract void openListener() throws JMSException;

  protected abstract void closeListener() throws JMSException;

  protected void sendRequest() throws Exception {
    // create message parts
    HashMap inputParts = new HashMap();
    // simple part
    Element simpleValue = XmlUtil.parseText("<simplePart>wazabi</simplePart>");
    inputParts.put("simplePart", simpleValue);
    // element part
    Element elementValue = XmlUtil.parseText("<sns:surpriseElement xmlns:sns='http://jbpm.org/xsd'>"
        + " <b on=\"true\">true</b>"
        + " <c name=\"venus\"/>"
        + " <d amount=\"20\"/>"
        + " <e>30</e>"
        + "</sns:surpriseElement>");
    inputParts.put("elementPart", elementValue);

    // get connection and destination
    PartnerLinkDefinition partnerLink = receiveAction.getPartnerLink();
    Destination destination = integrationControl.getPartnerLinkEntry(partnerLink).getDestination();

    // create and fill message
    Message message = jmsSession.createObjectMessage(inputParts);
    // set a reply destination so that the outstanding request is created
    message.setJMSReplyTo(destination);
    // set properties
    message.setLongProperty(IntegrationConstants.PARTNER_LINK_ID_PROP, partnerLink.getId());
    message.setStringProperty(IntegrationConstants.OPERATION_NAME_PROP,
        receiveAction.getOperation().getName());
    message.setStringProperty(ID_PROP.getLocalPart(), ID_VALUE);
    // send message
    MessageProducer producer = jmsSession.createProducer(destination);
    producer.send(message);
  }

  protected void waitForReception() throws Exception {
    synchronized (lock) {
      try {
        lock.wait();
      }
      catch (InterruptedException e) {
      }
    }
  }

  public static class ReceptionVerifier implements ActionHandler {

    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext exeContext) throws Exception {
      Token token = exeContext.getToken();
      org.hibernate.Session hbSession = exeContext.getJbpmContext().getSession();
      Receive receive = (Receive) hbSession.load(Receive.class, new Long(token.getNode().getId()));
      ReceiveAction receiveAction = receive.getReceiveAction();

      // variable
      VariableDefinition variable = receiveAction.getVariable();
      MessageValue messageValue = (MessageValue) variable.getValue(token);
      // simple part
      assertNotNull(messageValue.getPart("simplePart"));
      // element part
      assertNotNull(messageValue.getPart("elementPart"));
      // correlation sets
      Correlations correlations = receiveAction.getCorrelations();
      // id cset
      CorrelationSetDefinition set = correlations.getCorrelation("csId").getSet();
      Map properties = set.getInstance(token).getProperties();
      assertEquals(1, properties.size());
      assertEquals(ID_VALUE, properties.get(ID_PROP));
      // name cset
      set = correlations.getCorrelation("csName").getSet();
      properties = set.getInstance(token).getProperties();
      assertEquals(1, properties.size());
      assertEquals("venus", properties.get(new QName(NS_DEF, "nameProperty")));

      listenerThread = Thread.currentThread();
      synchronized (lock) {
        lock.notify();
      }
    }
  }
}
