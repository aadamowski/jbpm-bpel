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

import java.util.Iterator;

import javax.xml.namespace.QName;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.Text;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.bpel.xml.util.XmlUtil;

/**
 * Utility methods for dealing with SAAJ objects.
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/10/13 02:53:28 $
 */
public class SoapUtil {

  static final String DEFAULT_NAMESPACE_PREFIX = "defaultNS";
  static final String QUALIFIED_VALUE_PREFIX = "valueNS";

  private static final Log log = LogFactory.getLog(SoapUtil.class);
  private static final boolean traceEnabled = log.isTraceEnabled();

  // suppress default constructor, ensuring non-instantiability
  private SoapUtil() {
  }

  /**
   * Gets the first child element of the given SOAP element with the specified local name and a
   * <code>null</code> or empty namespace URI. This overload is necessary as method
   * {@link Node#getNextSibling()} does not behave as expected under some SAAJ implementations.
   * @param parent the parent SOAP element to examine
   * @param localName the local name of the desired child element
   * @return the corresponding child element, or <code>null</code> if there is no match
   */
  public static SOAPElement getElement(SOAPElement parent, String localName) {
    return getElement(parent, null, localName);
  }

  /**
   * Gets the first child element of the given SOAP element with the specified namespace URI and
   * local name. This overload is necessary as method {@link Node#getNextSibling()} does not behave
   * as expected under some SAAJ implementations.
   * @param parent the parent node to examine
   * @param namespaceURI the namespace URI of the desired child element; if <code>null</code>,
   * only elements with a <code>null</code> or empty namespace URI will be considered
   * @param localName the local name of the desired child element
   * @return the corresponding child element, or <code>null</code> if there is no match
   */
  public static SOAPElement getElement(SOAPElement parent, String namespaceURI, String localName) {
    Iterator childIt = parent.getChildElements();
    while (childIt.hasNext()) {
      javax.xml.soap.Node child = (javax.xml.soap.Node) childIt.next();
      if (XmlUtil.nodeNameEquals(child, namespaceURI, localName))
        return (SOAPElement) child;
    }
    return null;
  }

  /**
   * Gets the first child element of the given SOAP element with the specified qualified name. This
   * overload is necessary as method {@link Node#getNextSibling()} does not behave as expected under
   * some SAAJ implementations.
   * @param parent the parent node to examine
   * @param name the qualified name of the desired child element; if
   * <code>name.getNamespaceURI()</code> is empty, only elements with a <code>null</code> or
   * empty namespace URI will be considered
   * @return the corresponding child element, or <code>null</code> if there is no match
   */
  public static SOAPElement getElement(SOAPElement parent, QName name) {
    return getElement(parent, name.getNamespaceURI(), name.getLocalPart());
  }

  /**
   * Gets the first child element of the given SOAP element.
   * @param parent the parent SOAP element to examine
   * @return the corresponding child element, or <code>null</code> if there is no match
   */
  public static SOAPElement getElement(SOAPElement parent) {
    Iterator childIt = parent.getChildElements();
    while (childIt.hasNext()) {
      Object child = childIt.next();
      if (child instanceof SOAPElement)
        return (SOAPElement) child;
    }
    return null;
  }

  public static void setQNameValue(SOAPElement elem, QName value) throws SOAPException {
    elem.setValue(formatQName(value, elem));
  }

  private static String formatQName(QName value, SOAPElement elem) throws SOAPException {
    String namespaceURI = value.getNamespaceURI();
    String localName = value.getLocalPart();

    // easy way out: no namespace
    if (namespaceURI.length() == 0)
      return localName;

    String prefix = SoapUtil.getPrefix(namespaceURI, elem);
    if (prefix == null) {
      String givenPrefix = value.getPrefix();
      prefix = XmlUtil.generatePrefix(givenPrefix.length() > 0 ? givenPrefix
          : QUALIFIED_VALUE_PREFIX, elem);
      elem.addNamespaceDeclaration(prefix, namespaceURI);
    }
    return prefix + ':' + localName;
  }

  public static void copy(Element target, SOAPElement source) {
    if (traceEnabled)
      log.trace("copying from: " + XmlUtil.toTraceString(source));
    // attributes
    XmlUtil.removeAttributes(target);
    copyAttributes(target, source);
    // all namespaces
    copyVisibleNamespaces(target, source);
    XmlUtil.ensureOwnNamespaceDeclared(target);
    // child nodes
    XmlUtil.removeChildNodes(target);
    copyChildNodes(target, source);
    if (traceEnabled)
      log.trace("copied to: " + XmlUtil.toTraceString(target));
  }

