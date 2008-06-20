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

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.servlet.ServletContext;
import javax.wsdl.Binding;
import javax.wsdl.Definition;
import javax.wsdl.Message;
import javax.wsdl.Operation;
import javax.wsdl.Port;
import javax.wsdl.PortType;
import javax.wsdl.extensions.soap.SOAPBinding;
import javax.xml.namespace.QName;
import javax.xml.rpc.JAXRPCException;
import javax.xml.rpc.handler.Handler;
import javax.xml.rpc.handler.HandlerInfo;
import javax.xml.rpc.handler.MessageContext;
import javax.xml.rpc.handler.soap.SOAPMessageContext;
import javax.xml.rpc.soap.SOAPFaultException;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.bpel.BpelException;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.exe.BpelFaultException;
import org.jbpm.bpel.integration.jms.IntegrationConstants;
import org.jbpm.bpel.integration.jms.IntegrationControl;
import org.jbpm.bpel.integration.jms.PartnerLinkEntry;
import org.jbpm.bpel.integration.jms.RequestListener;
import org.jbpm.bpel.integration.soap.MessageDirection;
import org.jbpm.bpel.integration.soap.SoapBindConstants;
import org.jbpm.bpel.integration.soap.SoapFormatter;
import org.jbpm.bpel.integration.soap.SoapUtil;
import org.jbpm.bpel.sublang.def.PropertyQuery;
import org.jbpm.bpel.variable.def.MessageType;
import org.jbpm.bpel.wsdl.PropertyAlias;
import org.jbpm.bpel.wsdl.xml.WsdlUtil;
import org.jbpm.bpel.xml.util.DatatypeUtil;
import org.jbpm.bpel.xml.util.XmlUtil;
import org.jbpm.util.ClassLoaderUtil;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/11/25 13:06:55 $
 */
public class SoapHandler implements Handler {

  private QName[] headers;

  private IntegrationControl integrationControl;

  private QName portTypeName;
  private QName serviceName;
  private String portName;
  private SoapFormatter formatter;

  private static final Log log = LogFactory.getLog(SoapHandler.class);
  private static final Map endpointMetadataLookups = readEndpointMetadataLookups();

  /** Message context property for the operation name */
  static final String OPERATION_NAME_PROP = "org.jbpm.operation.name";
  /** Message context property for the message parts */
  static final String MESSAGE_PARTS_PROP = "org.jbpm.message.parts";
  /** Message context property for the fault name */
  static final String FAULT_NAME_PROP = "org.jbpm.fault.name";
  /** Message context property for the SOAP fault exception */
  static final String FAULT_EXCEPTION_PROP = "org.jbpm.fault.exception";

  /** Servlet context attribute for the integration control instance */
  public static final String INTEGRATION_CONTROL_ATTR = "org.jbpm.integration.control";

  public static final String RESOURCE_ENDPOINT_METADATA_LOOKUPS = "resource.endpoint.metadata.lookups";

  /**
   * Allocates a new handler for JAX-RPC use.
   */
  public SoapHandler() {
  }

  /**
   * Allocates a new handler for test purposes.
   */
  SoapHandler(IntegrationControl integrationControl, QName serviceName, Port port) {
    this.integrationControl = integrationControl;
    this.serviceName = serviceName;
    portName = port.getName();
    Binding binding = port.getBinding();
    portTypeName = binding.getPortType().getQName();
    formatter = new SoapFormatter(binding);
  }

  public void init(HandlerInfo handlerInfo) throws JAXRPCException {
    // save headers
    headers = handlerInfo.getHeaders();
  }

  public void destroy() {
    // release port-component specific state
    integrationControl = null;
  }

  public QName[] getHeaders() {
    return headers;
  }

