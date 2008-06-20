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

import javax.wsdl.Definition;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.ExtensionDeserializer;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.extensions.ExtensionSerializer;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.ibm.wsdl.Constants;
import com.ibm.wsdl.util.xml.DOMUtils;

import org.jbpm.bpel.wsdl.Property;
import org.jbpm.bpel.xml.util.DatatypeUtil;
import org.jbpm.bpel.xml.util.XmlUtil;

/**
 * Translates between <code>bpel:property</code> elements and {@link Property} instances.
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/09/04 06:42:27 $
 */
public class PropertySerializer implements ExtensionSerializer, ExtensionDeserializer, Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * Deserializes a DOM element into a {@link Property} instance.
   * @param parentType class object indicating where in the WSDL document this extensibility element
   * was encountered
   * @param elementType the qname of the extensibility element
   * @param elem the extensibility element to deserialize.
   * @param def the definition this extensibility element was encountered in.
   * @param extReg the ExtensionRegistry to use (if needed again).
   * @return the deserialized instance.
   * @throws WSDLException if deserialization fails.
   */
  public ExtensibilityElement unmarshall(Class parentType, QName elementType, Element elem,
      Definition def, ExtensionRegistry extReg) throws WSDLException {
    /*
     * XXX wsdl4j 1.4 doesn't register namespaces declared in subelements of wsdl:definitions to the
     * Definition object, and the wsdl converter pushes top-level bpel/plt declarations down to the
     * bpel/plt extension elements
     */
    def.addNamespace(elem.getPrefix(), elem.getNamespaceURI());

    Property property = (Property) extReg.createExtension(parentType, elementType);

    // name attribute
    String name = elem.getAttribute(WsdlConstants.ATTR_NAME);
    property.setQName(new QName(def.getTargetNamespace(), name));

    // type attribute
    property.setType(XmlUtil.getQNameValue(elem.getAttributeNode(WsdlConstants.ATTR_TYPE)));

    // wsdl:required attribute
    String required = DOMUtils.getAttributeNS(elem, Constants.NS_URI_WSDL, Constants.ATTR_REQUIRED);
    if (required != null)
      property.setRequired(DatatypeUtil.parseBoolean(required));

    // the definition is complete
    property.setUndefined(false);

    return property;
  }

  /**
   * Serializes a {@link Property} instance into the given {@link PrintWriter}.
   * @param parentType class object indicating where in the WSDL document this extensibility element
   * was encountered
   * @param elementType the qname of the extensibility element
   * @param extension the instance to serialize.
   * @param pw the stream to write in.
   * @param def the definition this extensibility element was encountered in.
   * @param extReg the ExtensionRegistry to use (if needed again).
   * @throws WSDLException if serialization fails
   */
  public void marshall(Class parentType, QName elementType, ExtensibilityElement extension,
      PrintWriter pw, Definition def, ExtensionRegistry extReg) throws WSDLException {
    if (extension == null)
      return;

    Property property = (Property) extension;

    // the definition is not complete, do not write it
    if (property.isUndefined())
      return;

    // open tag
    String tagName = DOMUtils.getQualifiedValue(WsdlConstants.NS_VPROP,
        WsdlConstants.ELEM_PROPERTY, def);
    pw.print("  <" + tagName);

    // name attribute
    DOMUtils.printAttribute(WsdlConstants.ATTR_NAME, property.getQName().getLocalPart(), pw);

    // type attribute
    DOMUtils.printQualifiedAttribute(WsdlConstants.ATTR_TYPE, property.getType(), def, pw);

    // wsdl:required attribute
    Boolean required = property.getRequired();
    if (required != null) {
      DOMUtils.printQualifiedAttribute(Constants.Q_ATTR_REQUIRED, required.toString(), def, pw);
    }

    // close tag
    pw.println("/>");
  }
}