  public static void copyVisibleNamespaces(Element target, SOAPElement source) {
    copyNamespaces(target, source, source.getVisibleNamespacePrefixes());
  }

  public static void copyNamespaces(Element target, SOAPElement source) {
    copyNamespaces(target, source, source.getNamespacePrefixes());
  }

  private static void copyNamespaces(Element target, SOAPElement source, Iterator prefixIt) {
    // namespace declarations appear as attributes in the target element
    while (prefixIt.hasNext()) {
      String prefix = (String) prefixIt.next();
      String namespaceURI = source.getNamespaceURI(prefix);
      // BPEL-195: prevent addition matching visible declaration at target
      if (prefix.equals(XmlUtil.getPrefix(namespaceURI, target)))
        continue;
      XmlUtil.addNamespaceDeclaration(target, namespaceURI, prefix);
      if (traceEnabled)
        log.trace("added namespace declaration: " + prefix + "->" + namespaceURI);
    }
  }

  public static void copyAttributes(Element target, SOAPElement source) {
    // easy way out: no attributes to copy
    if (!source.hasAttributes())
      return;
    // traverse attributes
    Iterator attrNameIt = source.getAllAttributes();
    while (attrNameIt.hasNext()) {
      Name attrName = (Name) attrNameIt.next();
      String namespaceURI = attrName.getURI();

      // isn't the attribute a namespace declaration?
      if (BpelConstants.NS_XMLNS.equals(namespaceURI))
        continue;

      // unqualified?
      if (namespaceURI == null || namespaceURI.length() == 0) {
        String localName = attrName.getLocalName();
        target.setAttribute(localName, source.getAttributeValue(attrName));
        if (traceEnabled)
          log.trace("set attribute: " + localName);
      }
      // qualified
      else {
        Attr attr = target.getOwnerDocument().createAttributeNS(namespaceURI,
            attrName.getQualifiedName());
        attr.setValue(source.getAttributeValue(attrName));
        target.setAttributeNodeNS(attr);
        XmlUtil.ensureNamespaceDeclared(attr, namespaceURI, attrName.getPrefix());
        if (traceEnabled)
          log.trace("set attribute: {" + namespaceURI + '}' + attrName.getQualifiedName());
      }
    }
  }

  public static void copyChildNodes(Element target, SOAPElement source) {
    // easy way out: no child nodes to copy
    if (!source.hasChildNodes())
      return;
    // traverse child nodes
    Iterator childIt = source.getChildElements();
    while (childIt.hasNext()) {
      Object child = childIt.next();
      if (child instanceof SOAPElement) {
        copyChildElement(target, (SOAPElement) child);
      }
      else if (child instanceof Text) {
        Text childText = (Text) child;
        String value = childText.getValue();
        target.appendChild(target.getOwnerDocument().createTextNode(value));
        if (traceEnabled)
          log.trace("appended text: " + value);
      }
      else
        log.debug("discarding child: " + child);
    }
  }

  public static void copyChildElement(Element parent, SOAPElement source) {
    // create a DOM element with the same name as the source
    String namespaceURI = source.getNamespaceURI();
    String qualifiedName = source.getNodeName();
    Element target = parent.getOwnerDocument().createElementNS(namespaceURI, qualifiedName);
    parent.appendChild(target);
    if (traceEnabled)
      log.trace("appended element: {" + namespaceURI + '}' + qualifiedName);
    // namespaces
    copyNamespaces(target, source);
    XmlUtil.ensureOwnNamespaceDeclared(target);
    // attributes
    copyAttributes(target, source);
    // child nodes
    copyChildNodes(target, source);
  }

  public static void copy(SOAPElement target, Element source) throws SOAPException {
    if (traceEnabled)
      log.trace("copying from: " + XmlUtil.toTraceString(source));
    // attributes
    removeAttributes(target);
    copyAttributes(target, source);
    // all namespaces
    removeNamespaces(target);
    copyVisibleNamespaces(target, source);
    ensureOwnNamespaceDeclared(target);
    // child nodes
    target.removeContents();
    copyChildNodes(target, source);
    if (traceEnabled)
      log.trace("copied to: " + XmlUtil.toTraceString(target));
  }

