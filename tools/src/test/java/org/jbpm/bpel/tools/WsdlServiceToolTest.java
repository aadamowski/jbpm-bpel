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
package org.jbpm.bpel.tools;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.wsdl.Binding;
import javax.wsdl.BindingFault;
import javax.wsdl.BindingInput;
import javax.wsdl.BindingOperation;
import javax.wsdl.BindingOutput;
import javax.wsdl.Definition;
import javax.wsdl.Import;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.extensions.soap.SOAPBinding;
import javax.wsdl.extensions.soap.SOAPBody;
import javax.wsdl.extensions.soap.SOAPFault;
import javax.wsdl.extensions.soap.SOAPOperation;
import javax.xml.namespace.QName;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.xml.sax.InputSource;

import com.ibm.wsdl.extensions.soap.SOAPConstants;

import org.jbpm.bpel.deploy.DeploymentDescriptor;
import org.jbpm.bpel.deploy.MyRoleDescriptor;
import org.jbpm.bpel.deploy.PartnerLinkDescriptor;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.integration.catalog.CentralCatalog;
import org.jbpm.bpel.integration.soap.SoapBindConstants;
import org.jbpm.bpel.wsdl.xml.WsdlUtil;
import org.jbpm.bpel.xml.BpelReader;
import org.jbpm.bpel.xml.DeploymentDescriptorReader;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/10/17 22:02:51 $
 */
public class WsdlServiceToolTest extends TestCase {

  private static WsdlServiceTool tool = new WsdlServiceTool();

  private static Map bindingDefinitions = new HashMap();
  private static Definition serviceDefinition;
  private static DeploymentDescriptor deploymentDescriptor = new DeploymentDescriptor();

  private static final String NS_ATM_FRONT = "urn:samples:atm";
  private static final String NS_ATM_PROCESS = "urn:samples:atmProcess";
  private static final String NS_TRAVEL_AGENT = "http://jbpm.org/examples/trip/";

  public void testGenerateBindingDefinitions() {
    assertEquals(2, bindingDefinitions.size());
    assertTrue(bindingDefinitions.containsKey(NS_ATM_FRONT));
    assertTrue(bindingDefinitions.containsKey(NS_TRAVEL_AGENT));
  }

  public void testGenerateBindingDefinition_rpc() {
    Definition bindingDef = getRpcBindingDefinition();
    List interfaceImps = bindingDef.getImports(NS_ATM_FRONT);
    assertEquals(1, interfaceImps.size());
    Import interfaceImp = (Import) interfaceImps.get(0);
    assertEquals(NS_ATM_FRONT, interfaceImp.getNamespaceURI());
    assertEquals("interface/rpc.wsdl", interfaceImp.getLocationURI());
  }

  public void testGenerateBindingDefinition_doc() {
    Definition bindingDef = getDocBindingDefinition();
    List interfaceImps = bindingDef.getImports(NS_TRAVEL_AGENT);
    assertEquals(1, interfaceImps.size());
    Import interfaceImp = (Import) interfaceImps.get(0);
    assertEquals(NS_TRAVEL_AGENT, interfaceImp.getNamespaceURI());
    assertEquals("interface/doc.wsdl", interfaceImp.getLocationURI());
  }

  public void testGenerateInterfaceImport_rpc() {
    Definition bindingDef = getRpcBindingDefinition();
    for (Iterator i = bindingDef.getImports(NS_ATM_FRONT).iterator(); i.hasNext();) {
      Import interfaceImport = (Import) i.next();
      testWriteImportedDefinition(tool.getWsdlDirectory(), interfaceImport);
    }
  }

  public void testGenerateInterfaceImport_doc() {
    Definition bindingDef = getDocBindingDefinition();
    for (Iterator i = bindingDef.getImports(NS_TRAVEL_AGENT).iterator(); i.hasNext();) {
      Import interfaceImport = (Import) i.next();
      testWriteImportedDefinition(tool.getWsdlDirectory(), interfaceImport);
    }
  }

  private static void testWriteImportedDefinition(File baseDir, Import _import) {
    File outputFile = new File(baseDir, _import.getLocationURI());
    assertTrue(outputFile.exists());

    // deal with imported documents
    baseDir = outputFile.getParentFile();
    for (Iterator l = _import.getDefinition().getImports().values().iterator(); l.hasNext();) {
      List imports = (List) l.next();

      for (Iterator i = imports.iterator(); i.hasNext();) {
        _import = (Import) i.next();
        testWriteImportedDefinition(baseDir, _import);
      }
    }
  }

