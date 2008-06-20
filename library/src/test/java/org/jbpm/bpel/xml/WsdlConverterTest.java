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
package org.jbpm.bpel.xml;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;

import javax.wsdl.Definition;
import javax.wsdl.WSDLException;
import javax.wsdl.factory.WSDLFactory;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import junit.framework.TestCase;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.ibm.wsdl.Constants;
import com.ibm.wsdl.util.xml.DOMUtils;

import org.jbpm.bpel.wsdl.xml.WsdlConstants;
import org.jbpm.bpel.wsdl.xml.WsdlUtil;
import org.jbpm.bpel.xml.util.DatatypeUtil;
import org.jbpm.bpel.xml.util.XmlUtil;

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/09/04 06:42:26 $
 */
public class WsdlConverterTest extends TestCase {

  private static final String TNS_URI = "http://www.manufacturing.org";

  public void testMultipleNamespacePrefixes() throws Exception {
    String xml = "<definitions xmlns='"
        + Constants.NS_URI_WSDL
        + "' xmlns:bpws='"
        + BpelConstants.NS_BPEL_1_1
        + "' xmlns:mice='http://rodents.net' xmlns:rats='http://rodents.net'>"
        + " <bpws:property name='mouseName' type='mice:rodentName'/>"
        + " <bpws:property name='ratName' type='rats:rodentName'/>"
        + " <message name='amessage'/>"
        + "</definitions>";
    Element definitions = transform(xml);
    // properties
    Iterator propertyIt = XmlUtil.getElements(definitions, WsdlConstants.NS_VPROP,
        WsdlConstants.ELEM_PROPERTY);
    QName rodentName = new QName("http://rodents.net", "rodentName");
    // mouseName
    Element property = (Element) propertyIt.next();
    QName name = XmlUtil.getQNameValue(property.getAttributeNode("type"));
    assertEquals(rodentName, name);
    // ratName
    property = (Element) propertyIt.next();
    name = XmlUtil.getQNameValue(property.getAttributeNode("type"));
    assertEquals(rodentName, name);
  }

  // ///////////////////// Renamed Elements

  public void testRole() throws Exception {
    String xml = "<plnk:role name='schedulingRequester'>"
        + " <plnk:portType name='tns:schedulingCallbackPT'/>"
        + "</plnk:role>";
    Element role = transformWrap(xml);
    assertNull(XmlUtil.getElement(role, WsdlConstants.NS_PLNK_1_1, "portType"));
    assertEquals("schedulingRequester", role.getAttribute("name"));
    assertEquals("tns:schedulingCallbackPT", role.getAttribute("portType"));
  }

  public void testPropertyAliasNoQuery() throws Exception {
    String xml = "<bpws:propertyAlias messageType='tns:POMessage'"
        + " propertyName='tns:invoiceId' part='PO'/>";
    Element propertyAlias = transformWrap(xml);
    // property name
    QName propertyName = XmlUtil.getQNameValue(propertyAlias.getAttributeNode("propertyName"));
    assertEquals("invoiceId", propertyName.getLocalPart());
    assertEquals(TNS_URI, propertyName.getNamespaceURI());
    // message type
    QName messageType = XmlUtil.getQNameValue(propertyAlias.getAttributeNode("messageType"));
    assertEquals("POMessage", messageType.getLocalPart());
    assertEquals(TNS_URI, messageType.getNamespaceURI());
    // part
    assertEquals("PO", propertyAlias.getAttribute("part"));
    // no query
    assertNull(XmlUtil.getElement(propertyAlias, WsdlConstants.NS_VPROP, "query"));
  }

  public void testPropertyAliasQuery() throws Exception {
    String xml = "<bpws:propertyAlias propertyName='tns:invoiceId'"
        + " messageType='tns:POMessage' part='PO' query='/PO/tns:orderNumber'"
        + " xmlns:tns='http://www.mecachi.com'/>";
    Element propertyAlias = transformWrap(xml);
    // no query attribute
    assertFalse(propertyAlias.hasAttribute("query"));
    // query subelement
    Element query = XmlUtil.getElement(propertyAlias, WsdlConstants.NS_VPROP, "query");
    assertEquals("/PO/tns:orderNumber", DatatypeUtil.toString(query));
    // namespaces
    assertEquals(BpelConstants.NS_BPEL_1_1, DOMUtils.getNamespaceURIFromPrefix(query, "bpws"));
    assertEquals("http://www.mecachi.com", DOMUtils.getNamespaceURIFromPrefix(query, "tns"));
  }

  public void testWriteConvertedDocument() throws Exception {
    String documentURI = getClass().getResource("propertyAliasSample-1_1.wsdl").toString();
    Element definitionsElem = transform(new StreamSource(documentURI));
    WSDLFactory wsdlFactory = WsdlUtil.getFactory();
    Definition definition = wsdlFactory.newWSDLReader().readWSDL(documentURI,
        definitionsElem.getOwnerDocument());
    try {
      wsdlFactory.newWSDLWriter().writeWSDL(definition, System.out);
    }
    catch (WSDLException e) {
      e.printStackTrace();
      fail("converted definitions must be writable");
    }
  }

  public static Element transformWrap(String xmlText) throws TransformerException, SAXException {
    String wrappedText = "<definitions xmlns='"
        + Constants.NS_URI_WSDL
        + "' xmlns:plnk='"
        + WsdlConstants.NS_PLNK_1_1
        + "' xmlns:bpws='"
        + BpelConstants.NS_BPEL_1_1
        + "' xmlns:tns='"
        + TNS_URI
        + "'>"
        + xmlText
        + "</definitions>";
    return XmlUtil.getElement(transform(wrappedText));
  }

  public static Element transform(String xmlText) throws TransformerException, SAXException {
    return transform(new StreamSource(new StringReader(xmlText)));
  }

  public static Element transform(Source source) throws TransformerException, SAXException {
    StringWriter sink = new StringWriter();
    ProcessWsdlLocator.getWsdlUpgradeTemplates().newTransformer().transform(source,
        new StreamResult(sink));
    String textResult = sink.toString();
    System.out.println(textResult);
    return XmlUtil.parseText(textResult);
  }
}