  public static void ensureOwnNamespaceDeclared(SOAPElement elem) throws SOAPException {
    ensureNamespaceDeclared(elem, elem.getNamespaceURI(), elem.getPrefix());
  }

  public static void ensureNamespaceDeclared(SOAPElement elem, String namespaceURI, String prefix)
      throws SOAPException {
    if (prefix == null || prefix.length() == 0) {
      if (namespaceURI == null || namespaceURI.length() == 0)
        return; // do not declare the empty namespace

      // verify the given URI is the default namespace
      if (!namespaceURI.equals(elem.getNamespaceURI(""))) {
        // the given URI is not the default namespace, declare locally
        elem.addNamespaceDeclaration("", namespaceURI);
      }
    }
    else {
      if (namespaceURI == null || namespaceURI.length() == 0)
        throw new IllegalArgumentException("namespaceURI cannot be empty unless prefix is empty");

      // verify given prefix is associated to given URI
      if (!namespaceURI.equals(elem.getNamespaceURI(prefix))) {
        // prefix is associated with other/no URI, declare locally
        elem.addNamespaceDeclaration(prefix, namespaceURI);
      }
    }
  }

  public static void copyVisibleNamespaces(SOAPElement target, Element source) throws SOAPException {
    // copy the namespaces declared at the source element
    copyNamespaces(target, source);
    // go up the element hierarchy
    for (Node parent = source.getParentNode(); parent instanceof Element; parent = parent.getParentNode())
      copyNamespaces(target, (Element) parent);
  }

  public static void copyNamespaces(SOAPElement target, Element source) throws SOAPException {
    // easy way out: no attributes
    if (!source.hasAttributes())
      return;
    // traverse attributes to discover namespace declarations
    NamedNodeMap attributes = source.getAttributes();
    for (int i = 0, n = attributes.getLength(); i < n; i++) {
      Node attribute = attributes.item(i);
      // is attribute a namespace declaration?
      if (!BpelConstants.NS_XMLNS.equals(attribute.getNamespaceURI()))
        continue;
      // namespace declaration format xmlns:prefix="namespaceURI" |
      // xmlns="defaultNamespaceURI"
      String namespaceURI = attribute.getNodeValue();
      String prefix = attribute.getLocalName();
      // non-default namespace declaration?
      if (!"xmlns".equals(prefix)) {
        // BPEL-195: prevent addition matching visible declaration at target
        if (namespaceURI.equals(target.getNamespaceURI(prefix)))
          continue;
        target.addNamespaceDeclaration(prefix, namespaceURI);
        if (traceEnabled)
          log.trace("added namespace declaration: " + prefix + "->" + namespaceURI);
      }
      // non-empty default namespace declaration
      else if (namespaceURI.length() > 0) {
        prefix = XmlUtil.generatePrefix(DEFAULT_NAMESPACE_PREFIX, source);
        target.addNamespaceDeclaration(prefix, namespaceURI);
        if (traceEnabled)
          log.trace("reassigned default namespace declaration: " + prefix + "->" + namespaceURI);
      }
    }
  }

  public static void copyAttributes(SOAPElement target, Element source) {
    // easy way out: no attributes
    if (!source.hasAttributes())
      return;
    // traverse attributes
    NamedNodeMap attributes = source.getAttributes();
    for (int i = 0, n = attributes.getLength(); i < n; i++) {
      Node attribute = attributes.item(i);
      String namespaceURI = attribute.getNamespaceURI();
      // isn't the attribute a namespace declaration?
      if (BpelConstants.NS_XMLNS.equals(namespaceURI))
        continue;

      String name = attribute.getNodeName();
      String value = attribute.getNodeValue();
      if (namespaceURI == null) {
        /*
         * use the DOM level 1 method as some SAAJ implementations complain when presented a null
         * namespace URI
         */
        target.setAttribute(name, value);
      }
      else
        target.setAttributeNS(namespaceURI, name, value);

      if (traceEnabled)
        log.trace("set attribute: " + name);
    }
  }