  public boolean handleRequest(MessageContext messageContext) throws JAXRPCException,
      SOAPFaultException {
    /*
     * WSEE 1.1 section 6.2.2.1: If Handler instances are pooled, they must be pooled by Port
     * component. This is because Handlers may retain non-client specific state across method calls
     * that are specific to the Port component.
     */
    if (integrationControl == null) {
      /*
       * COMPLIANCE NOTE: the state initialized in this call is port-component specific, but
       * non-client specific
       */
      lookupEndpointMetadata(messageContext);
    }

    JbpmContext jbpmContext = integrationControl.getIntegrationServiceFactory()
        .getJbpmConfiguration()
        .createJbpmContext();
    try {
      Session jmsSession = integrationControl.getJmsConnection().createSession(false,
          Session.CLIENT_ACKNOWLEDGE);
      try {
        SOAPMessage soapMessage = ((SOAPMessageContext) messageContext).getMessage();
        ObjectMessage jmsRequest = sendRequest(soapMessage, jmsSession, jbpmContext);

        Destination replyTo = jmsRequest.getJMSReplyTo();
        if (replyTo != null) {
          ObjectMessage jmsResponse = receiveResponse(jmsSession, replyTo,
              jmsRequest.getJMSMessageID(), jbpmContext);

          // remember operation name and message parts for handling response
          messageContext.setProperty(OPERATION_NAME_PROP,
              jmsRequest.getStringProperty(IntegrationConstants.OPERATION_NAME_PROP));
          messageContext.setProperty(MESSAGE_PARTS_PROP, jmsResponse.getObject());

          // is response a fault?
          String faultName = jmsResponse.getStringProperty(IntegrationConstants.FAULT_NAME_PROP);
          if (faultName != null) {
            // remember fault name for handling fault
            messageContext.setProperty(FAULT_NAME_PROP, faultName);
            throw new SOAPFaultException(SoapBindConstants.CLIENT_FAULTCODE,
                SoapBindConstants.BUSINESS_FAULTSTRING, null, null);
          }
        }
      }
      finally {
        jmsSession.close();
      }
    }
    /*
     * NO need to set jbpm context as rollback only for any exception, since operations in try-block
     * only read definitions from database
     */
    catch (SOAPFaultException e) {
      log.debug("request caused a fault", e);
      messageContext.setProperty(FAULT_EXCEPTION_PROP, e);
    }
    catch (SOAPException e) {
      /*
       * BP 1.2 R2724: If an INSTANCE receives an envelope that is inconsistent with its WSDL
       * description, it SHOULD generate a soap:Fault with a faultcode of "Client", unless a
       * "MustUnderstand" or "VersionMismatch" fault is generated.
       */
      log.debug("incoming soap message carries invalid content", e);
      messageContext.setProperty(FAULT_EXCEPTION_PROP, new SOAPFaultException(
          SoapBindConstants.CLIENT_FAULTCODE, e.getMessage(), null, null));
    }
    catch (JMSException e) {
      throw new JAXRPCException("message delivery failed", e);
    }
    finally {
      jbpmContext.close();
    }
    return true;
  }

  private void lookupEndpointMetadata(MessageContext messageContext) {
    // obtain metadata lookup strategy for the given message context class
    EndpointMetadataLookup endpointMetadataLookup = getEndpointMetadataLookup(messageContext.getClass());
    // lookup metadata in message context
    EndpointMetadata endpointMetadata = endpointMetadataLookup.lookupMetaData(messageContext);

    Definition definition = endpointMetadata.getWsdlDefinition();
    serviceName = endpointMetadata.getServiceName();
    portName = endpointMetadata.getPortName();

    Binding binding = definition.getService(serviceName).getPort(portName).getBinding();
    portTypeName = binding.getPortType().getQName();
    formatter = new SoapFormatter(binding, endpointMetadata.getFaultFormat());

    ServletContext servletContext = endpointMetadata.getServletContext();
    integrationControl = (IntegrationControl) servletContext.getAttribute(INTEGRATION_CONTROL_ATTR);
    integrationControl.getMyCatalog().addDefinition(definition);
  }

  private static EndpointMetadataLookup getEndpointMetadataLookup(Class messageContextClass) {
    String contextClassName = messageContextClass.getName();
    EndpointMetadataLookup endpointMetadataLookup = (EndpointMetadataLookup) endpointMetadataLookups.get(contextClassName);
    if (endpointMetadataLookup == null) {
      throw new BpelException("no endpoint metadata lookup for message context: "
          + contextClassName);
    }
    return endpointMetadataLookup;
  }

