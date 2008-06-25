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
package org.jbpm.bpel.integration.client;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.wsdl.Fault;
import javax.wsdl.Port;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ibm.wsdl.extensions.soap.SOAPConstants;

import org.jbpm.bpel.BpelException;
import org.jbpm.bpel.graph.exe.BpelFaultException;
import org.jbpm.bpel.graph.exe.FaultInstance;
import org.jbpm.bpel.integration.soap.MessageDirection;
import org.jbpm.bpel.integration.soap.SoapFormatter;
import org.jbpm.bpel.variable.def.MessageType;
import org.jbpm.bpel.variable.exe.MessageValue;
import org.jbpm.bpel.wsdl.xml.WsdlUtil;
import org.jbpm.bpel.xml.BpelConstants;

/**
 * Provides support for the dynamic invocation of a service endpoint bound to SOAP.
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2008/01/30 07:18:22 $
 */
public class SoapCaller implements Caller {

  private final SoapFormatter formatter;
  private final URL address;
  private final SOAPConnection soapConnection;

  private static final Log log = LogFactory.getLog(SoapCaller.class);

  private static MessageFactory messageFactory;
  private static SOAPConnectionFactory soapConnectionFactory;

  public SoapCaller(Port port) {
    formatter = new SoapFormatter(port.getBinding());

    // exclude non-soap ports
    SOAPAddress soapAddress = (SOAPAddress) WsdlUtil.getExtension(port.getExtensibilityElements(),
        SOAPConstants.Q_ELEM_SOAP_ADDRESS);
    if (soapAddress == null)
      throw new BpelException("not a soap-bound port: " + port);

    // exclude malformed locations
    String location = soapAddress.getLocationURI();
    try {
      address = new URL(location);
    }
    catch (MalformedURLException e) {
      throw new BpelException("invalid address location: " + location, e);
    }

    try {
      soapConnection = soapConnectionFactory.createConnection();
    }
    catch (SOAPException e) {
      throw new BpelException("could not create soap connection", e);
    }
  }

  public SoapFormatter getFormatter() {
    return formatter;
  }

  public URL getAddress() {
    return address;
  }

  public Map call(String operation, Map inputParts) {
    try {
      SOAPMessage soapOutput = callImpl(operation, inputParts);
      HashMap outputParts = new HashMap();

      if (!formatter.hasFault(soapOutput)) {
        formatter.readMessage(operation, soapOutput, outputParts, MessageDirection.OUTPUT);
        return outputParts;
      }

      Fault fault = formatter.readFault(operation, soapOutput, outputParts);
      /*
       * WS-BPEL 2.0 section 6.1: each WSDL fault is identified in WS-BPEL by a qualified name
       * formed by the target namespace of the WSDL document in which the relevant port type and
       * fault are defined, and the NCName of the fault
       */
      String targetNamespace = formatter.getBinding().getPortType().getQName().getNamespaceURI();
      QName faultQName = new QName(targetNamespace, fault.getName());

      MessageValue faultMessage = new MessageValue(new MessageType(fault.getMessage()));
      faultMessage.setParts(outputParts);

      FaultInstance faultInstance = new FaultInstance(faultQName, faultMessage);
      throw new BpelFaultException(faultInstance);
    }
    catch (SOAPException e) {
      // BPEL-286 raise SOAP communication exception as BPEL fault
      log.error("endpoint call failed: " + address, e);
      throw new BpelFaultException(BpelConstants.FAULT_INVOCATION_FAILURE);
    }
  }

  public void callOneWay(String operation, Map inputParts) {
    try {
      callImpl(operation, inputParts);
    }
    catch (SOAPException e) {
      // BPEL-286 raise SOAP communication exception as BPEL fault
      log.error("endpoint call failed: " + address, e);
      throw new BpelFaultException(BpelConstants.FAULT_INVOCATION_FAILURE);
    }
  }

  private SOAPMessage callImpl(String operation, Map inputParts) throws SOAPException {
    // create message
    SOAPMessage soapInput = messageFactory.createMessage();

    // write message
    formatter.writeMessage(operation, soapInput, inputParts, MessageDirection.INPUT);

    // call endpoint
    log.debug("calling endpoint at: " + address);
    return soapConnection.call(soapInput, address);
  }

  public void close() {
    try {
      soapConnection.close();
    }
    catch (SOAPException e) {
      log.warn("could not close soap connection", e);
    }
  }

  static {
    /*
     * Static creation of SAAJ factories is a moot question. Whereas he specification does not
     * indicate their concurrency, typical implementations simply instantiate objects of appropriate
     * concrete class and are totally thread safe.
     */
    try {
      messageFactory = MessageFactory.newInstance();
      soapConnectionFactory = SOAPConnectionFactory.newInstance();
    }
    catch (SOAPException e) {
      // should not happen
      throw new AssertionError(e);
    }
  }

  public String toString() {
    return new ToStringBuilder(this).append("formatter", formatter)
        .append("address", address)
        .toString();
  }
}
