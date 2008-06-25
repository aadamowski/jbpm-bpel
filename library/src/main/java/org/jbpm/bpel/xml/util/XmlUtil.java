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
package org.jbpm.bpel.xml.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.ibm.wsdl.util.xml.DOMUtils;

import org.jbpm.bpel.endpointref.EndpointReference;
import org.jbpm.bpel.graph.exe.BpelFaultException;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.util.ClassLoaderUtil;

/**
 * Utility methods for dealing with JAXP objects.
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/11/25 13:03:14 $
 */
public class XmlUtil {

  static final String QUALIFIED_VALUE_PREFIX = "valueNS";

  private static final Log log = LogFactory.getLog(XmlUtil.class);
  private static final boolean traceEnabled = log.isTraceEnabled();

  private static final String[] schemaSources = createSchemaSources();

  private static ThreadLocal documentBuilderLocal = new ThreadLocal() {

    protected Object initialValue() {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      factory.setValidating(true);
      factory.setCoalescing(true);
      factory.setIgnoringElementContentWhitespace(true);
      factory.setIgnoringComments(true);
      try {
        // set xml schema as the schema language
        factory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage",
            BpelConstants.NS_XML_SCHEMA);
        // set schema sources
        factory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaSource", schemaSources);
      }
      catch (IllegalArgumentException e) {
        log.warn("JAXP implementation does not support XML Schema, "
            + "XML documents will not be checked for grammar errors", e);
      }
      try {
        // validate the document only if a grammar is specified
        factory.setAttribute("http://apache.org/xml/features/validation/dynamic", Boolean.TRUE);
      }
      catch (IllegalArgumentException e) {
        log.warn("JAXP implementation is not Xerces, cannot enable dynamic validation, "
            + "XML documents without schema location will not parse", e);
      }
      try {
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setEntityResolver(LocalEntityResolver.INSTANCE);
        return builder;
      }
      catch (ParserConfigurationException e) {
        // should not happen
        throw new AssertionError(e);
      }
    }
  };

  private static ThreadLocal transformerFactoryLocal = new ThreadLocal() {

    protected Object initialValue() {
      return TransformerFactory.newInstance();
    }
  };

  // suppress default constructor, ensuring non-instantiability
  private XmlUtil() {
  }

  private static String[] createSchemaSources() {
    final String[] schemaResources = { "xml.xsd", "wsdl.xsd", "bpel_2_0.xsd",
        "plnktype_2_0.xsd", "varprop.xsd", "serviceref.xsd", "bpel_1_1.xsd", "plnktype_1_1.xsd",
        "bpel_definition_1_1.xsd", "bpel_deployment_1_1.xsd", "addressing.xsd" };
    String[] schemaSources = new String[schemaResources.length];
    for (int i = 0; i < schemaResources.length; i++)
      schemaSources[i] = XmlUtil.class.getResource(schemaResources[i]).toExternalForm();
    return schemaSources;
  }

  /**
   * Gets the first child element of the given node having the specified local name and a
   * <code>null</code> or empty namespace URI.
   * @param parent the parent node to examine
   * @param localName the local name of the desired child element
   * @return the corresponding child element, or <code>null</code> if there is no match
   */
  public static Element getElement(Node parent, String localName) {
    return getElement(parent, null, localName);
  }

  /**
   * Gets the first child element of the given node having the specified namespace URI and local
   * name.
   * @param parent the parent node to examine
   * @param namespaceURI the namespace URI of the desired child element; if <code>null</code> or
   * empty, only elements with a <code>null</code> or empty namespace URI will be considered
   * @param localName the local name of the desired child element
   * @return the corresponding child element, or <code>null</code> if there is no match
   */
  public static Element getElement(Node parent, String namespaceURI, String localName) {
    for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (nodeNameEquals(child, namespaceURI, localName))
        return (Element) child;
    }
    return null;
  }

  /**
   * Gets the first child element of the given node.
   * @param parent the parent node to examine
   * @return the corresponding child element, or <code>null</code> if there is no match
   */
  public static Element getElement(Node parent) {
    for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child.getNodeType() == Node.ELEMENT_NODE)
        return (Element) child;
    }
    return null;
  }

  /**
   * Gets an iterator over the child elements of the given node with the specified namespace URI and
   * any local name.
   * @param parent the parent node to iterate
   * @param namespaceURI the namespace URI of the desired child elements; if <code>null</code>,
   * only elements with a <code>null</code> or empty namespace URI will be iterated
   * @return an {@link Element} iterator, empty if there is no match
   */
  public static Iterator getElements(Node parent, String namespaceURI) {
    return IteratorUtils.filteredIterator(new NodeIterator(parent), new NamespaceElementPredicate(
        namespaceURI));
  }

  /**
   * Gets an iterator over the child elements of the given node with the specified namespace URI and
   * local name.
   * @param parent the parent node to iterate
   * @param namespaceURI the namespace URI of the desired child elements; if <code>null</code>,
   * only elements with a <code>null</code> or empty namespace URI will be iterated
   * @param localName the local name of the desired child elements
   * @return an {@link Element} iterator, empty if there is no match
   */
  public static Iterator getElements(Node parent, String namespaceURI, String localName) {
    return IteratorUtils.filteredIterator(new NodeIterator(parent),
        new QualifiedNameElementPredicate(namespaceURI, localName));
  }

  /**
   * Gets an iterator over the child elements of the given node.
   * @param parent the parent node to iterate
   * @return an {@link Element} iterator, empty if there is no match
   */
  public static Iterator getElements(Node parent) {
    return IteratorUtils.filteredIterator(new NodeIterator(parent), ElementPredicate.INSTANCE);
  }

  /**
   * Counts the child elements of the given node with the specified namespace URI and local name.
   * @param parent the parent node to iterate
   * @param namespaceURI the namespace URI of the desired child elements; if <code>null</code>,
   * only elements with a <code>null</code> or empty namespace URI will be iterated
   * @param localName the local name of the desired child elements
   * @return the element count, non-negative
   */
  public static int countElements(Node parent, String namespaceURI, String localName) {
    int count = 0;
    for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (nodeNameEquals(child, namespaceURI, localName))
        count++;
    }
    return count;
  }

  /**
   * Gets an attribute value of the given element.
   * @param ownerElem the element that owns the attribute
   * @param attrName the name of the attribute to retrieve
   * @return the attribute value as a string, or <code>null</code> if that attribute does not have
   * a specified value
   */
  public static String getAttribute(Element ownerElem, String attrName) {
    Attr attribute = ownerElem.getAttributeNode(attrName);
    return attribute != null ? attribute.getValue() : null;
  }

  /**
   * Parses the string argument as a {@linkplain QName qualified name} in the context of the given
   * node.
   * @param prefixedName a name that may contain a colon character
   * @param contextNode the node to search for namespace declarations
   * @return a qualified name constructed with the following arguments:
   * <ul>
   * <li><code>localPart</code> substring of <code>prefixedName</code> after the first colon</li>
   * <li><code>prefix</code> substring of <code>prefixedName</code> before the first colon</li>
   * <li><code>namespaceURI</code> namespace associated with <code>prefix</code> in the
   * <code>contextNode</code></li>
   * </ul>
   */
  public static QName parseQName(String prefixedName, Node contextNode) {
    int index = prefixedName.indexOf(':');
    // no colon?
    if (index == -1)
      return new QName(prefixedName); // local name

    String prefix = prefixedName.substring(0, index);
    return new QName(getNamespaceURI(prefix, contextNode), prefixedName.substring(index + 1),
        prefix);
  }

  /**
   * Parses the text content of the given element as a {@linkplain QName qualified name}.
   * @param elem the element whose text content will be parsed
   * @return the element text content as a {@link QName}
   */
  public static QName getQNameValue(Element elem) {
    return parseQName(DatatypeUtil.toString(elem), elem);
  }

  /**
   * Parses the value of the given attribute as a {@linkplain QName qualified name}.
   * @param attr the attribute whose value will be parsed
   * @return the attribute value as a {@link QName}
   */
  public static QName getQNameValue(Attr attr) {
    return parseQName(attr.getValue(), attr.getOwnerElement());
  }

  /**
   * Tells whether the name of the specified node matches the given qualified name.
   * @param node the node to examine
   * @param name the qualified name to match
   * @return <code>true</code> if the qualified name matches, <code>false</code> otherwise
   */
  public static boolean nodeNameEquals(Node node, QName name) {
    return nodeNameEquals(node, name.getNamespaceURI(), name.getLocalPart());
  }

  /**
   * Tells whether the name of the specified node matches the given namespace URI and local name.
   * @param node the node to examine
   * @param namespaceURI the namespace URI to match
   * @param localName the local name to match
   * @return <code>true</code> if the namespace URI and local name match, <code>false</code>
   * otherwise
   */
  public static boolean nodeNameEquals(Node node, String namespaceURI, String localName) {
    return nodeNamespaceURIEquals(node, namespaceURI) && localName.equals(node.getLocalName());
  }

  /**
   * Tells whether the namespace URI of the specified node matches the given namespace URI.
   * @param node the node to examine
   * @param namespaceURI the namespace URI to match
   * @return <code>true</code> if the namespace URI matches, <code>false</code> otherwise
   */
  public static boolean nodeNamespaceURIEquals(Node node, String namespaceURI) {
    String nodeNamespaceURI = node.getNamespaceURI();
    return nodeNamespaceURI == null || nodeNamespaceURI.length() == 0 ? namespaceURI == null
        || namespaceURI.length() == 0 : nodeNamespaceURI.equals(namespaceURI);
  }

  public static void setObjectValue(Node node, Object value) {
    switch (node.getNodeType()) {
    case Node.ELEMENT_NODE:
      setObjectValue((Element) node, value);
      break;
    case Node.DOCUMENT_NODE:
      setObjectValue(((Document) node).getDocumentElement(), value);
      break;
    default:
      // BPEL-243 throw selectionFailure if the source is an EII with xsi:nil=true
      if (value instanceof Element) {
        String nil = ((Element) value).getAttributeNS(BpelConstants.NS_XML_SCHEMA_INSTANCE,
            BpelConstants.ATTR_NIL);
        if (DatatypeUtil.parseBoolean(nil) == Boolean.TRUE)
          throw new BpelFaultException(BpelConstants.FAULT_SELECTION_FAILURE);
      }
      // replace content
      node.setNodeValue(DatatypeUtil.toString(value));
    }
  }

  public static void setObjectValue(Element elem, Object value) {
    if (value instanceof Node) {
      switch (((Node) value).getNodeType()) {
      case Node.ELEMENT_NODE:
        // replace element
        copy(elem, (Element) value);
        break;
      case Node.DOCUMENT_NODE:
        // replace element
        copy(elem, ((Document) value).getDocumentElement());
        break;
      default:
        // replace content
        setStringValue(elem, ((Node) value).getNodeValue());
      }
    }
    else if (value instanceof EndpointReference) {
      // replace element
      ((EndpointReference) value).writeServiceRef(elem);
    }
    else {
      // replace content
      setStringValue(elem, DatatypeUtil.toString(value));
    }
  }

  public static void setStringValue(Element elem, String value) {
    // remove jbpm:initialized
    elem.removeAttributeNS(BpelConstants.NS_VENDOR, BpelConstants.ATTR_INITIALIZED);

    Node firstChild = elem.getFirstChild();
    // if first child is a text node, reuse it
    if (firstChild instanceof org.w3c.dom.Text)
      firstChild.setNodeValue(value);
    // otherwise, just create a new text node
    else
      firstChild = elem.getOwnerDocument().createTextNode(value);

    // remove all children
    removeChildNodes(elem);
    // append text
    elem.appendChild(firstChild);
  }

  public static void setQNameValue(Element elem, QName value) {
    setStringValue(elem, formatQName(value, elem));
  }

  public static void setQNameValue(Attr attr, QName value) {
    attr.setValue(formatQName(value, attr.getOwnerElement()));
  }

  private static String formatQName(QName value, Element elem) {
    String namespace = value.getNamespaceURI();
    String localName = value.getLocalPart();

    // easy way out: no namespace
    if (namespace.length() == 0)
      return localName;

    String prefix = getPrefix(namespace, elem);
    if (prefix == null) {
      String givenPrefix = value.getPrefix();
      prefix = generatePrefix(givenPrefix.length() > 0 ? givenPrefix : QUALIFIED_VALUE_PREFIX, elem);
      addNamespaceDeclaration(elem, namespace, prefix);
    }
    return prefix + ':' + localName;
  }

  public static String toTraceString(Element elem) {
    String namespace = elem.getNamespaceURI();
    String localName = elem.getLocalName();

    // easy way out: no namespace
    if (namespace == null || namespace.length() == 0)
      return localName;

    StringBuffer traceBuffer = new StringBuffer(namespace.length() + localName.length());
    traceBuffer.append('{').append(namespace).append('}');

    String prefix = elem.getPrefix();
    if (prefix != null && prefix.length() > 0)
      traceBuffer.append(prefix).append(':');

    return traceBuffer.append(localName).toString();
  }

  public static void copy(Element target, Element source) {
    if (traceEnabled)
      log.trace("copying from: " + toTraceString(source));
    // attributes
    removeAttributes(target);
    copyAttributes(target, source);
    // all namespaces
    copyVisibleNamespaces(target, source);
    ensureOwnNamespaceDeclared(target);
    // child nodes
    removeChildNodes(target);
    copyChildNodes(target, source);
    if (traceEnabled)
      log.trace("copied to: " + toTraceString(target));
  }

  public static void copyVisibleNamespaces(final Element target, Element source) {
    // copy namespaces declared at source element
    copyNamespaces(target, source);
    // go up the element hierarchy
    for (Node parent = source.getParentNode(); parent instanceof Element; parent = parent.getParentNode())
      copyNamespaces(target, (Element) parent);
  }

  public static void copyNamespaces(final Element target, Element source) {
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
      // namespace declaration format
      // xmlns:prefix="namespaceURI" | xmlns="defaultNamespaceURI"
      String namespaceURI = attribute.getNodeValue();
      String prefix = attribute.getLocalName();
      // default namespace declaration?
      if ("xmlns".equals(prefix)) {
        // BPEL-195: prevent addition matching visible declaration at target
        if ("".equals(getPrefix(namespaceURI, target)))
          continue;
        addNamespaceDeclaration(target, namespaceURI);
        if (traceEnabled)
          log.trace("added default namespace declaration: " + namespaceURI);
      }
      else {
        // BPEL-195: prevent addition matching visible declaration at target
        if (prefix.equals(getPrefix(namespaceURI, target)))
          continue;
        addNamespaceDeclaration(target, namespaceURI, prefix);
        if (traceEnabled)
          log.trace("added namespace declaration: " + prefix + "->" + namespaceURI);
      }
    }
  }

  public static void copyAttributes(Element target, Element source) {
    // easy way out: no attributes
    if (!source.hasAttributes())
      return;
    // traverse attributes
    NamedNodeMap attributes = source.getAttributes();
    for (int i = 0, n = attributes.getLength(); i < n; i++) {
      Node sourceAttr = attributes.item(i);
      String namespaceURI = sourceAttr.getNamespaceURI();
      String name = sourceAttr.getNodeName();
      // namespace declaration?
      if (BpelConstants.NS_XMLNS.equals(namespaceURI))
        continue;
      // unqualified?
      if (namespaceURI == null || namespaceURI.length() == 0) {
        target.setAttribute(name, sourceAttr.getNodeValue());
        if (traceEnabled)
          log.trace("set attribute: " + name);
      }
      // qualified
      else {
        Attr targetAttr = target.getOwnerDocument().createAttributeNS(namespaceURI, name);
        targetAttr.setValue(sourceAttr.getNodeValue());
        target.setAttributeNodeNS(targetAttr);
        ensureNamespaceDeclared(targetAttr, namespaceURI, sourceAttr.getPrefix());
        if (traceEnabled)
          log.trace("set attribute: {" + namespaceURI + '}' + name);
      }
    }
  }

  public static void copyChildNodes(Element target, Element source) {
    Document targetDoc = target.getOwnerDocument();
    for (Node child = source.getFirstChild(); child != null; child = child.getNextSibling()) {
      switch (child.getNodeType()) {
      case Node.ELEMENT_NODE:
        copyChildElement(target, (Element) child);
        break;
      case Node.TEXT_NODE:
      case Node.CDATA_SECTION_NODE:
        target.appendChild(targetDoc.createTextNode(child.getNodeValue()));
        if (traceEnabled)
          log.trace("appended text: " + child.getNodeValue());
        break;
      default:
        log.debug("discarding child: " + child);
      }
    }
  }

  public static void copyChildElement(Element parent, Element source) {
    Element target = (Element) parent.getOwnerDocument().importNode(source, true);
    parent.appendChild(target);
    // ensure namespace declared after appending in order to consider all visible declarations
    ensureOwnNamespaceDeclared(target);
    if (traceEnabled)
      log.trace("appended element: " + toTraceString(target));
  }

  /**
   * Ensures the given namespace URI is associated to the given prefix in the scope of the given
   * attribute. If the namespace URI is <em>not</em> associated to the prefix, a namespace
   * declaration is {@linkplain #addNamespaceDeclaration(Element, String, String) added} to the
   * attribute's owner element. Otherwise, this call has no effect.
   * @param attribute the attribute to verify
   * @param namespaceURI the namespace URI to ensure; cannot be <code>null</code> or empty
   * @param prefix the prefix to ensure; cannot be <code>null</code> or empty
   */
  public static void ensureNamespaceDeclared(Attr attribute, String namespaceURI, String prefix) {
    if (prefix == null || prefix.length() == 0)
      throw new IllegalArgumentException("prefix cannot be empty");

    if (namespaceURI == null || namespaceURI.length() == 0)
      throw new IllegalArgumentException("namespaceURI cannot be empty");

    Element element = attribute.getOwnerElement();
    if (!prefix.equals(getPrefix(namespaceURI, element))) {
      // prefix is associated with other/no URI, declare locally
      addNamespaceDeclaration(element, namespaceURI, prefix);
    }
  }

  /**
   * Ensures the given namespace URI is associated to the given prefix in the scope of the given
   * element. If the namespace URI is <em>not</em> associated to the prefix, a namespace
   * declaration is {@linkplain #addNamespaceDeclaration(Element, String, String) added} to the
   * element. Otherwise, this call has no effect.
   * @param element the element to verify
   * @param namespaceURI the namespace URI to ensure; cannot be <code>null</code> or empty unless
   * <code>prefix</code> is <code>null</code> or empty as well
   * @param prefix the prefix to ensure; if <code>null</code> or empty, the given namespace URI is
   * ensured to be the default
   * @throws IllegalArgumentException if <code>namespaceURI</code> is <code>null</code> or
   * empty, <em>unless</em> <code>prefix</code> is empty as well
   */
  public static void ensureNamespaceDeclared(Element element, String namespaceURI, String prefix) {
    if (prefix == null || prefix.length() == 0) {
      if (namespaceURI == null)
        namespaceURI = "";

      // verify the given URI is the default namespace
      if (!"".equals(getPrefix(namespaceURI, element))) {
        // the given URI is not the default namespace, declare locally
        addNamespaceDeclaration(element, namespaceURI);
      }
    }
    else {
      if (namespaceURI == null || namespaceURI.length() == 0)
        throw new IllegalArgumentException("namespaceURI cannot be empty unless prefix is empty");

      // verify given prefix is associated to given URI
      if (!prefix.equals(getPrefix(namespaceURI, element))) {
        // prefix is associated with other/no URI, declare locally
        addNamespaceDeclaration(element, namespaceURI, prefix);
      }
    }
  }

  public static void ensureOwnNamespaceDeclared(Element elem) {
    ensureNamespaceDeclared(elem, elem.getNamespaceURI(), elem.getPrefix());
  }

  public static Node appendForeignChild(Node node, Node foreignChild) {
    return node.appendChild(node.getOwnerDocument().importNode(foreignChild, true));
  }

  public static void addNamespaceDeclaration(Element elem, String namespaceURI) {
    elem.setAttributeNS(BpelConstants.NS_XMLNS, "xmlns", namespaceURI);
  }

  public static void addNamespaceDeclaration(Element elem, String namespaceURI, String prefix) {
    elem.setAttributeNS(BpelConstants.NS_XMLNS, "xmlns:" + prefix, namespaceURI);
  }

  public static void removeAttributes(Element elem) {
    if (elem.hasAttributes()) {
      NamedNodeMap attributeMap = elem.getAttributes();
      // since node maps are live, hold attributes in a separate collection
      int n = attributeMap.getLength();
      Attr[] attributes = new Attr[n];
      for (int i = 0; i < n; i++)
        attributes[i] = (Attr) attributeMap.item(i);
      // now remove each attribute from the element
      for (int i = 0; i < n; i++)
        elem.removeAttributeNode(attributes[i]);
    }
  }

  public static void removeChildNodes(Node node) {
    for (Node current = node.getFirstChild(), next; current != null; current = next) {
      next = current.getNextSibling();
      node.removeChild(current);
    }
  }

  public static Document createDocument() {
    return getDocumentBuilder().newDocument();
  }

  /**
   * Creates an element with the given qualified name. The returned element will be set as the
   * document element of its owner document.
   * @param name the qualified name of the element to create
   * @return a new element with the specified <code>name</code>
   */
  public static Element createElement(QName name) {
    String namespace = name.getNamespaceURI();
    String localName = name.getLocalPart();
    if (namespace.length() == 0)
      return createElement(localName);

    String prefix = name.getPrefix();
    return createElement(namespace, prefix.length() > 0 ? prefix + ':' + localName : localName);
  }

  /**
   * Creates an element with the given namespace URI and prefixed name. The returned element will be
   * set as the document element of its owner document.
   * @param namespaceURI the namespace URI of the element to create
   * @param prefixedName the prefixed name of the element to instantiate
   * @return a new element with the specified <code>namespaceURI</code> and
   * <code>prefixedName</code>.
   */
  public static Element createElement(String namespaceURI, String prefixedName) {
    Document doc = createDocument();
    Element elem = doc.createElementNS(namespaceURI, prefixedName);
    doc.appendChild(elem);

    // some TrAX implementations do not fix up namespaces, declare namespace
    String prefix = elem.getPrefix();
    if (prefix != null)
      addNamespaceDeclaration(elem, namespaceURI, prefix);
    else
      addNamespaceDeclaration(elem, namespaceURI);

    return elem;
  }

  /**
   * Creates an element with the given local name and neither namespace URI nor prefix. The returned
   * element will be set as the document element of its owner document.
   * @param localName the local name of the element to create
   * @return a new element with the specified <code>localName</code>
   */
  public static Element createElement(String localName) {
    Document doc = createDocument();
    Element elem = doc.createElementNS(null, localName);
    doc.appendChild(elem);
    return elem;
  }

  /**
   * Parses the XML document contained in the given string into a DOM document.
   * @param text a string containing the document to parse
   * @return a new DOM document representing the XML content
   * @throws SAXException if any parse errors occur
   */
  public static Element parseText(String text) throws SAXException {
    try {
      return getDocumentBuilder().parse(new InputSource(new StringReader(text)))
          .getDocumentElement();
    }
    catch (IOException e) {
      // cannot happen, string reader doesn't throw I/O exceptions unless closed
      throw new AssertionError(e);
    }
  }

  public static Element parseResource(String resource) throws SAXException, IOException {
    return parseResource(resource, ClassLoaderUtil.getClassLoader().getResource(resource));
  }

  public static DOMSource parseResource(String resource, Class cl) throws SAXException, IOException {
    URL resourceURL = cl.getResource(resource);
    Element elem = parseResource(resource, resourceURL);
    return new DOMSource(elem, resourceURL.toExternalForm());
  }

  private static Element parseResource(String resource, URL location) throws SAXException,
      IOException {
    if (location == null)
      throw new FileNotFoundException(resource);

    InputStream inputStream = location.openStream();
    try {
      return getDocumentBuilder().parse(inputStream, resource).getDocumentElement();
    }
    finally {
      inputStream.close();
    }
  }

  /**
   * Gets a validating document builder local to the current thread.
   * @return a thread-local document builder
   */
  public static DocumentBuilder getDocumentBuilder() {
    return (DocumentBuilder) documentBuilderLocal.get();
  }

  public static Map findNamespaceDeclarations(Element elem) {
    Map namespaces = new HashMap();

    findLocalNamespaceDeclarations(elem, namespaces);

    // go up the parent hierarchy
    for (Node parent = elem.getParentNode(); parent instanceof Element; parent = parent.getParentNode())
      findLocalNamespaceDeclarations((Element) parent, namespaces);

    return namespaces;
  }

  private static void findLocalNamespaceDeclarations(Element elem, Map namespaces) {
    // traverse attributes to discover namespace declarations
    NamedNodeMap attributes = elem.getAttributes();
    for (int i = 0, n = attributes.getLength(); i < n; i++) {
      Node attribute = attributes.item(i);

      // is attribute a namespace declaration?
      if (!BpelConstants.NS_XMLNS.equals(attribute.getNamespaceURI()))
        continue;

      // namespace declaration format:
      // xmlns:prefix="namespaceURI" | xmlns="defaultNamespaceURI"
      String prefix = attribute.getLocalName();

      // exclude default and overriden namespace declarations
      if ("xmlns".equals(prefix) || namespaces.containsKey(prefix))
        continue;

      String namespaceURI = attribute.getNodeValue();
      namespaces.put(prefix, namespaceURI);
    }
  }

  /**
   * Retrieves the prefix associated with a namespace URI in the given context node.
   * @param namespaceURI the namespace whose prefix is required
   * @param contextNode the node where to search for namespace declarations
   * @return the prefix associated with the namespace URI; the empty string indicates the default
   * namespace, while <code>null</code> indicates no association
   */
  public static String getPrefix(String namespaceURI, Node contextNode) {
    switch (contextNode.getNodeType()) {
    case Node.ATTRIBUTE_NODE:
      contextNode = ((Attr) contextNode).getOwnerElement();
      break;
    case Node.ELEMENT_NODE:
      break;
    default:
      contextNode = contextNode.getParentNode();
    }

    while (contextNode != null && contextNode.getNodeType() == Node.ELEMENT_NODE) {
      NamedNodeMap attributes = contextNode.getAttributes();
      for (int i = 0, n = attributes.getLength(); i < n; i++) {
        Node attr = attributes.item(i);

        // is attribute a namespace declaration and matches the given URI?
        if (BpelConstants.NS_XMLNS.equals(attr.getNamespaceURI())
            && namespaceURI.equals(attr.getNodeValue())) {
          String prefix = attr.getLocalName();
          return "xmlns".equals(prefix) ? "" : prefix;
        }
      }
      contextNode = contextNode.getParentNode();
    }
    return null;
  }

  public static String getNamespaceURI(String prefix, Node contextNode) {
    return DOMUtils.getNamespaceURIFromPrefix(contextNode, prefix);
  }

  public static String generatePrefix(String base, Element contextElem) {
    // check possible collision with namespace declarations
    if (!contextElem.hasAttributeNS(BpelConstants.NS_XMLNS, base))
      return base;

    // collision detected, append a discriminator number
    StringBuffer prefixBuffer = new StringBuffer(base);

    for (int i = 1; i < Integer.MAX_VALUE; i++) {
      String prefix = prefixBuffer.append(i).toString();

      if (!contextElem.hasAttributeNS(BpelConstants.NS_XMLNS, prefix))
        return prefix;

      // remove appended number
      prefixBuffer.setLength(base.length());
    }

    throw new Error("could not generate prefix from base: " + base);
  }

  public static TransformerFactory getTransformerFactory() {
    return (TransformerFactory) transformerFactoryLocal.get();
  }

  public static Templates createTemplates(URL templateURL) throws TransformerException {
    return getTransformerFactory().newTemplates(
        new SAXSource(new InputSource(templateURL.toExternalForm())));
  }

  public static void writeFile(Node node, File file) throws IOException {
    try {
      // prepare identity transformer
      Transformer xmlWriter = XmlUtil.getTransformerFactory().newTransformer();

      // prepare output stream
      OutputStream fileSink = new BufferedOutputStream(new FileOutputStream(file));
      try {
        xmlWriter.transform(new DOMSource(node), new StreamResult(fileSink));
      }
      finally {
        fileSink.close();
      }
    }
    catch (TransformerException e) {
      Throwable cause = e.getCause();
      if (cause instanceof IOException)
        throw (IOException) cause;
      // should not happen
      throw new AssertionError(e);
    }
  }

  private static class ElementPredicate implements Predicate {

    static final Predicate INSTANCE = new ElementPredicate();

    private ElementPredicate() {
    }

    public boolean evaluate(Object arg) {
      return evaluate((Node) arg);
    }

    public static boolean evaluate(Node node) {
      return node.getNodeType() == Node.ELEMENT_NODE;
    }
  }

  private static class NamespaceElementPredicate implements Predicate {

    private final String namespaceURI;

    NamespaceElementPredicate(String namespaceURI) {
      this.namespaceURI = namespaceURI;
    }

    public boolean evaluate(Object arg) {
      return evaluate((Node) arg, namespaceURI);
    }

    public static boolean evaluate(Node node, String namespaceURI) {
      return node.getNodeType() == Node.ELEMENT_NODE && nodeNamespaceURIEquals(node, namespaceURI);
    }
  }

  private static class QualifiedNameElementPredicate implements Predicate {

    private final String namespaceURI;
    private final String localName;

    QualifiedNameElementPredicate(String namespaceURI, String localName) {
      this.namespaceURI = namespaceURI;
      this.localName = localName;
    }

    public boolean evaluate(Object arg) {
      return evaluate((Node) arg, namespaceURI, localName);
    }

    public static boolean evaluate(Node node, String namespaceURI, String localName) {
      return node.getNodeType() == Node.ELEMENT_NODE
          && nodeNameEquals(node, namespaceURI, localName);
    }
  }
}