  /**
   * Obtain the mapping of known message context implementations to their corresponding endpoint
   * metadata lookups from the configuration.
   */
  private static Map readEndpointMetadataLookups() {
    // get activity readers resource name
    String resource = JbpmConfiguration.Configs.getString(RESOURCE_ENDPOINT_METADATA_LOOKUPS);

    // parse lookups document
    Element lookupsElem;
    try {
      lookupsElem = XmlUtil.parseResource(resource);
    }
    catch (SAXException e) {
      log.error("endpoint metadata lookups document contains invalid xml: " + resource, e);
      return Collections.EMPTY_MAP;
    }
    catch (IOException e) {
      log.error("could not read endpoint metadata lookups document: " + resource, e);
      return Collections.EMPTY_MAP;
    }

    // walk through endpointMetadataLookup elements
    HashMap lookups = new HashMap();
    for (Iterator i = XmlUtil.getElements(lookupsElem, null, "endpointMetadataLookup"); i.hasNext();) {
      Element lookupElem = (Element) i.next();
      String contextClassName = lookupElem.getAttribute("messageContextClass");

      // load lookup class
      String lookupClassName = lookupElem.getAttribute("lookupClass");
      try {
        Class lookupClass = ClassLoaderUtil.getClassLoader().loadClass(lookupClassName);
        // validate lookup class
        if (!EndpointMetadataLookup.class.isAssignableFrom(lookupClass)) {
          log.warn("not an endpoint metadata lookup: " + lookupClassName);
          continue;
        }

        // instantiate lookup class
        Object lookup = lookupClass.newInstance();
        // register lookup
        lookups.put(contextClassName, lookup);
        log.debug("registered endpoint metadata lookup: name="
            + contextClassName
            + ", class="
            + lookupClassName);
      }
      catch (ClassNotFoundException e) {
        log.debug("endpoint metadata lookup not found, skipping: " + lookupClassName);
      }
      catch (InstantiationException e) {
        log.warn("endpoint metadata lookup class not instantiable: " + lookupClassName, e);
      }
      catch (IllegalAccessException e) {
        log.warn("endpoint metadata lookup class or constructor not public: " + lookupClassName, e);
      }
    }
    return lookups;
  }

  public boolean handleResponse(MessageContext messageContext) throws JAXRPCException {
    Map parts = (Map) messageContext.getProperty(MESSAGE_PARTS_PROP);
    SOAPFaultException faultException = (SOAPFaultException) messageContext.getProperty(FAULT_EXCEPTION_PROP);

    // absence of both parts and fault means one-way operation
    if (parts == null && faultException == null)
      return true;

    String operationName = (String) messageContext.getProperty(OPERATION_NAME_PROP);
    SOAPMessage soapMessage = ((SOAPMessageContext) messageContext).getMessage();

    JbpmContext jbpmContext = integrationControl.getIntegrationServiceFactory()
        .getJbpmConfiguration()
        .createJbpmContext();
    try {
      lookupEndpointMetadata(messageContext);

      SOAPEnvelope envelope = soapMessage.getSOAPPart().getEnvelope();
      // remove existing body, it might have undesirable content
      SOAPBody body = envelope.getBody();
      body.detachNode();
      // re-create body
      body = envelope.addBody();

      if (faultException == null)
        writeOutput(operationName, soapMessage, parts);
      else {
        String faultName = (String) messageContext.getProperty(FAULT_NAME_PROP);
        writeFault(operationName, soapMessage, faultName, parts, faultException);
      }
    }
    /*
     * NO need to set jbpm context as rollback only for any exception, since operations in try-block
     * only read definitions from database
     */
    catch (SOAPException e) {
      throw new JAXRPCException("could not compose outbound soap message", e);
    }
    finally {
      jbpmContext.close();
    }
    return true;
  }

  public boolean handleFault(MessageContext messageContext) throws JAXRPCException {
    return true;
  }

