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

import javax.xml.namespace.QName;

import org.jbpm.bpel.endpointref.wsa.WsaConstants;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/11/25 13:07:15 $
 */
public class IntegrationConstants {

  /** JMS property for the partner link identifier, in the request message. */
  public static final String PARTNER_LINK_ID_PROP = "JbpmPartnerLink";

  /** JMS property for the operation name, in the request message. */
  public static final String OPERATION_NAME_PROP = "JbpmOperation";

  /** JMS property for the fault qualified name, in the response message. */
  public static final String FAULT_NAME_PROP = "JbpmFault";

  static final QName DEFAULT_REFERENCE_NAME = new QName(WsaConstants.NS_ADDRESSING,
      WsaConstants.ELEM_ENDPOINT_REFERENCE);
}