  public void testGenerateBindings_rpc() {
    Definition bindingDef = getRpcBindingDefinition();
    Map bindings = bindingDef.getBindings();
    assertEquals(1, bindings.size());
    assertTrue(bindings.containsKey(new QName(NS_ATM_FRONT, "atmBinding")));
  }

  public void testGenerateBinding_rpc() {
    Binding rpcBinding = getRpcBinding();
    assertEquals(new QName(NS_ATM_FRONT, "atm"), rpcBinding.getPortType().getQName());
    SOAPBinding soapBind = (SOAPBinding) WsdlUtil.getExtension(
        rpcBinding.getExtensibilityElements(), SOAPConstants.Q_ELEM_SOAP_BINDING);
    assertEquals(SoapBindConstants.RPC_STYLE, soapBind.getStyle());
    assertEquals("http://schemas.xmlsoap.org/soap/http", soapBind.getTransportURI());
  }

  public void testGenerateBindingOperations_rpc() {
    List bindOps = getRpcBinding().getBindingOperations();
    assertEquals(3, bindOps.size());
    assertEquals("logon", ((BindingOperation) bindOps.get(0)).getName());
    assertEquals("deposit", ((BindingOperation) bindOps.get(1)).getName());
    assertEquals("withdraw", ((BindingOperation) bindOps.get(2)).getName());
  }

  public void testGenerateBindingOperation_rpc() {
    Binding rpcBinding = getRpcBinding();
    List bindOps = rpcBinding.getBindingOperations();
    BindingOperation logonOp = (BindingOperation) bindOps.get(0);
    SOAPOperation soapOp = (SOAPOperation) WsdlUtil.getExtension(
        logonOp.getExtensibilityElements(), SOAPConstants.Q_ELEM_SOAP_OPERATION);
    assertEquals(NS_ATM_FRONT + '#' + logonOp.getName(), soapOp.getSoapActionURI());
  }

  public void testGenerateBindingInput_rpc() {
    BindingOperation logonOp = (BindingOperation) getRpcBinding().getBindingOperations().get(0);
    BindingInput bindInput = logonOp.getBindingInput();
    SOAPBody soapBody = (SOAPBody) WsdlUtil.getExtension(bindInput.getExtensibilityElements(),
        SOAPConstants.Q_ELEM_SOAP_BODY);
    assertEquals(SoapBindConstants.LITERAL_USE, soapBody.getUse());
    assertEquals(NS_ATM_FRONT, soapBody.getNamespaceURI());
    assertNull(logonOp.getBindingOutput());
    assertTrue(logonOp.getBindingFaults().isEmpty());
  }

  public void testGenerateBindingOutput_rpc() {
    BindingOperation depositOp = (BindingOperation) getRpcBinding().getBindingOperations().get(1);
    assertNotNull(depositOp.getBindingInput());
    BindingOutput bindOutput = depositOp.getBindingOutput();
    SOAPBody soapBody = (SOAPBody) WsdlUtil.getExtension(bindOutput.getExtensibilityElements(),
        SOAPConstants.Q_ELEM_SOAP_BODY);
    assertEquals(SoapBindConstants.LITERAL_USE, soapBody.getUse());
    assertEquals(NS_ATM_FRONT, soapBody.getNamespaceURI());
    assertTrue(depositOp.getBindingFaults().isEmpty());
  }

  public void testGenerateBindings_doc() {
    Definition bindingDef = getDocBindingDefinition();
    Map bindings = bindingDef.getBindings();
    assertEquals(1, bindings.size());
    assertTrue(bindings.containsKey(new QName(NS_TRAVEL_AGENT, "TravelAgentBinding")));
  }

  public void testGenerateBinding_doc() {
    Binding docBinding = getDocBinding();
    assertEquals(new QName(NS_TRAVEL_AGENT, "TravelAgent"), docBinding.getPortType().getQName());
    SOAPBinding soapBind = (SOAPBinding) WsdlUtil.getExtension(
        docBinding.getExtensibilityElements(), SOAPConstants.Q_ELEM_SOAP_BINDING);
    assertEquals(SoapBindConstants.DOCUMENT_STYLE, soapBind.getStyle());
    assertEquals("http://schemas.xmlsoap.org/soap/http", soapBind.getTransportURI());
  }