  protected ObjectMessage sendRequest(SOAPMessage soapMessage, Session jmsSession,
      JbpmContext jbpmContext) throws SOAPException, JMSException {
    // create a jms message to deliver the incoming content
    ObjectMessage jmsRequest = jmsSession.createObjectMessage();

    // put the partner link identified by handle in a jms property
    PartnerLinkEntry partnerLinkEntry = integrationControl.getPartnerLinkEntry(portTypeName,
        serviceName, portName);
    long partnerLinkId = partnerLinkEntry.getId();
    jmsRequest.setLongProperty(IntegrationConstants.PARTNER_LINK_ID_PROP, partnerLinkId);

    Operation operation = determineOperation(soapMessage);
    if (operation == null)
      throw new SOAPException("could not determine operation to perform");

    // put the operation name in a jms property
    String operationName = operation.getName();
    jmsRequest.setStringProperty(IntegrationConstants.OPERATION_NAME_PROP, operationName);

    log.debug("received request: partnerLink=" + partnerLinkId + ", operation=" + operationName);

    // extract message content
    HashMap requestParts = new HashMap();
    formatter.readMessage(operationName, soapMessage, requestParts, MessageDirection.INPUT);
    jmsRequest.setObject(requestParts);

    // fill message properties
    BpelProcessDefinition process = integrationControl.getDeploymentDescriptor()
        .findProcessDefinition(jbpmContext);
    MessageType requestType = process.getImportDefinition().getMessageType(
        operation.getInput().getMessage().getQName());
    fillCorrelationProperties(requestParts, jmsRequest, requestType.getPropertyAliases());

    // set up producer
    MessageProducer producer = jmsSession.createProducer(partnerLinkEntry.getDestination());
    try {
      // is the exchange pattern request/response?
      if (operation.getOutput() != null) {
        Destination replyTo = integrationControl.getIntegrationServiceFactory()
            .getResponseDestination();
        jmsRequest.setJMSReplyTo(replyTo);

        // have jms discard request message if response timeout expires
        Number responseTimeout = getResponseTimeout(jbpmContext);
        if (responseTimeout != null)
          producer.setTimeToLive(responseTimeout.longValue());
      }
      else {
        // have jms discard message if one-way timeout expires
        Number oneWayTimeout = getOneWayTimeout(jbpmContext);
        if (oneWayTimeout != null)
          producer.setTimeToLive(oneWayTimeout.longValue());
      }

      // send request message
      producer.send(jmsRequest);
      log.debug("sent request: " + RequestListener.messageToString(jmsRequest));

      return jmsRequest;
    }
    finally {
      // release producer resources
      producer.close();
    }
  }

  private static Number getResponseTimeout(JbpmContext jbpmContext) {
    Object responseTimeout = jbpmContext.getObjectFactory().createObject(
        "jbpm.bpel.response.timeout");
    if (responseTimeout instanceof Number)
      return (Number) responseTimeout;
    else if (responseTimeout != null)
      log.warn("response timeout is not a number: " + responseTimeout);
    return null;
  }

  private static Number getOneWayTimeout(JbpmContext jbpmContext) {
    Object oneWayTimeout = jbpmContext.getObjectFactory().createObject("jbpm.bpel.oneway.timeout");
    if (oneWayTimeout instanceof Number)
      return (Number) oneWayTimeout;
    else if (oneWayTimeout != null)
      log.warn("one-way timeout is not a number: " + oneWayTimeout);
    return null;
  }

  private Operation determineOperation(SOAPMessage soapMessage) throws SOAPException {
    Binding binding = formatter.getBinding();

    SOAPBinding soapBinding = (SOAPBinding) WsdlUtil.getExtension(
        binding.getExtensibilityElements(),
        com.ibm.wsdl.extensions.soap.SOAPConstants.Q_ELEM_SOAP_BINDING);

    String style = soapBinding.getStyle();
    if (style == null) {
      // wsdlsoap:binding does not specify any style, assume 'document'
      style = SoapBindConstants.DOCUMENT_STYLE;
    }

    PortType portType = binding.getPortType();
    SOAPElement bodyElement = SoapUtil.getElement(soapMessage.getSOAPBody());

    if (style.equals(SoapBindConstants.RPC_STYLE)) {
      String operationName = bodyElement.getLocalName();
      return portType.getOperation(operationName, null, null);
    }

    List operations = portType.getOperations();
    for (int i = 0, n = operations.size(); i < n; i++) {
      Operation operation = (Operation) operations.get(i);
      Message inputMessage = operation.getInput().getMessage();
      QName docLitElementName = WsdlUtil.getDocLitElementName(inputMessage);

      if (XmlUtil.nodeNameEquals(bodyElement, docLitElementName))
        return operation;
    }
    return null;
  }

