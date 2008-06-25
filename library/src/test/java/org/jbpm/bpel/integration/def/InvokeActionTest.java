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

import java.util.Map;
import java.util.Random;

import javax.naming.InitialContext;
import javax.naming.LinkRef;
import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;

import junit.framework.Test;

import org.w3c.dom.Element;

import org.jbpm.bpel.deploy.DeploymentDescriptor;
import org.jbpm.bpel.deploy.PartnerLinkDescriptor;
import org.jbpm.bpel.deploy.PartnerRoleDescriptor;
import org.jbpm.bpel.deploy.PartnerRoleDescriptor.InitiateMode;
import org.jbpm.bpel.graph.basic.Invoke;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.exe.BpelFaultException;
import org.jbpm.bpel.graph.exe.FaultInstance;
import org.jbpm.bpel.graph.exe.ScopeInstance;
import org.jbpm.bpel.graph.struct.Sequence;
import org.jbpm.bpel.integration.catalog.UrlCatalog;
import org.jbpm.bpel.integration.exe.CorrelationSetInstance;
import org.jbpm.bpel.integration.jms.IntegrationControl;
import org.jbpm.bpel.integration.jms.IntegrationControlHelper;
import org.jbpm.bpel.integration.jms.JmsIntegrationServiceFactory;
import org.jbpm.bpel.persistence.db.AbstractDbTestCase;
import org.jbpm.bpel.tools.ModuleDeployTestSetup;
import org.jbpm.bpel.variable.exe.MessageValue;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.bpel.xml.BpelReader;
import org.jbpm.bpel.xml.util.DatatypeUtil;
import org.jbpm.bpel.xml.util.XmlUtil;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2008/06/12 08:18:54 $
 */
public class InvokeActionTest extends AbstractDbTestCase {

  private BpelProcessDefinition processDefinition;
  private Token token;
  private IntegrationControl integrationControl;

  private static DOMSource processSource;

  protected void setUp() throws Exception {
    // set up db
    super.setUp();

    // create process definition
    processDefinition = new BpelProcessDefinition("pd", BpelConstants.NS_EXAMPLES);
    // read bpel
    BpelReader bpelReader = new BpelReader();
    bpelReader.read(processDefinition, processSource);
    assertEquals(0, bpelReader.getProblemHandler().getProblemCount());

    // deploy process definition and commit changes
    bpelGraphSession.deployProcessDefinition(processDefinition);
    newTransaction();

    // create process instance
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    token = processInstance.getRootToken();
    // initialize global data
    ScopeInstance scopeInstance = processDefinition.getGlobalScope().createInstance(token);
    scopeInstance.initializeData();

    // service catalog
    UrlCatalog catalog = new UrlCatalog();
    catalog.addLocation("http://localhost:8080/translator/text?wsdl");
    // partner role descriptor
    PartnerRoleDescriptor partnerRole = new PartnerRoleDescriptor();
    partnerRole.setInitiateMode(InitiateMode.PULL);
    // partner link descriptor
    PartnerLinkDescriptor partnerLink = new PartnerLinkDescriptor();
    partnerLink.setName("translator");
    partnerLink.setPartnerRole(partnerRole);
    // app descriptor
    DeploymentDescriptor deploymentDescriptor = new DeploymentDescriptor();
    deploymentDescriptor.setName(processDefinition.getName());
    deploymentDescriptor.setServiceCatalog(catalog);
    deploymentDescriptor.addPartnerLink(partnerLink);

    // link jms administered objects
    InitialContext initialContext = new InitialContext();
    try {
      initialContext.rebind("pl", new LinkRef("queue/testQueue"));
      initialContext.rebind(IntegrationControl.CONNECTION_FACTORY_NAME, new LinkRef(
          "ConnectionFactory"));

      // configure relation service factory
      integrationControl = JmsIntegrationServiceFactory.getConfigurationInstance(jbpmConfiguration)
          .getIntegrationControl(processDefinition);
      integrationControl.setDeploymentDescriptor(deploymentDescriptor);
      IntegrationControlHelper.setUp(integrationControl, jbpmContext);

      // unlink jms administered objects
      initialContext.unbind("pl");
      initialContext.unbind(IntegrationControl.CONNECTION_FACTORY_NAME);
    }
    finally {
      initialContext.close();
    }
  }