  public void testGenerateBindingOperations_doc() {
    List bindOps = getDocBinding().getBindingOperations();
    assertEquals(1, bindOps.size());
    assertEquals("purchaseTrip", ((BindingOperation) bindOps.get(0)).getName());
  }

  public void testGenerateBindingOperation_doc() {
    Binding docBinding = getDocBinding();
    List bindOps = docBinding.getBindingOperations();
    BindingOperation purchaseTripOp = (BindingOperation) bindOps.get(0);
    SOAPOperation soapOp = (SOAPOperation) WsdlUtil.getExtension(
        purchaseTripOp.getExtensibilityElements(), SOAPConstants.Q_ELEM_SOAP_OPERATION);
    assertEquals(NS_TRAVEL_AGENT + '#' + purchaseTripOp.getName(), soapOp.getSoapActionURI());
  }

  public void testGenerateBindingInput_doc() {
    BindingOperation purchaseTripOp = (BindingOperation) getDocBinding().getBindingOperations()
        .get(0);
    BindingInput bindInput = purchaseTripOp.getBindingInput();
    SOAPBody soapBody = (SOAPBody) WsdlUtil.getExtension(bindInput.getExtensibilityElements(),
        SOAPConstants.Q_ELEM_SOAP_BODY);
    assertEquals(SoapBindConstants.LITERAL_USE, soapBody.getUse());
    assertNull(soapBody.getNamespaceURI());
  }

  public void testGenerateBindingOutput_doc() {
    BindingOperation purchaseTripOp = (BindingOperation) getDocBinding().getBindingOperations()
        .get(0);
    BindingOutput bindOutput = purchaseTripOp.getBindingOutput();
    SOAPBody soapBody = (SOAPBody) WsdlUtil.getExtension(bindOutput.getExtensibilityElements(),
        SOAPConstants.Q_ELEM_SOAP_BODY);
    assertEquals(SoapBindConstants.LITERAL_USE, soapBody.getUse());
    assertNull(soapBody.getNamespaceURI());
  }

  public void testGenerateBindingFaults() {
    BindingOperation withdrawOp = (BindingOperation) getRpcBinding().getBindingOperations().get(2);
    assertNotNull(withdrawOp.getBindingInput());
    assertNotNull(withdrawOp.getBindingOutput());
    Map bindFaults = withdrawOp.getBindingFaults();
    assertEquals(1, bindFaults.size());
    assertNotNull(bindFaults.get("notEnoughFunds"));
  }

  public void testGenerateBindingFault() {
    BindingOperation withdrawOp = (BindingOperation) getRpcBinding().getBindingOperations().get(2);
    BindingFault bindFault = withdrawOp.getBindingFault("notEnoughFunds");
    SOAPFault soapFault = (SOAPFault) WsdlUtil.getExtension(bindFault.getExtensibilityElements(),
        SOAPConstants.Q_ELEM_SOAP_FAULT);
    assertEquals("notEnoughFunds", soapFault.getName());
    assertEquals("literal", soapFault.getUse());
  }

  public void testGenerateServiceDefinition() {
    assertEquals(NS_ATM_PROCESS, serviceDefinition.getTargetNamespace());
    List bindingImps = serviceDefinition.getImports(NS_ATM_FRONT);
    assertEquals(1, bindingImps.size());
    Import bindingImp = (Import) bindingImps.get(0);
    assertEquals(NS_ATM_FRONT, bindingImp.getNamespaceURI());
    assertEquals("binding1.wsdl", bindingImp.getLocationURI());
  }

  public void testGenerateService() {
    Service atmService = getProcessService();
    Map ports = atmService.getPorts();
    assertEquals(2, ports.size());
    assertTrue(ports.containsKey("frontPort"));
    assertTrue(ports.containsKey("agentPort"));
  }

  public void testGeneratePort_rpc() {
    Service atmService = getProcessService();
    Port frontEndPort = atmService.getPort("frontPort");
    assertEquals(new QName(NS_ATM_FRONT, "atmBinding"), frontEndPort.getBinding().getQName());

    SOAPAddress soapAddress = (SOAPAddress) WsdlUtil.getExtension(
        frontEndPort.getExtensibilityElements(), SOAPConstants.Q_ELEM_SOAP_ADDRESS);
    assertEquals("REPLACE_WITH_ACTUAL_URI", soapAddress.getLocationURI());
  }