  /**
   * Gets the values of message properties from the request message parts and sets them in the
   * property fields of the JMS message.
   * @param requestParts the parts extracted from the request SOAP message
   * @param jmsRequest the JMS message whose properties will be set
   * @param propertyAliases the property aliases associated with the request message type
   * @throws JMSException
   */
  private static void fillCorrelationProperties(Map requestParts, ObjectMessage jmsRequest,
      Map propertyAliases) throws JMSException {
    // easy way out: no property aliases
    if (propertyAliases == null)
      return;
    // iterate through the property aliases associated with the message type
    for (Iterator i = propertyAliases.entrySet().iterator(); i.hasNext();) {
      Entry aliasEntry = (Entry) i.next();
      QName propertyName = (QName) aliasEntry.getKey();
      PropertyAlias alias = (PropertyAlias) aliasEntry.getValue();
      // get part accessor from operation wrapper
      String partName = alias.getPart();
      Object value = requestParts.get(partName);
      if (value == null) {
        log.debug("message part not found, cannot get property value: property="
            + propertyName
            + ", part="
            + partName);
        continue;
      }
      // evaluate the query against the part value, if any
      PropertyQuery query = alias.getQuery();
      if (query != null) {
        try {
          value = query.getEvaluator().evaluate((Element) value);
        }
        catch (BpelFaultException e) {
          // the most likely cause is a selection failure due to missing nodes
          log.debug("query evaluation failed, "
              + "cannot get property value: property="
              + propertyName
              + ", part="
              + partName
              + ", query="
              + query.getText(), e);
          continue;
        }
      }
      // set the value in a jms message property field
      jmsRequest.setObjectProperty(propertyName.getLocalPart(),
          value instanceof Node ? DatatypeUtil.toString((Node) value) : value);
    }
  }

  protected ObjectMessage receiveResponse(Session jmsSession, Destination replyTo,
      String requestId, JbpmContext jbpmContext) throws JMSException, SOAPFaultException {
    // set up consumer
    String selector = "JMSCorrelationID='" + requestId + '\'';
    MessageConsumer consumer = jmsSession.createConsumer(replyTo, selector);
    try {
      // receive response message
      log.debug("listening for response: destination=" + replyTo + ", requestId=" + requestId);
      Number responseTimeout = getResponseTimeout(jbpmContext);
      ObjectMessage jmsResponse = (ObjectMessage) (responseTimeout != null ? consumer.receive(responseTimeout.longValue())
          : consumer.receive());
      // did a message arrive in time?
      if (jmsResponse == null) {
        log.debug("response timeout expired: destination=" + replyTo + ", requestId" + requestId);
        throw new SOAPFaultException(SoapBindConstants.SERVER_FAULTCODE,
            SoapBindConstants.TIMEOUT_FAULTSTRING, null, null);
      }
      jmsResponse.acknowledge();
      log.debug("received response: " + RequestListener.messageToString(jmsResponse));
      return jmsResponse;
    }
    finally {
      // release consumer resources
      consumer.close();
    }
  }

  protected void writeOutput(String operationName, SOAPMessage soapMessage, Map responseParts)
      throws SOAPException {
    formatter.writeMessage(operationName, soapMessage, responseParts, MessageDirection.OUTPUT);
  }

  protected void writeFault(String operationName, SOAPMessage soapMessage, String faultName,
      Map faultParts, SOAPFaultException faultException) throws SOAPException {
    formatter.writeFault(operationName, soapMessage, faultName, faultParts,
        faultException.getFaultCode(), faultException.getFaultString());
  }
}