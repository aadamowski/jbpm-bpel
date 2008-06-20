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
package org.jbpm.bpel.integration.soap;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPConstants;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/04/11 06:37:35 $
 */
public class SoapBindConstants {

  // transports
  public static final String HTTP_TRANSPORT_URI = "http://schemas.xmlsoap.org/soap/http";

  // styles
  public static final String RPC_STYLE = "rpc";
  public static final String DOCUMENT_STYLE = "document";

  // uses
  public static final String LITERAL_USE = "literal";
  public static final String ENCODED_USE = "encoded";

  // headers
  public static final String SOAP_ACTION_HEADER = "SOAPAction";

  // fault codes
  public static final QName CLIENT_FAULTCODE = new QName(
      SOAPConstants.URI_NS_SOAP_ENVELOPE, "Client");
  public static final QName SERVER_FAULTCODE = new QName(
      SOAPConstants.URI_NS_SOAP_ENVELOPE, "Server");

  // fault strings
  /** The fault string for response timeouts. */
  public static final String TIMEOUT_FAULTSTRING = "The service is not in "
      + "an appropiate state for the requested operation";
  /** The fault string for faults returned from the business process. */
  public static final String BUSINESS_FAULTSTRING = "Business logic fault";

  // suppress default constructor, ensuring non-instantiability
  private SoapBindConstants() {
  }
}