  public static void copyChildNodes(SOAPElement target, Element source) throws SOAPException {
    // easy way out: no child nodes
    if (!source.hasChildNodes())
      return;
    // traverse child nodes
    for (Node child = source.getFirstChild(); child != null; child = child.getNextSibling()) {
      switch (child.getNodeType()) {
      case Node.ELEMENT_NODE: {
        copyChildElement(target, (Element) child);
        break;
      }
      case Node.TEXT_NODE:
      case Node.CDATA_SECTION_NODE: {
        String text = child.getNodeValue();
        // drop whitespace-only text nodes
        if (!StringUtils.isWhitespace(text)) {
          target.addTextNode(text);
          if (traceEnabled)
            log.trace("appended text: " + text);
        }
        break;
      }
      default:
        log.debug("discarding child: " + child);
      }
    }
  }

  public static void copyChildElement(SOAPElement parent, Element source) throws SOAPException {
    String localName = source.getLocalName();
    String prefix = source.getPrefix();
    String namespaceURI = source.getNamespaceURI();

    Name targetName;
    SOAPEnvelope envelope = findEnvelope(parent);

    if (prefix == null || prefix.length() == 0) {
      // source has no prefix, distinguish between no namespace and default namespace
      if (namespaceURI == null || namespaceURI.length() == 0) {
        // no namespace
        targetName = envelope.createName(localName);
        if (traceEnabled)
          log.trace("appended element: " + localName);
      }
      else {
        // default namespace, look for existing prefix at target
        prefix = getPrefix(namespaceURI, parent);

        // no prefix for that namespace?
        if (prefix == null) {
          prefix = XmlUtil.generatePrefix(DEFAULT_NAMESPACE_PREFIX, source);
        }
        // BPEL-195 source maps prefix to another URI?
        else if (!namespaceURI.equals(source.getAttributeNS(BpelConstants.NS_XMLNS, prefix))) {
          prefix = XmlUtil.generatePrefix(prefix, source);
        }

        targetName = envelope.createName(localName, prefix, namespaceURI);
        if (traceEnabled)
          log.trace("added child element: {" + namespaceURI + '}' + prefix + ':' + localName);
      }
    }
    else {
      // source has prefix
      targetName = envelope.createName(localName, prefix, namespaceURI);
      if (traceEnabled)
        log.trace("added child element: {" + namespaceURI + '}' + prefix + ':' + localName);
    }

    SOAPElement target;
    if (parent instanceof SOAPBody) {
      /*
       * jboss-ws4ee throws ClassCastException upon calling the remote endpoint if child elements
       * other than SOAPBodyElements are added to SOAPBody
       */
      SOAPBody body = (SOAPBody) parent;
      target = body.addBodyElement(targetName);
    }
    else
      target = parent.addChildElement(targetName);

    // namespaces
    copyNamespaces(target, source);
    ensureOwnNamespaceDeclared(target);
    // attributes
    copyAttributes(target, source);
    // child nodes
    copyChildNodes(target, source);
  }

  public static SOAPEnvelope findEnvelope(SOAPElement element) throws SOAPException {
    do {
      if (element instanceof SOAPEnvelope)
        return (SOAPEnvelope) element;

      element = element.getParentElement();
    } while (element != null);

    return null;
  }

  public static SOAPElement addChildElement(SOAPElement parent, String localName)
      throws SOAPException {
    // the proper call is addChildElement(localName); however, the WS4EE stack
    // mistakenly adds a child element with parent's namespace URI
    return parent.addChildElement(localName, "", "");
  }

  public static void removeAttributes(SOAPElement elem) {
    if (elem.hasAttributes()) {
      Iterator attrNameIt = elem.getAllAttributes();
      while (attrNameIt.hasNext()) {
        Name attrName = (Name) attrNameIt.next();
        elem.removeAttribute(attrName);
      }
    }
  }

  public static void removeNamespaces(SOAPElement elem) {
    Iterator prefixIt = elem.getNamespacePrefixes();
    while (prefixIt.hasNext()) {
      String prefix = (String) prefixIt.next();
      elem.removeNamespaceDeclaration(prefix);
    }
  }

  public static String getPrefix(String namespaceURI, SOAPElement contextElem) {
    Iterator prefixIt = contextElem.getVisibleNamespacePrefixes();
    while (prefixIt.hasNext()) {
      String prefix = (String) prefixIt.next();
      if (namespaceURI.equals(contextElem.getNamespaceURI(prefix)))
        return prefix;
    }
    return null;
  }
}
