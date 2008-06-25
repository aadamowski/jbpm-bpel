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
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.wsdl.Binding;
import javax.wsdl.BindingFault;
import javax.wsdl.BindingInput;
import javax.wsdl.BindingOperation;
import javax.wsdl.BindingOutput;
import javax.wsdl.Definition;
import javax.wsdl.Fault;
import javax.wsdl.Import;
import javax.wsdl.Input;
import javax.wsdl.Message;
import javax.wsdl.Operation;
import javax.wsdl.Output;
import javax.wsdl.Part;
import javax.wsdl.Port;
import javax.wsdl.PortType;
import javax.wsdl.Service;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.extensions.soap.SOAPBinding;
import javax.wsdl.extensions.soap.SOAPBody;
import javax.wsdl.extensions.soap.SOAPFault;
import javax.wsdl.extensions.soap.SOAPOperation;
import javax.wsdl.xml.WSDLWriter;
import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;

import com.ibm.wsdl.Constants;
import com.ibm.wsdl.extensions.soap.SOAPConstants;

import org.jbpm.bpel.deploy.DeploymentDescriptor;
import org.jbpm.bpel.deploy.MyRoleDescriptor;
import org.jbpm.bpel.deploy.PartnerLinkDescriptor;
import org.jbpm.bpel.deploy.ScopeDescriptor;
import org.jbpm.bpel.graph.def.AbstractBpelVisitor;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.def.ImportDefinition;
import org.jbpm.bpel.graph.scope.Scope;
import org.jbpm.bpel.integration.catalog.CentralCatalog;
import org.jbpm.bpel.integration.catalog.ServiceCatalog;
import org.jbpm.bpel.integration.def.PartnerLinkDefinition;
import org.jbpm.bpel.integration.soap.SoapBindConstants;
import org.jbpm.bpel.wsdl.PartnerLinkType.Role;
import org.jbpm.bpel.wsdl.xml.WsdlUtil;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.bpel.xml.DeploymentDescriptorWriter;
import org.jbpm.bpel.xml.ProblemCounter;
import org.jbpm.bpel.xml.ProblemHandler;
import org.jbpm.bpel.xml.util.XmlUtil;
import org.jbpm.jpdl.xml.Problem;

/**
 * Generates WSDL binding and service definitions.
 * @author Alejandro Guízar
 * @author Juan Cantú
 * @version $Revision$ $Date: 2008/02/05 05:37:34 $
 */
public class WsdlServiceTool {

  private File wsdlDirectory = DEFAULT_WSDL_DIRECTORY;
  private String bindingFilesPrefix = DEFAULT_BINDING_FILES_PREFIX;
  private String bindingFilesSuffix = DEFAULT_BINDING_FILES_SUFFIX;
  private String serviceFileName = DEFAULT_SERVICE_FILE_NAME;
  private File deploymentDescriptorFile = DEFAULT_DEPLOYMENT_DESCRIPTOR_FILE;

  private ProblemHandler problemHandler = new ProblemCounter();

  static final File DEFAULT_WSDL_DIRECTORY = FileUtil.CURRENT_DIRECTORY;
  static final String DEFAULT_BINDING_FILES_PREFIX = "binding";
  static final String DEFAULT_BINDING_FILES_SUFFIX = ".wsdl";
  static final String DEFAULT_SERVICE_FILE_NAME = "service.wsdl";
  static final File DEFAULT_DEPLOYMENT_DESCRIPTOR_FILE = new File(DeploymentDescriptor.FILE_NAME);

  private static final String ADDRESS_LOCATION_URI = "REPLACE_WITH_ACTUAL_URI";

  private static final Log log = LogFactory.getLog(WsdlServiceTool.class);

  /**
   * Returns where to place generated WSDL files.
   * @return the WSDL directory
   */
  public File getWsdlDirectory() {
    return wsdlDirectory;
  }

  /**
   * Specifies where to place generated WSDL files.
   * @param wsdlDirectory the directory for generated WSDL files
   * @throws IllegalArgumentException if the argument is <code>null</code>
   */
  public void setWsdlDirectory(File wsdlDirectory) {
    if (wsdlDirectory == null)
      throw new IllegalArgumentException("wsdl directory cannot be null");
    this.wsdlDirectory = wsdlDirectory;
  }

