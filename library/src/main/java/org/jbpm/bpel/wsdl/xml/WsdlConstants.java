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
package org.jbpm.bpel.wsdl.xml;

import javax.xml.namespace.QName;

/**
 * Constant values used in WSDL extension definitions.
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/11/25 13:03:13 $
 */
public class WsdlConstants {

  // Namespace URIs
  public static final String NS_PLNK = "http://docs.oasis-open.org/wsbpel/2.0/plnktype";
  public static final String NS_PLNK_1_1 = "http://schemas.xmlsoap.org/ws/2003/05/partner-link/";
  public static final String NS_VPROP = "http://docs.oasis-open.org/wsbpel/2.0/varprop";

  // Element names
  public static final String ELEM_PARTNER_LINK_TYPE = "partnerLinkType";
  public static final String ELEM_ROLE = "role";
  public static final String ELEM_PROPERTY = "property";
  public static final String ELEM_PROPERTY_ALIAS = "propertyAlias";
  public static final String ELEM_QUERY = "query";

  // Qualified element names
  public static final QName Q_PARTNER_LINK_TYPE = new QName(NS_PLNK, ELEM_PARTNER_LINK_TYPE);
  public static final QName Q_PROPERTY = new QName(NS_VPROP, ELEM_PROPERTY);
  public static final QName Q_PROPERTY_ALIAS = new QName(NS_VPROP, ELEM_PROPERTY_ALIAS);
  public static final QName Q_QUERY = new QName(NS_VPROP, ELEM_QUERY);

  // Attribute names
  public static final String ATTR_NAME = "name";
  public static final String ATTR_PORT_TYPE = "portType";
  public static final String ATTR_TYPE = "type";
  public static final String ATTR_PROPERTY_NAME = "propertyName";
  public static final String ATTR_MESSAGE_TYPE = "messageType";
  public static final String ATTR_PART = "part";
  public static final String ATTR_ELEMENT = "element";
  public static final String ATTR_QUERY_LANGUAGE = "queryLanguage";
}