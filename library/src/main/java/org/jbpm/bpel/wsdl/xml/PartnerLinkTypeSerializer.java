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

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Iterator;

import javax.wsdl.Definition;
import javax.wsdl.PortType;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.ExtensionDeserializer;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.extensions.ExtensionSerializer;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.ibm.wsdl.Constants;
import com.ibm.wsdl.util.xml.DOMUtils;

import org.jbpm.bpel.wsdl.PartnerLinkType;
import org.jbpm.bpel.xml.util.DatatypeUtil;
import org.jbpm.bpel.xml.util.XmlUtil;

/**
 * Translates between <code>plt:partnerLinkType</code> elements and {@link PartnerLinkType}
 * instances.
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/09/04 06:42:27 $
 */
public class PartnerLinkTypeSerializer implements ExtensionDeserializer, ExtensionSerializer,
    Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * Deserializes a DOM element into a {@link PartnerLinkType} instance.
   * @param parentType class object indicating where in the WSDL document this extensibility element
   * was encountered
   * @param elementType the qname of the extensibility element
   * @param elem the extensibility element to deserialize
   * @param def the definition this extensibility element was encountered in
   * @param extReg the ExtensionRegistry to use (if needed again)
   * @return the deserialized instance.
   * @throws WSDLException if deserialization fails
   */
  public ExtensibilityElement unmarshall(Class parentType, QName elementType, Element elem,
      Definition def, ExtensionRegistry extReg) throws WSDLException {
    /*
     * XXX wsdl4j 1.4 doesn't register namespaces declared in subelements of wsdl:definitions to the
     * Definition object, and the wsdl converter pushes top-level bpel/plt declarations down to the
     * bpel/plt extension elements
     */
    def.addNamespace(elem.getPrefix(), elem.getNamespaceURI());

    PartnerLinkType partnerLinkType = (PartnerLinkType) extReg.createExtension(parentType,
        elementType);

    // name attribute
    String name = elem.getAttribute(WsdlConstants.ATTR_NAME);
    partnerLinkType.setQName(new QName(def.getTargetNamespace(), name));

    // wsdl:required attribute
    String required = DOMUtils.getAttributeNS(elem, Constants.NS_URI_WSDL, Constants.ATTR_REQUIRED);
    if (required != null) {
      partnerLinkType.setRequired(DatatypeUtil.parseBoolean(required));
    }

    Iterator roleElemIt = XmlUtil.getElements(elem, WsdlConstants.NS_PLNK, WsdlConstants.ELEM_ROLE);

    // first role element
    Element roleElem = (Element) roleElemIt.next();
    partnerLinkType.setFirstRole(unmarshallRole(roleElem, partnerLinkType, def));

    // second role element (optional)
    if (roleElemIt.hasNext()) {
      roleElem = (Element) roleElemIt.next();
      partnerLinkType.setSecondRole(unmarshallRole(roleElem, partnerLinkType, def));
    }
    return partnerLinkType;
  }

  /**
   * Serializes a {@link PartnerLinkType} instance into the given {@link PrintWriter}.
   * @param parentType class object indicating where in the WSDL document this extensibility element
   * was encountered
   * @param elementType the qname of the extensibility element
   * @param extension the instance to serialize
   * @param pw the stream to write in
   * @param def the definition this extensibility element was encountered in
   * @param extReg the ExtensionRegistry to use (if needed again)
   * @throws WSDLException if serialization fails
   */
  public void marshall(Class parentType, QName elementType, ExtensibilityElement extension,
      PrintWriter pw, Definition def, ExtensionRegistry extReg) throws WSDLException {
    if (extension == null)
      return;

    PartnerLinkType partnerLinkType = (PartnerLinkType) extension;

    // open tag
    String tagName = DOMUtils.getQualifiedValue(WsdlConstants.NS_PLNK,
        WsdlConstants.ELEM_PARTNER_LINK_TYPE, def);
    pw.print("  <" + tagName);

    // name attribute
    DOMUtils.printAttribute(WsdlConstants.ATTR_NAME, partnerLinkType.getQName().getLocalPart(), pw);

    // wsdl:required attribute
    Boolean required = partnerLinkType.getRequired();
    if (required != null) {
      DOMUtils.printQualifiedAttribute(Constants.Q_ATTR_REQUIRED, required.toString(), def, pw);
    }
    pw.println('>');

    // role elements
    marshallRole(partnerLinkType.getFirstRole(), pw, def);
    marshallRole(partnerLinkType.getSecondRole(), pw, def);

    // close tag
    pw.println("  </" + tagName + '>');
  }

  /**
   * Deserializes a DOM element into a {@link org.jbpm.bpel.wsdl.PartnerLinkType.Role} instance.
   * @param roleElem the element to deserialize
   * @param partnerLinkType the partner link type that contains this role
   * @param def the definition where the extensibility element appears
   * @return the deserialized instance
   */
  protected PartnerLinkType.Role unmarshallRole(Element roleElem, PartnerLinkType partnerLinkType,
      Definition def) {
    PartnerLinkType.Role role = partnerLinkType.createRole();

    // name attribute
    String name = roleElem.getAttribute(WsdlConstants.ATTR_NAME);
    role.setName(name);

    // portType attribute
    QName portTypeQName = XmlUtil.getQNameValue(roleElem.getAttributeNode(WsdlConstants.ATTR_PORT_TYPE));
    PortType portType = WsdlUtil.getPortType(def, portTypeQName);
    if (portType == null) {
      // patch missing port type
      portType = def.createPortType();
      portType.setQName(portTypeQName);
      def.addPortType(portType);
    }
    role.setPortType(portType);
    return role;
  }

  /**
   * Serializes a {@link org.jbpm.bpel.wsdl.PartnerLinkType.Role} instance into the given
   * {@link PrintWriter}.
   * @param role the instance to serialize
   * @param pw the stream to write in
   * @param def the definition where the extensibility element appears
   * @throws WSDLException if serialization fails
   */
  protected void marshallRole(PartnerLinkType.Role role, PrintWriter pw, Definition def)
      throws WSDLException {
    if (role == null)
      return;

    // Open tag
    String roleTag = DOMUtils.getQualifiedValue(WsdlConstants.NS_PLNK, WsdlConstants.ELEM_ROLE, def);
    pw.print("    <" + roleTag);

    // name attribute
    DOMUtils.printAttribute(WsdlConstants.ATTR_NAME, role.getName(), pw);

    // port type attribute
    DOMUtils.printQualifiedAttribute(WsdlConstants.ATTR_PORT_TYPE, role.getPortType().getQName(),
        def, pw);

    // Close tag
    pw.println("/>");
  }
}