  /**
   * Returns the prefix used to construct the name of WSDL binding files.
   * @return the WSDL binding files prefix
   */
  public String getBindingFilesPrefix() {
    return bindingFilesPrefix;
  }

  /**
   * Sets the prefix used to construct the name of WSDL binding files.
   * @param bindingFilePrefix the WSDL binding files prefix
   * @throws IllegalArgumentException if the argument is <code>null</code>
   */
  public void setBindingFilesPrefix(String bindingFilePrefix) {
    if (bindingFilePrefix == null)
      throw new IllegalArgumentException("binding files prefix cannot be null");

    this.bindingFilesPrefix = bindingFilePrefix;
  }

  /**
   * Returns the suffix used to generate the name of WSDL binding files.
   * @return the WSDL binding files suffix
   */
  public String getBindingFilesSuffix() {
    return bindingFilesSuffix;
  }

  /**
   * Sets the suffix used to generate the name of WSDL binding files.
   * @param bindingFileSuffix the WSDL binding files suffix
   * @throws IllegalArgumentException if the argument is <code>null</code>
   */
  public void setBindingFilesSuffix(String bindingFileSuffix) {
    if (bindingFileSuffix == null)
      throw new IllegalArgumentException("binding files suffix cannot be null");

    this.bindingFilesSuffix = bindingFileSuffix;
  }

  /**
   * Returns the name of the generated WSDL service file.
   * @return the WSDL service file name
   */
  public String getServiceFileName() {
    return serviceFileName;
  }

  /**
   * Sets the name of the generated WSDL service file.
   * @param serviceFileName the WSDL service file name
   * @throws IllegalArgumentException if the argument is <code>null</code>
   */
  public void setServiceFileName(String serviceFileName) {
    if (serviceFileName == null)
      throw new IllegalArgumentException("service file name cannot be null");

    this.serviceFileName = serviceFileName;
  }

  /**
   * Returns where to write the application descriptor.
   * @return the application descriptor file
   */
  public File getDeploymentDescriptorFile() {
    return deploymentDescriptorFile;
  }

  /**
   * Specifies where to write the application descriptor.
   * @param deploymentDescriptorFile the application descriptor file
   */
  public void setDeploymentDescriptorFile(File deploymentDescriptorFile) {
    this.deploymentDescriptorFile = deploymentDescriptorFile;
  }

  public ProblemHandler getProblemHandler() {
    return problemHandler;
  }

  public void setProblemHandler(ProblemHandler problemHandler) {
    if (problemHandler == null)
      throw new IllegalArgumentException("problem handler cannot be null");

    this.problemHandler = problemHandler;
  }