  protected void tearDown() throws Exception {
    // finalize relation service factory
    IntegrationControlHelper.tearDown(integrationControl);
    // tear down db
    super.tearDown();
  }

  public void testInvoke_oneWay() throws Exception {
    // grab quote invoker
    Sequence seq = (Sequence) processDefinition.getGlobalScope().getActivity();
    InvokeAction quoteInvoker = (InvokeAction) seq.getNode("quote").getAction();
    // call quote operation
    String clientName = createClientName();
    invokeQuote(quoteInvoker, clientName);
    // check correlation set
    CorrelationSetInstance csi = quoteInvoker.getResponseCorrelations()
        .getCorrelation("client")
        .getSet()
        .getInstance(token);
    Map properties = csi.getProperties();
    assertEquals(1, properties.size());
    assertEquals(clientName, properties.get(new QName("http://example.org/translator/client",
        "clientName")));
  }

  public void testInvoke_requestResponse() throws Exception {
    // grab quote invoker
    Sequence seq = (Sequence) processDefinition.getGlobalScope().getActivity();
    InvokeAction quoteInvoker = (InvokeAction) seq.getNode("quote").getAction();
    // call quote operation
    String clientName = createClientName();
    invokeQuote(quoteInvoker, clientName);
    // grab status invoker
    InvokeAction statusInvoker = (InvokeAction) ((Invoke) seq.getNode("status")).getAction();
    // init message variable
    MessageValue messageValue = (MessageValue) statusInvoker.getInputVariable().getValueForAssign(
        token);
    messageValue.setPart("clientName", clientName);
    /*
     * call status operation - quote is an one-way operation, so the status change might not be
     * reflected immediately
     */
    Thread.sleep(500);
    ReceiveAction.getIntegrationService(jbpmContext).invoke(statusInvoker, token);
    // check output variable
    messageValue = (MessageValue) statusInvoker.getOutputVariable().getValue(token);
    assertEquals("received", DatatypeUtil.toString(messageValue.getPart("status")));
  }

  private static String createClientName() {
    return "client" + new Random().nextInt(100000);
  }

  private void invokeQuote(InvokeAction quoteInvoker, String clientName) throws Exception {
    // init message variable
    MessageValue message = (MessageValue) quoteInvoker.getInputVariable().getValueForAssign(token);
    message.setPart("clientName", clientName);
    message.setPart("text", "hi");
    message.setPart("sourceLanguage", "en");
    message.setPart("targetLanguage", "es");
    // consume service
    ReceiveAction.getIntegrationService(jbpmContext).invoke(quoteInvoker, token);
  }

  public void testInvoke_requestFault() throws Exception {
    // grab invoker
    Sequence seq = (Sequence) processDefinition.getGlobalScope().getActivity();
    InvokeAction invokeAction = (InvokeAction) seq.getNode("translate").getAction();
    // init message variable
    MessageValue message = (MessageValue) invokeAction.getInputVariable().getValueForAssign(token);
    message.setPart("text", "hi");
    message.setPart("sourceLanguage", "en");
    message.setPart("targetLanguage", "ja");
    // consume service
    try {
      ReceiveAction.getIntegrationService(jbpmContext).invoke(invokeAction, token);
      fail("invocation should have thrown a fault");
    }
    catch (BpelFaultException e) {
      // check returned fault
      FaultInstance faultInstance = e.getFaultInstance();
      // name
      assertEquals(new QName("http://example.com/translator", "dictionaryNotAvailable"),
          faultInstance.getName());
      // data type
      message = faultInstance.getMessageValue();
      assertEquals(new QName("http://example.com/translator", "dictionaryNotAvailableFault"),
          message.getType().getName());
      // data content
      Element detailPart = message.getPart("detail");
      assertEquals("http://example.com/translator/types", detailPart.getNamespaceURI());
      assertEquals("dictionaryNotAvailable", detailPart.getLocalName());
    }
  }

  public static Test suite() {
    return new Setup();
  }

  private static class Setup extends ModuleDeployTestSetup {

    private Setup() {
      super(InvokeActionTest.class, InvokeActionTest.class.getResource("translator.war")
          .toExternalForm());
    }

    protected void setUp() throws Exception {
      super.setUp();
      processSource = XmlUtil.parseResource("translatorClient.bpel", InvokeActionTest.class);
    }

    protected void tearDown() throws Exception {
      processSource = null;
      super.tearDown();
    }
  }
}