  public void testGeneratePort_doc() {
    Service atmService = getProcessService();
    Port travelAgentPort = atmService.getPort("agentPort");
    assertEquals(new QName(NS_TRAVEL_AGENT, "TravelAgentBinding"), travelAgentPort.getBinding()
        .getQName());

    SOAPAddress soapAddress = (SOAPAddress) WsdlUtil.getExtension(
        travelAgentPort.getExtensibilityElements(), SOAPConstants.Q_ELEM_SOAP_ADDRESS);
    assertEquals("REPLACE_WITH_ACTUAL_URI", soapAddress.getLocationURI());
  }

  public void testGenerateDeploymentDescriptor() {
    assertEquals("atm", deploymentDescriptor.getName());
    assertEquals(NS_ATM_PROCESS, deploymentDescriptor.getTargetNamespace());
    assertNull(deploymentDescriptor.getVersion());
  }

  public void testGeneratePartnerLinkDescriptor_rpc() {
    PartnerLinkDescriptor partnerLink = (PartnerLinkDescriptor) deploymentDescriptor.getPartnerLinks()
        .get("frontEnd");
    MyRoleDescriptor myRole = partnerLink.getMyRole();
    assertEquals(new QName(NS_ATM_PROCESS, "atmService"), myRole.getService());
    assertEquals("frontPort", myRole.getPort());
  }

  public void testGeneratePartnerLinkDescriptor_doc() {
    PartnerLinkDescriptor partnerLink = (PartnerLinkDescriptor) deploymentDescriptor.getPartnerLinks()
        .get("travelAgent");
    MyRoleDescriptor myRole = partnerLink.getMyRole();
    assertEquals(new QName(NS_ATM_PROCESS, "atmService"), myRole.getService());
    assertEquals("agentPort", myRole.getPort());
  }

  public void testGenerateServiceCatalogs() {
    CentralCatalog centralCatalog = CentralCatalog.getConfigurationInstance();
    assertSame(centralCatalog, deploymentDescriptor.getServiceCatalog());
  }

  private Definition getRpcBindingDefinition() {
    return (Definition) bindingDefinitions.get(NS_ATM_FRONT);
  }

  private Binding getRpcBinding() {
    return getRpcBindingDefinition().getBinding(new QName(NS_ATM_FRONT, "atmBinding"));
  }

  private Definition getDocBindingDefinition() {
    return (Definition) bindingDefinitions.get(NS_TRAVEL_AGENT);
  }

  private Binding getDocBinding() {
    return getDocBindingDefinition().getBinding(new QName(NS_TRAVEL_AGENT, "TravelAgentBinding"));
  }

  private Service getProcessService() {
    return serviceDefinition.getService(new QName(NS_ATM_PROCESS, "atmService"));
  }

  public static Test suite() {
    return new Setup();
  }

  private static class Setup extends TestSetup {

    private Setup() {
      super(new TestSuite(WsdlServiceToolTest.class));
    }

    protected void setUp() throws Exception {
      BpelProcessDefinition processDefinition = new BpelProcessDefinition();

      // read bpel
      URL processUrl = WsdlServiceToolTest.class.getResource("process.bpel");
      BpelReader bpelReader = new BpelReader();
      bpelReader.read(processDefinition, new InputSource(processUrl.toExternalForm()));
      assertEquals(0, bpelReader.getProblemHandler().getProblemCount());

      // generate the binding and service documents
      tool.generateWsdlService(processDefinition);

      // read the service definition
      File serviceFile = new File(tool.getWsdlDirectory(), tool.getServiceFileName());
      serviceDefinition = WsdlUtil.getFactory().newWSDLReader().readWSDL(serviceFile.getPath());

      // get the binding definitions
      for (Iterator l = serviceDefinition.getImports().values().iterator(); l.hasNext();) {
        List importList = (List) l.next();
        for (int i = 0, n = importList.size(); i < n; i++) {
          Import _import = (Import) importList.get(i);
          bindingDefinitions.put(_import.getNamespaceURI(), _import.getDefinition());
        }
      }

      // read the deployment descriptor
      File deploymentDescriptorFile = tool.getDeploymentDescriptorFile();
      new DeploymentDescriptorReader().read(deploymentDescriptor, new InputSource(
          deploymentDescriptorFile.getPath()));
    }

    protected void tearDown() throws Exception {
      // comment if you want to see the generated files
      tool.deleteGeneratedFiles();
    }
  }
}