  public void generateWsdlService(BpelProcessDefinition process) {
    // generate the binding and service documents
    ServiceDefinitionBuilder serviceDefinitionBuilder = new ServiceDefinitionBuilder();
    serviceDefinitionBuilder.visit(process);

    // write required interface files
    WSDLWriter wsdlWriter = WsdlUtil.getFactory().newWSDLWriter();

    Map interfaceFiles = serviceDefinitionBuilder.getInterfaceFiles();
    for (Iterator i = interfaceFiles.entrySet().iterator(); i.hasNext();) {
      Map.Entry interfaceEntry = (Map.Entry) i.next();
      File interfaceFile = (File) interfaceEntry.getKey();
      Definition interfaceDefinition = (Definition) interfaceEntry.getValue();

      // write interface file
      try {
        WsdlUtil.writeFile(interfaceFile, interfaceDefinition, wsdlWriter);
        log.debug("wrote interface definition: " + interfaceFile.getName());
      }
      catch (WSDLException e) {
        problemHandler.add(new Problem(Problem.LEVEL_ERROR,
            "could not write interface definition: " + interfaceFile, e));
      }
    }

    // write binding files
    StringBuffer bindingFileNameBuffer = new StringBuffer(bindingFilesPrefix);
    int bindingCount = 0;

    Definition serviceDefinition = serviceDefinitionBuilder.getServiceDefinition();
    for (Iterator i = serviceDefinition.getImports().values().iterator(); i.hasNext();) {
      List bindingImports = (List) i.next();
      assert bindingImports.size() == 1 : bindingImports.size();

      // format binding filename
      bindingFileNameBuffer.setLength(bindingFilesPrefix.length());
      String bindingFileName = bindingFileNameBuffer.append(++bindingCount).append(
          bindingFilesSuffix).toString();

      // fill import location
      Import bindingImport = (Import) bindingImports.get(0);
      bindingImport.setLocationURI(bindingFileName);

      // write binding file
      File bindingFile = new File(wsdlDirectory, bindingFileName);
      try {
        WsdlUtil.writeFile(bindingFile, bindingImport.getDefinition(), wsdlWriter);
        log.debug("wrote binding definition: " + bindingFile.getName());
      }
      catch (WSDLException e) {
        problemHandler.add(new Problem(Problem.LEVEL_ERROR, "could not write binding definition: "
            + bindingFile, e));
      }
    }

    // write service file
    File serviceFile = new File(wsdlDirectory, serviceFileName);
    try {
      WsdlUtil.writeFile(serviceFile, serviceDefinition, wsdlWriter);
      log.debug("wrote service definition: " + serviceFile.getName());
    }
    catch (WSDLException e) {
      problemHandler.add(new Problem(Problem.LEVEL_ERROR, "could not write service definition: "
          + serviceFile, e));
    }

    // write deployment descriptor, if requested
    if (deploymentDescriptorFile != null) {
      DeploymentDescriptor deploymentDescriptor = serviceDefinitionBuilder.getDeploymentDescriptor();

      // transform descriptor to xml format
      Element descriptorElem = XmlUtil.createElement(BpelConstants.NS_DEPLOYMENT_DESCRIPTOR,
          BpelConstants.ELEM_BPEL_DEPLOYMENT);
      DeploymentDescriptorWriter.getInstance().write(deploymentDescriptor, descriptorElem);

      // write descriptor file
      try {
        XmlUtil.writeFile(descriptorElem, deploymentDescriptorFile);
        log.debug("wrote deployment descriptor: " + deploymentDescriptorFile.getName());
      }
      catch (IOException e) {
        problemHandler.add(new Problem(Problem.LEVEL_ERROR,
            "could not write deployment descriptor: " + deploymentDescriptorFile, e));
      }
    }
  }

  protected Definition createDefinition(String targetNamespace) {
    Definition def = WsdlUtil.getFactory().newDefinition();
    def.setTargetNamespace(targetNamespace);
    def.addNamespace("tns", targetNamespace);
    def.addNamespace("soap", SOAPConstants.NS_URI_SOAP);
    def.addNamespace(null, Constants.NS_URI_WSDL);
    return def;
  }

  protected Definition generateServiceDefinition(BpelProcessDefinition processDefinition) {
    return createDefinition(processDefinition.getTargetNamespace());
  }

  protected String generateServiceLocalName(BpelProcessDefinition processDefinition,
      Definition serviceDefinition) {
    return processDefinition.getName() + "Service";
  }

  protected DeploymentDescriptor generateDeploymentDescriptor(
      BpelProcessDefinition processDefinition) {
    DeploymentDescriptor deploymentDescriptor = new DeploymentDescriptor();
    deploymentDescriptor.setName(processDefinition.getName());
    deploymentDescriptor.setTargetNamespace(processDefinition.getTargetNamespace());
    deploymentDescriptor.setVersion(new Integer(processDefinition.getVersion()));
    return deploymentDescriptor;
  }

  private static Definition getBindingDefinition(Definition serviceDefinition,
      String targetNamespace) {
    List imports = serviceDefinition.getImports(targetNamespace);
    if (imports == null || imports.isEmpty())
      return null;
    Import _import = (Import) imports.get(0);
    return _import.getDefinition();
  }

  protected Definition generateBindingDefinition(PartnerLinkDefinition partnerLink,
      Definition serviceDefinition) throws WSDLException {
    String targetNamespace = partnerLink.getMyRole().getPortType().getQName().getNamespaceURI();
    return createDefinition(targetNamespace);
  }

  protected Import generateBindingImport(Definition serviceDefinition, Definition bindingDefinition) {
    // import the binding definition from the service definition
    Import bindingImport = serviceDefinition.createImport();
    bindingImport.setNamespaceURI(bindingDefinition.getTargetNamespace());
    bindingImport.setDefinition(bindingDefinition);
    return bindingImport;
  }

