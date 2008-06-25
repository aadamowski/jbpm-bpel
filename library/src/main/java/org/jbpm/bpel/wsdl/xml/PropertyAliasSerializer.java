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
import javax.wsdl.Message;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.ExtensionDeserializer;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.extensions.ExtensionSerializer;
import javax.xml.namespace.QName;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;

import com.ibm.wsdl.Constants;
import com.ibm.wsdl.util.xml.DOMUtils;
import com.ibm.wsdl.util.xml.XPathUtils;

import org.jbpm.bpel.sublang.def.PropertyQuery;
import org.jbpm.bpel.wsdl.Property;
import org.jbpm.bpel.wsdl.PropertyAlias;
import org.jbpm.bpel.xml.util.DatatypeUtil;
import org.jbpm.bpel.xml.util.XmlUtil;
import org.jbpm.graph.action.Script;

/**
 * Translates between <code>bpel:propertyAlias</code> elements and {@link PropertyAlias}
 * instances.
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/09/12 23:20:17 $
 */
public class PropertyAliasSerializer implements ExtensionSerializer, ExtensionDeserializer,
    Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * Deserializes a DOM element into a {@link PropertyAlias} instance.
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
    PropertyAlias alias = (PropertyAlias) extReg.createExtension(parentType, elementType);

    /*
     * XXX wsdl4j 1.4 doesn't register namespaces declared in subelements of wsdl:definitions to the
     * Definition object, and the wsdl converter pushes top-level bpel/plt declarations down to the
     * bpel/plt extension elements
     */
    def.addNamespace(elem.getPrefix(), elem.getNamespaceURI());

    // property attribute - required
    QName propertyName = XmlUtil.getQNameValue(elem.getAttributeNode(WsdlConstants.ATTR_PROPERTY_NAME));
    Property property = WsdlUtil.getProperty(def, propertyName);
    if (property == null) {
      // patch missing property
      property = (Property) extReg.createExtension(Definition.class, WsdlConstants.Q_PROPERTY);
      property.setQName(propertyName);
      def.addExtensibilityElement(property);
    }
    alias.setProperty(property);

    // messageType attribute
    Attr messageTypeAttr = elem.getAttributeNode(WsdlConstants.ATTR_MESSAGE_TYPE);
    if (messageTypeAttr != null) {
      QName messageType = XmlUtil.getQNameValue(messageTypeAttr);
      Message message = WsdlUtil.getMessage(def, messageType);
      if (message == null) {
        // patch missing message
        message = def.createMessage();
        message.setQName(messageType);
        def.addMessage(message);
      }
      alias.setMessage(message);

      // part attribute, required if the messageType attribute is present
      String part = XmlUtil.getAttribute(elem, WsdlConstants.ATTR_PART);
      if (part == null) {
        WSDLException e = new WSDLException(WSDLException.INVALID_WSDL, "part attribute missing");
        e.setLocation(XPathUtils.getXPathExprFromNode(elem));
        throw e;
      }
      alias.setPart(part);
    }
    else {
      // type attribute
      Attr typeAttr = elem.getAttributeNode(WsdlConstants.ATTR_TYPE);
      if (typeAttr != null) {
        QName typeName = XmlUtil.getQNameValue(typeAttr);
        alias.setType(typeName);
      }
      else {
        // element attribute
        Attr elementAttr = elem.getAttributeNode(WsdlConstants.ATTR_ELEMENT);
        if (elementAttr != null) {
          QName elementName = XmlUtil.getQNameValue(elementAttr);
          alias.setElement(elementName);
        }
      }
    }

    // wsdl:required attribute
    String required = DOMUtils.getAttributeNS(elem, Constants.NS_URI_WSDL, Constants.ATTR_REQUIRED);
    if (required != null)
      alias.setRequired(DatatypeUtil.parseBoolean(required));

    // query element
    Element queryElem = XmlUtil.getElement(elem, WsdlConstants.NS_VPROP, WsdlConstants.ELEM_QUERY);
    if (queryElem != null)
      alias.setQuery(unmarshallQuery(queryElem));

    return alias;
  }

  /**
   * Serializes a {@link PropertyAlias} instance into the given {@link PrintWriter}.
   * @param parentType class object indicating where in the WSDL document this extensibility element
   * was encountered
   * @param elementType the qname of the extensibility element
   * @param extension the instance to serialize
   * @param pw the stream to write in
   * @param def the definition this extensibility element was encountered in
   * @param extReg the extension registry to use (if needed again)
   * @throws WSDLException if serialization fails
   */
  public void marshall(Class parentType, QName elementType, ExtensibilityElement extension,
      PrintWriter pw, Definition def, ExtensionRegistry extReg) throws WSDLException {
    if (extension == null)
      return;

    // open tag
    String tagName = DOMUtils.getQualifiedValue(WsdlConstants.NS_VPROP,
        WsdlConstants.ELEM_PROPERTY_ALIAS, def);
    pw.print("  <" + tagName);
    PropertyAlias alias = (PropertyAlias) extension;

    // property attribute
    DOMUtils.printQualifiedAttribute(WsdlConstants.ATTR_PROPERTY_NAME, alias.getProperty()
        .getQName(), def, pw);

    // message type attribute
    Message message = alias.getMessage();
    if (message != null) {
      DOMUtils.printQualifiedAttribute(WsdlConstants.ATTR_MESSAGE_TYPE, message.getQName(), def, pw);
      DOMUtils.printAttribute(WsdlConstants.ATTR_PART, alias.getPart(), pw);
    }
    else {
      // type attribute
      QName type = alias.getType();
      if (type != null) {
        DOMUtils.printQualifiedAttribute(WsdlConstants.ATTR_TYPE, type, def, pw);
      }
      else {
        // element attribute
        QName element = alias.getElement();
        if (element != null)
          DOMUtils.printQualifiedAttribute(WsdlConstants.ATTR_ELEMENT, element, def, pw);
      }
    }

    // wsdl:required attribute
    Boolean required = alias.getRequired();
    if (required != null)
      DOMUtils.printQualifiedAttribute(Constants.Q_ATTR_REQUIRED, required.toString(), def, pw);

    PropertyQuery query = alias.getQuery();
    if (query != null) {
      pw.println('>');
      // query element
      marshallQuery(alias.getQuery(), pw, def);
      // close tag
      pw.println("  </" + tagName + '>');
    }
    else {
      pw.println("/>");
    }
  }

  /**
   * Deserializes a DOM element into a {@link Script} instance.
   * @param queryElem the element to deserialize
   * @return the deserialized instance
   */
  protected PropertyQuery unmarshallQuery(Element queryElem) {
    PropertyQuery query = new PropertyQuery();
    // namespace declarations
    query.setNamespaces(XmlUtil.findNamespaceDeclarations(queryElem));
    // language attribute
    query.setLanguage(XmlUtil.getAttribute(queryElem, WsdlConstants.ATTR_QUERY_LANGUAGE));
    // text content
    query.setText(DatatypeUtil.toString(queryElem));
    return query;
  }

  /**
   * Serializes a {@link PropertyQuery} instance into the given {@link PrintWriter}.
   * @param query the instance to serialize
   * @param pw the stream to write in
   * @param def the definition where the extensibility element appears
   * @throws WSDLException if serialization fails
   */
  protected void marshallQuery(PropertyQuery query, PrintWriter pw, Definition def) throws WSDLException {
    // open tag
    String queryTag = DOMUtils.getQualifiedValue(WsdlConstants.NS_VPROP, WsdlConstants.ELEM_QUERY,
        def);
    pw.print("    <" + queryTag);
    // language attribute
    DOMUtils.printAttribute(WsdlConstants.ATTR_QUERY_LANGUAGE, query.getLanguage(), pw);
    pw.print('>');
    // text content
    pw.print(DOMUtils.cleanString(query.getText()));
    // close tag
    pw.println("</" + queryTag + '>');
  }
}