  private static Import getInterfaceImport(Definition bindingDefinition, String interfaceLocation) {
    List imports = bindingDefinition.getImports(bindingDefinition.getTargetNamespace());
    if (imports != null) {
      for (int i = 0, n = imports.size(); i < n; i++) {
        Import _import = (Import) imports.get(i);
        if (_import.getLocationURI().equals(interfaceLocation))
          return _import;
      }
    }
    return null;
  }

  protected Import generateInterfaceImport(Definition bindingDefinition,
      Definition interfaceDefinition) throws WSDLException {
    Import interfaceImport = bindingDefinition.createImport();
    interfaceImport.setNamespaceURI(interfaceDefinition.getTargetNamespace());
    interfaceImport.setDefinition(interfaceDefinition);
    return interfaceImport;
  }

  protected String generateBindingLocalName(PartnerLinkDefinition partnerLink,
      Definition bindingDefinition) {
    return partnerLink.getMyRole().getPortType().getQName().getLocalPart() + "Binding";
  }

  protected Binding generateBinding(PortType portType, Definition bindingDefinition)
      throws WSDLException {
    // wsdl binding
    Binding binding = bindingDefinition.createBinding();
    binding.setPortType(portType);

    // the binding is fully specified, mark it as defined
    binding.setUndefined(false);

    // soap binding
    ExtensionRegistry extRegistry = bindingDefinition.getExtensionRegistry();
    SOAPBinding soapBinding = (SOAPBinding) extRegistry.createExtension(Binding.class,
        SOAPConstants.Q_ELEM_SOAP_BINDING);
    String style = determineBindingStyle(portType);
    soapBinding.setStyle(style);
    soapBinding.setTransportURI(SoapBindConstants.HTTP_TRANSPORT_URI);
    binding.addExtensibilityElement(soapBinding);

    // operations
    for (Iterator i = portType.getOperations().iterator(); i.hasNext();) {
      Operation operation = (Operation) i.next();
      BindingOperation bindingOperation = generateBindingOperation(operation, bindingDefinition,
          style);
      binding.addBindingOperation(bindingOperation);
    }

    return binding;
  }

  /**
   * Determines the appropriate binding style for the given port type according to BP 1.2
   * requirements.
   * @param portType the port type whose binding style is to be determined
   * @return "{@linkplain SoapBindConstants#RPC_STYLE rpc}" if the port type references one or
   * more parts that have been defined using the type attribute; "{@linkplain SoapBindConstants#DOCUMENT_STYLE document}"
   * otherwise
   */
  protected String determineBindingStyle(PortType portType) {
    /*
     * BP 1.2 R2203: An rpc-literal binding MUST refer, in its soapbind:body element(s), only to
     * wsdl:part element(s) that have been defined using the type attribute
     */
    for (Iterator i = portType.getOperations().iterator(); i.hasNext();) {
      Operation operation = (Operation) i.next();

      // input message
      Input input = operation.getInput();
      if (input != null && containsTypePart(input.getMessage()))
        return SoapBindConstants.RPC_STYLE;

      // output message
      Output output = operation.getOutput();
      if (output != null && containsTypePart(output.getMessage()))
        return SoapBindConstants.RPC_STYLE;
    }
    return SoapBindConstants.DOCUMENT_STYLE;
  }

  private static boolean containsTypePart(Message message) {
    for (Iterator i = message.getParts().values().iterator(); i.hasNext();) {
      Part part = (Part) i.next();
      if (part.getTypeName() != null)
        return true;
    }
    return false;
  }

  protected BindingOperation generateBindingOperation(Operation operation,
      Definition bindingDefinition, String style) throws WSDLException {
    // binding operation
    BindingOperation bindOperation = bindingDefinition.createBindingOperation();
    bindOperation.setOperation(operation);
    bindOperation.setName(operation.getName());

    // soap operation
    SOAPOperation soapOper = (SOAPOperation) bindingDefinition.getExtensionRegistry()
        .createExtension(BindingOperation.class, SOAPConstants.Q_ELEM_SOAP_OPERATION);
    soapOper.setSoapActionURI(generateSoapAction(operation, bindingDefinition));
    bindOperation.addExtensibilityElement(soapOper);

    // binding input
    BindingInput bindInput = generateBindingInput(operation.getInput(), bindingDefinition, style);
    bindOperation.setBindingInput(bindInput);

    // request-response operations have an output and zero or more faults
    if (operation.getOutput() != null) {
      BindingOutput bindOutput = generateBindingOutput(operation.getOutput(), bindingDefinition,
          style);
      bindOperation.setBindingOutput(bindOutput);

      // faults
      for (Iterator i = operation.getFaults().values().iterator(); i.hasNext();) {
        Fault fault = (Fault) i.next();
        BindingFault bindFault = generateBindingFault(fault, bindingDefinition);
        bindOperation.addBindingFault(bindFault);
      }
    }

    return bindOperation;
  }

  protected String generateSoapAction(Operation operation, Definition bindingDefinition) {
    try {
      // target namespace
      URI targetNamespaceURI = new URI(bindingDefinition.getTargetNamespace());

      // set the operation name as the fragment of the target namespace URI
      URI soapActionURI = new URI(targetNamespaceURI.getScheme(),
          targetNamespaceURI.getSchemeSpecificPart(), operation.getName());
      return soapActionURI.toString();
    }
    catch (URISyntaxException e) {
      // target namespace is not a valid URI - generate an empty action
      return "";
    }
  }

  protected BindingInput generateBindingInput(Input input, Definition bindingDefinition,
      String style) throws WSDLException {
    // soap body
    SOAPBody soapBody = (SOAPBody) bindingDefinition.getExtensionRegistry().createExtension(
        BindingInput.class, SOAPConstants.Q_ELEM_SOAP_BODY);
    soapBody.setUse(SoapBindConstants.LITERAL_USE);

    if (SoapBindConstants.RPC_STYLE.equals(style)) {
      soapBody.setNamespaceURI(generateRpcBodyNamespace(input, bindingDefinition));
      soapBody.setParts(getRpcBodyPartNames(input.getMessage()));
    }
    else
      soapBody.setParts(getDocumentBodyPartNames(input.getMessage()));

    // binding input
    BindingInput bindInput = bindingDefinition.createBindingInput();
    bindInput.addExtensibilityElement(soapBody);
    return bindInput;
  }

  protected String generateRpcBodyNamespace(Input input, Definition bindingDefinition) {
    return bindingDefinition.getTargetNamespace();
  }

  private static List getRpcBodyPartNames(Message message) {
    ArrayList partNames = new ArrayList();
    /*
     * BP 1.2 R2203: An rpc-literal binding MUST refer, in its soapbind:body element(s), only to
     * wsdl:part element(s) that have been defined using the type attribute
     */
    Map parts = message.getParts();
    for (Iterator i = parts.values().iterator(); i.hasNext();) {
      Part part = (Part) i.next();
      if (part.getTypeName() != null)
        partNames.add(part.getName());
    }
    /*
     * WSDL 1.1 section 3.5: If the parts attribute is omitted, then all parts defined by the
     * message are assumed to be included in the SOAP Body portion
     */
    return partNames.size() == parts.size() ? null : partNames;
  }

  private static List getDocumentBodyPartNames(Message message) {
    /*
     * BP 1.2 R2210: If a document-literal binding does not specify the parts attribute on a
     * soapbind:body element, the corresponding abstract wsdl:message MUST define zero or one
     * wsdl:parts
     */
    Map parts = message.getParts();
    if (parts.size() <= 1)
      return null;

    /*
     * BP 1.2 R2204: A document-literal binding MUST refer, in each of its soapbind:body element(s),
     * only to wsdl:part element(s) that have been defined using the element attribute.
     * 
     * BP 1.2 R2201: A document-literal binding MUST, in each of its soapbind:body element(s), have
     * at most one part listed in the parts attribute, if the parts attribute is specified
     * 
     * Corollary: the first element part is to be listed in the parts attribute
     */
    for (Iterator i = parts.values().iterator(); i.hasNext();) {
      Part part = (Part) i.next();
      if (part.getElementName() != null)
        return Collections.singletonList(part.getName());
    }
    return Collections.EMPTY_LIST;
  }

  protected BindingOutput generateBindingOutput(Output output, Definition bindingDefinition,
      String style) throws WSDLException {
    // soap body
    SOAPBody soapBody = (SOAPBody) bindingDefinition.getExtensionRegistry().createExtension(
        BindingOutput.class, SOAPConstants.Q_ELEM_SOAP_BODY);
    soapBody.setUse(SoapBindConstants.LITERAL_USE);
    if (SoapBindConstants.RPC_STYLE.equals(style)) {
      soapBody.setNamespaceURI(generateRpcBodyNamespace(output, bindingDefinition));
      soapBody.setParts(getRpcBodyPartNames(output.getMessage()));
    }
    else
      soapBody.setParts(getDocumentBodyPartNames(output.getMessage()));

    // binding output
    BindingOutput bindOutput = bindingDefinition.createBindingOutput();
    bindOutput.addExtensibilityElement(soapBody);
    return bindOutput;
  }

  protected String generateRpcBodyNamespace(Output output, Definition bindingDefinition) {
    return bindingDefinition.getTargetNamespace();
  }

  protected BindingFault generateBindingFault(Fault fault, Definition bindingDefinition)
      throws WSDLException {
    String faultName = fault.getName();

    // soap fault
    SOAPFault soapFault = (SOAPFault) bindingDefinition.getExtensionRegistry().createExtension(
        BindingFault.class, SOAPConstants.Q_ELEM_SOAP_FAULT);
    soapFault.setName(faultName);
    soapFault.setUse(SoapBindConstants.LITERAL_USE);

    // binding fault
    BindingFault bindFault = bindingDefinition.createBindingFault();
    bindFault.setName(faultName);
    bindFault.addExtensibilityElement(soapFault);
    return bindFault;
  }

  protected String generatePortName(PartnerLinkDefinition partnerLink, Service service) {
    String portName = partnerLink.getMyRole().getName() + "Port";

    // check for a conflicting port name
    Map ports = service.getPorts();
    if (ports.containsKey(portName))
      portName = generateName(portName, ports.keySet());

    return portName;
  }

  protected Port generatePort(Binding binding, Definition serviceDefinition) throws WSDLException {
    // port
    Port port = serviceDefinition.createPort();
    port.setBinding(binding);

    // namespace declaration for binding name
    String bindingNamespace = binding.getQName().getNamespaceURI();
    Map namespaces = serviceDefinition.getNamespaces();
    if (!namespaces.containsValue(bindingNamespace)) {
      String prefix = generateName("bindingNS", namespaces.keySet());
      serviceDefinition.addNamespace(prefix, bindingNamespace);
    }

    // soap address
    SOAPAddress soapAddress = (SOAPAddress) serviceDefinition.getExtensionRegistry()
        .createExtension(Port.class, SOAPConstants.Q_ELEM_SOAP_ADDRESS);
    soapAddress.setLocationURI(ADDRESS_LOCATION_URI);
    port.addExtensibilityElement(soapAddress);

    return port;
  }

  private static String generateName(String base, Set existingNames) {
    StringBuffer nameBuffer = new StringBuffer(base);
    int baseLength = base.length();
    for (int i = 2; i < Integer.MAX_VALUE; i++) {
      // append a natural number to the base text
      String altName = nameBuffer.append(i).toString();

      // check there is no collision with existing names
      if (!existingNames.contains(altName))
        return altName;

      // remove appended number
      nameBuffer.setLength(baseLength);
    }
    throw new Error("could not generate name from base: " + base);
  }

  protected PartnerLinkDescriptor generatePartnerLinkDescriptor(PartnerLinkDefinition partnerLink,
      Service service, Port port) {
    MyRoleDescriptor myRoleDescriptor = new MyRoleDescriptor();
    myRoleDescriptor.setService(service.getQName());
    myRoleDescriptor.setPort(port.getName());

    PartnerLinkDescriptor partnerLinkDescriptor = new PartnerLinkDescriptor();
    partnerLinkDescriptor.setName(partnerLink.getName());
    partnerLinkDescriptor.setMyRole(myRoleDescriptor);
    return partnerLinkDescriptor;
  }

  protected ServiceCatalog generateServiceCatalog(DeploymentDescriptor deploymentDescriptor) {
    return CentralCatalog.getConfigurationInstance();
  }

  public void deleteGeneratedFiles() {
    // recursively delete wsdl files
    deleteWsdlFile(new File(wsdlDirectory, serviceFileName));
    // delete deployment descriptor
    if (FileUtil.clean(deploymentDescriptorFile))
      log.info("deleted: " + deploymentDescriptorFile);
  }

  private static void deleteWsdlFile(File file) {
    Definition def;
    try {
      def = WsdlUtil.getFactory().newWSDLReader().readWSDL(file.getPath());
    }
    catch (WSDLException e) {
      log.error("not a wsdl file: " + file, e);
      return;
    }

    // delete the current file first
    if (FileUtil.clean(file))
      log.info("deleted: " + file);

    // deal with imported files
    for (Iterator l = def.getImports().values().iterator(); l.hasNext();) {
      List importList = (List) l.next();

      for (Iterator i = importList.iterator(); i.hasNext();) {
        javax.wsdl.Import _import = (javax.wsdl.Import) i.next();
        deleteSourceWsdlFile(_import.getDefinition());
      }
    }
  }

  private static void deleteSourceWsdlFile(Definition def) {
    String baseLocation = def.getDocumentBaseURI();
    try {
      URI baseUri = new URI(baseLocation);

      // easy way out: not a file
      if (!"file".equalsIgnoreCase(baseUri.getScheme()))
        return;

      // delete the base file first
      File file = new File(baseUri);
      if (FileUtil.clean(file))
        log.info("deleted: " + file);

      // deal with imported files
      for (Iterator l = def.getImports().values().iterator(); l.hasNext();) {
        List importList = (List) l.next();

        for (Iterator i = importList.iterator(); i.hasNext();) {
          javax.wsdl.Import _import = (javax.wsdl.Import) i.next();
          deleteSourceWsdlFile(_import.getDefinition());
        }
      }
    }
    catch (URISyntaxException e) {
      log.debug("document base is not a valid uri: " + baseLocation, e);
    }
  }

  class ServiceDefinitionBuilder extends AbstractBpelVisitor {

    private Definition serviceDefinition;
    private Service service;

    private DeploymentDescriptor deploymentDescriptor;
    private ScopeDescriptor topmostScopeDescriptor;

    private URI processLocationUri;
    private ImportDefinition importDefinition;

    private final Map interfaceFiles = new HashMap();

    public void visit(BpelProcessDefinition processDefinition) {
      // enclosing definition
      serviceDefinition = generateServiceDefinition(processDefinition);

      // service
      service = serviceDefinition.createService();
      QName serviceName = new QName(serviceDefinition.getTargetNamespace(),
          generateServiceLocalName(processDefinition, serviceDefinition));
      service.setQName(serviceName);
      serviceDefinition.addService(service);

      // keep import module for later use
      importDefinition = processDefinition.getImportDefinition();

      String processLocation = processDefinition.getLocation();
      // strip filename off the location so that URI.relativize() works on import locations
      int slashIndex = processLocation.lastIndexOf('/');
      if (slashIndex != -1)
        processLocation = processLocation.substring(0, slashIndex + 1);

      try {
        processLocationUri = new URI(processLocation);
      }
      catch (URISyntaxException e) {
        problemHandler.add(new Problem(Problem.LEVEL_WARNING, "process location is not a uri: "
            + processLocation, e));
        processLocationUri = URI.create("");
      }

      // app descriptor
      deploymentDescriptor = generateDeploymentDescriptor(processDefinition);
      topmostScopeDescriptor = deploymentDescriptor;

      // propagate visit
      propagate(processDefinition.getGlobalScope());

      // service catalog
      ServiceCatalog serviceCatalog = generateServiceCatalog(deploymentDescriptor);
      deploymentDescriptor.setServiceCatalog(serviceCatalog);
    }

    public void visit(Scope scope) {
      ScopeDescriptor scopeDescriptor = new ScopeDescriptor();
      scopeDescriptor.setName(scope.getName());

      ScopeDescriptor parentScopeDescriptor = topmostScopeDescriptor;
      parentScopeDescriptor.addScope(scopeDescriptor);

      // push scope to stack
      topmostScopeDescriptor = scopeDescriptor;
      // propagate visit
      propagate(scope);
      // pop scope from stack
      topmostScopeDescriptor = parentScopeDescriptor;
    }

    private void propagate(Scope scope) {
      for (Iterator i = scope.getPartnerLinks().values().iterator(); i.hasNext();) {
        PartnerLinkDefinition partnerLink = (PartnerLinkDefinition) i.next();
        try {
          visit(partnerLink);
        }
        catch (WSDLException e) {
          problemHandler.add(new Problem(Problem.LEVEL_ERROR, "could not generate port for: "
              + partnerLink, e));
        }
      }
      // propagate visit
      scope.getActivity().accept(this);
    }

    Map getInterfaceFiles() {
      return interfaceFiles;
    }

    Definition getServiceDefinition() {
      return serviceDefinition;
    }

    DeploymentDescriptor getDeploymentDescriptor() {
      return deploymentDescriptor;
    }

    private void visit(PartnerLinkDefinition partnerLink) throws WSDLException {
      Role myRole = partnerLink.getMyRole();
      // if the process has no role, there is nothing to generate
      if (myRole == null)
        return;
      PortType portType = myRole.getPortType();

      // binding definition
      Definition bindingDefinition = getBindingDefinition(serviceDefinition, portType.getQName()
          .getNamespaceURI());
      if (bindingDefinition == null) {
        // binding definition does not exist yet, generate it
        bindingDefinition = generateBindingDefinition(partnerLink, serviceDefinition);

        // import binding definition from service definition
        Import bindingImport = generateBindingImport(serviceDefinition, bindingDefinition);
        serviceDefinition.addImport(bindingImport);
      }

      // check whether binding definition already imports interface definition
      Definition interfaceDefinition = importDefinition.getDeclaringDefinition(portType);
      String interfaceLocation = interfaceDefinition.getDocumentBaseURI();
      try {
        interfaceLocation = processLocationUri.relativize(new URI(interfaceLocation)).toString();
      }
      catch (URISyntaxException e) {
        problemHandler.add(new Problem(Problem.LEVEL_WARNING,
            "interface location is not a valid uri: " + interfaceLocation, e));
      }

      if (getInterfaceImport(bindingDefinition, interfaceLocation) == null) {
        // import interface definition from binding definition
        Import interfaceImport = generateInterfaceImport(bindingDefinition, interfaceDefinition);
        interfaceImport.setLocationURI(interfaceLocation);
        bindingDefinition.addImport(interfaceImport);

        // write interface definition, if read from a relative location
        addInterfaceDefinition(interfaceImport, wsdlDirectory);
      }

      // binding
      QName bindingName = new QName(bindingDefinition.getTargetNamespace(),
          generateBindingLocalName(partnerLink, bindingDefinition));
      Binding binding = bindingDefinition.getBinding(bindingName);

      if (binding == null) {
        binding = generateBinding(portType, bindingDefinition);
        binding.setQName(bindingName);
        bindingDefinition.addBinding(binding);
      }

      // port
      Port port = generatePort(binding, serviceDefinition);
      port.setName(generatePortName(partnerLink, service));
      service.addPort(port);

      // partner link in app descriptor
      topmostScopeDescriptor.addPartnerLink(generatePartnerLinkDescriptor(partnerLink, service,
          port));
    }

    private void addInterfaceDefinition(Import _import, File baseDirectory) throws WSDLException {
      String location = _import.getLocationURI();

      try {
        // if location is absolute, there is no need to write a copy
        if (new URI(location).isAbsolute())
          return;
      }
      catch (URISyntaxException e) {
        problemHandler.add(new Problem(Problem.LEVEL_WARNING,
            "import location is not a valid uri: " + location, e));
        // fall through, try and write the file
      }

      // place the definition in a location relative to the base directory
      File importFile = new File(baseDirectory, location);

      // check the existing files to suppress duplicates
      if (!interfaceFiles.containsKey(importFile)) {
        Definition definition = _import.getDefinition();
        interfaceFiles.put(importFile, definition);

        // add imported documents as well
        baseDirectory = importFile.getParentFile();
        for (Iterator l = definition.getImports().values().iterator(); l.hasNext();) {
          List imports = (List) l.next();

          for (int i = 0, n = imports.size(); i < n; i++) {
            Import recursiveImport = (Import) imports.get(i);
            addInterfaceDefinition(recursiveImport, baseDirectory);
          }
        }
      }
    }
  }
}