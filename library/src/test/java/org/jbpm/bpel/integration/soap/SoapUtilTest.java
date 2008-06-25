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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPMessage;

import junit.framework.TestCase;

import org.apache.commons.collections.IteratorUtils;
import org.w3c.dom.Element;

import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.bpel.xml.util.DatatypeUtil;
import org.jbpm.bpel.xml.util.XmlUtil;
import org.jbpm.bpel.xml.util.XmlUtilTest;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/11/25 13:03:15 $
 */
public class SoapUtilTest extends TestCase {

  public void testRemoveAttributes_soap() throws Exception {
    String xml = "<soap:Envelope xmlns:soap='"
        + SOAPConstants.URI_NS_SOAP_ENVELOPE
        + "'>"
        + " <soap:Body xmlns:fish='urn:example:fish'>"
        + "  <lunch time='1200' produce:lettuce='0.1lb' fish:fillet='0.25lb' "
        + "   xmlns:produce='urn:example:produce' />"
        + " </soap:Body>"
        + "</soap:Envelope>";
    ByteArrayInputStream sourceStream = new ByteArrayInputStream(xml.getBytes());
    SOAPMessage soapMessage = MessageFactory.newInstance().createMessage(null, sourceStream);
    SOAPElement element = SoapUtil.getElement(soapMessage.getSOAPBody(), "lunch");
    // remove the attributes
    SoapUtil.removeAttributes(element);
    // verify remotion with the dom & saaj apis
    assertFalse(element.hasAttribute("time"));
    assertFalse(element.hasAttributeNS("urn:example:produce", "lettuce"));
    assertFalse(element.hasAttributeNS("urn:example:fish", "fillet"));
    // namespaces should still be there
    // prefixed declaration
    assertEquals("produce", SoapUtil.getPrefix("urn:example:produce", element));
    // parent prefixed declaration
    assertEquals("fish", SoapUtil.getPrefix("urn:example:fish", element));
  }

  public void testRemoveChildNodes_soap() throws Exception {
    String xml = "<soap:Envelope xmlns:soap='"
        + SOAPConstants.URI_NS_SOAP_ENVELOPE
        + "'>"
        + " <soap:Body xmlns:fish='urn:example:fish'>"
        + "  <meal:lunch xmlns:produce='urn:example:produce'"
        + "   xmlns:meal='urn:example:meal'>"
        + "   <time>1200</time>"
        + "   <produce:lettuce>0.1lb</produce:lettuce>"
        + "   <fish:fillet xmlns:fish='urn:example:fish'>0.25lb</fish:fillet>"
        + "  </meal:lunch>"
        + " </soap:Body>"
        + "</soap:Envelope>";
    ByteArrayInputStream sourceStream = new ByteArrayInputStream(xml.getBytes());
    SOAPMessage soapMessage = MessageFactory.newInstance().createMessage(null, sourceStream);
    SOAPElement element = SoapUtil.getElement(soapMessage.getSOAPBody(), "urn:example:meal",
        "lunch");
    // remove the child nodes
    element.removeContents();
    // verify remotion
    assertFalse(element.getChildElements().hasNext());
  }

  public void testRemoveNamespaces_soap() throws Exception {
    String xml = "<soap:Envelope xmlns:soap='"
        + SOAPConstants.URI_NS_SOAP_ENVELOPE
        + "'>"
        + " <soap:Body xmlns:fish='urn:example:fish'>"
        + "  <lunch time='1200' produce:lettuce='0.1lb' fish:fillet='0.25lb' "
        + "   xmlns:produce='urn:example:produce' />"
        + " </soap:Body>"
        + "</soap:Envelope>";
    ByteArrayInputStream sourceStream = new ByteArrayInputStream(xml.getBytes());
    SOAPMessage soapMessage = MessageFactory.newInstance().createMessage(null, sourceStream);
    SOAPElement element = SoapUtil.getElement(soapMessage.getSOAPBody(), "lunch");
    // remove namespaces
    SoapUtil.removeNamespaces(element);
    // verify remotion
    assertFalse(element.getNamespacePrefixes().hasNext());
    // attributes should still be there
    // qualified attributes
    assertEquals("0.1lb", element.getAttributeNS("urn:example:produce", "lettuce"));
    assertEquals("0.25lb", element.getAttributeNS("urn:example:fish", "fillet"));
    // local attribute
    assertEquals("1200", element.getAttribute("time"));
  }

  public void testCopyAttributes_soapDom() throws Exception {
    String xml = "<lunch time='1200' produce:lettuce='0.1lb' fish:fillet='0.25lb' "
        + " xmlns:produce='urn:example:produce' xmlns:fish='urn:example:fish'/>";
    Element source = XmlUtil.parseText(xml);
    SOAPFactory soapFactory = SOAPFactory.newInstance();
    SOAPElement target = soapFactory.createElement("detail");
    // perform the copy
    SoapUtil.copyAttributes(target, source);
    // qualified attributes
    assertEquals("0.1lb", target.getAttributeValue(soapFactory.createName("lettuce", null,
        "urn:example:produce")));
    assertEquals("0.25lb", target.getAttributeValue(soapFactory.createName("fillet", null,
        "urn:example:fish")));
    // local attribute
    assertEquals("1200", target.getAttributeValue(soapFactory.createName("time")));
  }

  public void testCopyNamespaces_soapDom() throws Exception {
    String xml = "<part xmlns:produce='urn:example:produce'>"
        + " <lunch produce:lettuce='0.1lb' fish:fillet='0.25lb' "
        + "  xmlns:fish='urn:example:fish' xmlns='urn:example:meal'/>"
        + "</part>";
    Element source = XmlUtil.getElement(XmlUtil.parseText(xml), "urn:example:meal", "lunch");
    SOAPFactory soapFactory = SOAPFactory.newInstance();
    SOAPElement target = soapFactory.createElement("detail");
    // perform the copy
    SoapUtil.copyNamespaces(target, source);
    // prefixed declaration
    assertEquals("urn:example:fish", target.getNamespaceURI("fish"));
    // parent prefixed declaration
    assertNull(target.getNamespaceURI("produce"));
    // default declaration (reassigned)
    assertEquals("urn:example:meal", target.getNamespaceURI(SoapUtil.DEFAULT_NAMESPACE_PREFIX));
  }

  public void testCopyVisibleNamespaces_soapDom() throws Exception {
    String xml = "<part xmlns:produce='urn:example:produce'>"
        + " <lunch produce:lettuce='0.1lb' fish:fillet='0.25lb' "
        + "  xmlns:fish='urn:example:fish' xmlns='urn:example:meal'/>"
        + "</part>";
    Element source = XmlUtil.getElement(XmlUtil.parseText(xml), "urn:example:meal", "lunch");
    SOAPFactory soapFactory = SOAPFactory.newInstance();
    SOAPElement target = soapFactory.createElement("lunch");
    // perform the copy
    SoapUtil.copyVisibleNamespaces(target, source);
    // prefixed declaration
    assertEquals("urn:example:fish", target.getNamespaceURI("fish"));
    // parent prefixed declaration
    assertEquals("urn:example:produce", target.getNamespaceURI("produce"));
    // default declaration (reassigned)
    assertEquals("urn:example:meal", target.getNamespaceURI(SoapUtil.DEFAULT_NAMESPACE_PREFIX));
  }

  public void testCopyVisibleNamespaces_soapDom_targetMatch() throws Exception {
    String xml = "<part xmlns:produce='urn:example:produce'>"
        + " <lunch produce:lettuce='0.1lb' fish:fillet='0.25lb' "
        + "  xmlns:fish='urn:example:fish' xmlns='urn:example:meal'/>"
        + "</part>";
    Element source = XmlUtil.getElement(XmlUtil.parseText(xml), "urn:example:meal", "lunch");

    String targetXml = "<soap:Envelope xmlns:soap='"
        + SOAPConstants.URI_NS_SOAP_ENVELOPE
        + "'>"
        + " <soap:Body>"
        + "  <other:Operation xmlns:produce='urn:example:produce' xmlns:meal='urn:example:meal'"
        + "   xmlns:other='urn:example:other'>"
        + "   <lunch />"
        + "  </other:Operation>"
        + " </soap:Body>"
        + "</soap:Envelope>";
    SOAPMessage soapMessage = parseSoap(targetXml);
    SOAPElement operation = SoapUtil.getElement(soapMessage.getSOAPBody(), "urn:example:other",
        "Operation");
    SOAPElement target = SoapUtil.getElement(operation, "lunch");

    // in the WS4EE stack, target contains the *visible* namespace after parsing
    target.removeNamespaceDeclaration("produce");
    target.removeNamespaceDeclaration("meal");

    // perform the copy
    SoapUtil.copyVisibleNamespaces(target, source);
    List prefixes = IteratorUtils.toList(target.getNamespacePrefixes());

    // prefixed declaration
    assertTrue(prefixes.contains("fish"));
    assertEquals("urn:example:fish", target.getNamespaceURI("fish"));
    // parent prefixed declaration
    assertFalse(prefixes.contains("produce"));
    assertEquals("urn:example:produce", target.getNamespaceURI("produce"));
    // default declaration (reassigned)
    assertFalse(prefixes.contains("meal"));
    assertEquals("urn:example:meal", target.getNamespaceURI("meal"));
  }

  public void testCopyChildNodes_soapDom() throws Exception {
    String xml = "<lunch xmlns:produce='urn:example:produce'"
        + " xmlns='urn:example:meal'>"
        + " <time>1200</time>"
        + " <produce:lettuce>0.1lb</produce:lettuce>"
        + " <fish:fillet xmlns:fish='urn:example:fish'>0.25lb</fish:fillet>"
        + " <padding xmlns=''/>"
        + "</lunch>";
    Element source = XmlUtil.parseText(xml);

    SOAPMessage soapMessage = MessageFactory.newInstance().createMessage();
    SOAPEnvelope envelope = soapMessage.getSOAPPart().getEnvelope();
    SOAPElement target = envelope.getBody().addBodyElement(envelope.createName("detail"));

    // perform the copy
    SoapUtil.copyChildNodes(target, source);

    // qualified, prefixless element
    SOAPElement time = SoapUtil.getElement(target, "urn:example:meal", "time");
    assertEquals(SoapUtil.DEFAULT_NAMESPACE_PREFIX, time.getPrefix());
    // qualified, prefixed element
    SOAPElement lettuce = SoapUtil.getElement(target, "urn:example:produce", "lettuce");
    assertEquals("produce", lettuce.getPrefix());
    // parent qualified, prefixed element
    SOAPElement fillet = SoapUtil.getElement(target, "urn:example:fish", "fillet");
    assertEquals("fish", fillet.getPrefix());
    // local element
    SOAPElement padding = SoapUtil.getElement(target, "padding");
    assertNull(padding.getPrefix());
    assertNull(padding.getNamespaceURI());
  }

  public void testCopyChildElement_soapDom() throws Exception {
    String xml = "<meal:lunch xmlns:meal='urn:example:meal'>" + " <padding />" + "</meal:lunch>";
    Element source = XmlUtil.parseText(xml);
    SOAPMessage message = MessageFactory.newInstance().createMessage();
    SOAPElement parent = message.getSOAPBody().addChildElement("lunch", "meal", "urn:example:meal");
    // perform the copy
    SoapUtil.copyChildElement(parent, XmlUtil.getElement(source, "padding"));
    SOAPElement padding = SoapUtil.getElement(parent, "padding");

    // unqualified element
    assertNull(padding.getPrefix());

    // reload
    message = writeAndRead(message);
    parent = SoapUtil.getElement(message.getSOAPBody(), "urn:example:meal", "lunch");
    padding = SoapUtil.getElement(parent, "padding");

    // unqualified element
    assertNull(padding.getPrefix());
  }

  public void testCopyAttributes_domSoap() throws Exception {
    String xml = "<soap:Envelope xmlns:soap='"
        + SOAPConstants.URI_NS_SOAP_ENVELOPE
        + "'>"
        + " <soap:Body xmlns:fish='urn:example:fish'>"
        + "  <lunch time='1200' produce:lettuce='0.1lb' fish:fillet='0.25lb' "
        + "   xmlns:produce='urn:example:produce' />"
        + " </soap:Body>"
        + "</soap:Envelope>";
    SOAPMessage soapMessage = parseSoap(xml);
    SOAPElement source = SoapUtil.getElement(soapMessage.getSOAPBody(), "lunch");
    Element target = XmlUtil.createElement("detail");
    // perform the copy
    SoapUtil.copyAttributes(target, source);
    // qualified attributes
    assertEquals("0.1lb", target.getAttributeNS("urn:example:produce", "lettuce"));
    assertEquals("0.25lb", target.getAttributeNS("urn:example:fish", "fillet"));
    // local attribute
    assertEquals("1200", target.getAttribute("time"));
  }

  public void testCopyNamespaces_domSoap() throws Exception {
    String xml = "<soap:Envelope xmlns:soap='"
        + SOAPConstants.URI_NS_SOAP_ENVELOPE
        + "'>"
        + " <soap:Body xmlns:produce='urn:example:produce'>"
        + "  <meal:lunch produce:lettuce='0.1lb' fish:fillet='0.25lb' "
        + "   xmlns:fish='urn:example:fish' xmlns:meal='urn:example:meal'/>"
        + " </soap:Body>"
        + "</soap:Envelope>";
    SOAPMessage soapMessage = parseSoap(xml);
    SOAPElement source = SoapUtil.getElement(soapMessage.getSOAPBody(), "urn:example:meal", "lunch");
    Element target = XmlUtil.createElement("detail");

    // perform the copy
    SoapUtil.copyNamespaces(target, source);

    // prefixed declaration
    assertEquals("urn:example:fish", XmlUtil.getNamespaceURI("fish", target));
    assertEquals("urn:example:meal", XmlUtil.getNamespaceURI("meal", target));
    // parent prefixed declaration
    assertNull(XmlUtil.getNamespaceURI("produce", target));
    assertNull(XmlUtil.getNamespaceURI("soap", target));
  }

  public void testCopyVisibleNamespaces_domSoap() throws Exception {
    String xml = "<soap:Envelope xmlns:soap='"
        + SOAPConstants.URI_NS_SOAP_ENVELOPE
        + "'>"
        + " <soap:Body xmlns:produce='urn:example:produce'>"
        + "  <meal:lunch produce:lettuce='0.1lb' fish:fillet='0.25lb' xmlns=''"
        + "   xmlns:fish='urn:example:fish' xmlns:meal='urn:example:meal'/>"
        + " </soap:Body>"
        + "</soap:Envelope>";
    SOAPMessage soapMessage = parseSoap(xml);
    SOAPElement source = SoapUtil.getElement(soapMessage.getSOAPBody(), "urn:example:meal", "lunch");
    Element target = XmlUtil.createElement("detail");

    // perform the copy
    SoapUtil.copyVisibleNamespaces(target, source);

    // prefixed declaration
    assertEquals("urn:example:fish", XmlUtil.getNamespaceURI("fish", target));
    assertEquals("urn:example:meal", XmlUtil.getNamespaceURI("meal", target));
    // parent prefixed declaration
    assertEquals("urn:example:produce", XmlUtil.getNamespaceURI("produce", target));
    assertEquals(SOAPConstants.URI_NS_SOAP_ENVELOPE, XmlUtil.getNamespaceURI("soap", target));
  }

  public void testCopyVisibleNamespaces_domSoap_targetMatch() throws Exception {
    String xml = "<soap:Envelope xmlns:soap='"
        + SOAPConstants.URI_NS_SOAP_ENVELOPE
        + "'>"
        + " <soap:Body xmlns:produce='urn:example:produce'>"
        + "  <meal:lunch produce:lettuce='0.1lb' fish:fillet='0.25lb' "
        + "   xmlns:fish='urn:example:fish' xmlns:meal='urn:example:meal'/>"
        + " </soap:Body>"
        + "</soap:Envelope>";
    SOAPMessage soapMessage = parseSoap(xml);
    SOAPElement source = SoapUtil.getElement(soapMessage.getSOAPBody(), "urn:example:meal", "lunch");

    String targetXml = "<detail xmlns:produce='urn:example:produce'>"
        + " <other:target xmlns:other='urn:example:other'/>"
        + "</detail>";
    Element target = XmlUtil.getElement(XmlUtil.parseText(targetXml), "urn:example:other", "target");

    // perform the copy
    SoapUtil.copyVisibleNamespaces(target, source);

    // prefixed declaration
    assertEquals("urn:example:fish", target.getAttributeNS(BpelConstants.NS_XMLNS, "fish"));
    assertEquals("urn:example:meal", target.getAttributeNS(BpelConstants.NS_XMLNS, "meal"));
    // parent prefixed declaration
    assertNull(target.getAttributeNodeNS(BpelConstants.NS_XMLNS, "produce"));
    assertEquals(SOAPConstants.URI_NS_SOAP_ENVELOPE, target.getAttributeNS(BpelConstants.NS_XMLNS,
        "soap"));
  }

  public void testCopyChildNodes_domSoap() throws Exception {
    String xml = "<soap:Envelope xmlns:soap='"
        + SOAPConstants.URI_NS_SOAP_ENVELOPE
        + "'>"
        + " <soap:Body xmlns:fish='urn:example:fish'>"
        + "  <meal:lunch xmlns:produce='urn:example:produce'"
        + "   xmlns:meal='urn:example:meal'>"
        + "   <time>1200</time>"
        + "   <produce:lettuce>0.1lb</produce:lettuce>"
        + "   <fish:fillet xmlns:fish='urn:example:fish'>0.25lb</fish:fillet>"
        + "  </meal:lunch>"
        + " </soap:Body>"
        + "</soap:Envelope>";
    SOAPMessage soapMessage = parseSoap(xml);
    SOAPElement source = SoapUtil.getElement(soapMessage.getSOAPBody(), "urn:example:meal", "lunch");
    Element target = XmlUtil.createElement("detail");
    // perform the copy
    SoapUtil.copyChildNodes(target, source);
    // local element
    Element time = XmlUtil.getElement(target, "time");
    assertNull(time.getPrefix());
    // qualified, prefixed element
    Element lettuce = XmlUtil.getElement(target, "urn:example:produce", "lettuce");
    assertEquals("produce", lettuce.getPrefix());
    // parent qualified, prefixed element
    Element fillet = XmlUtil.getElement(target, "urn:example:fish", "fillet");
    assertEquals("fish", fillet.getPrefix());
  }

  public void testCopyChildElement_domSoap() throws Exception {
    // <soap:Envelope xmlns:soap='${SOAPConstants.URI_NS_SOAP_ENVELOPE}'>
    // <soap:Body xmlns:fish='urn:example:fish'>
    // <meal:lunch xmlns:produce='urn:example:produce'
    // xmlns:meal='urn:example:meal'>
    // <padding />
    // </meal:lunch>
    // </soap:Body>
    // </soap:Envelope>
    SOAPMessage soapMessage = MessageFactory.newInstance().createMessage();
    SOAPEnvelope envelope = soapMessage.getSOAPPart().getEnvelope();

    SOAPBody body = envelope.getBody();
    body.addNamespaceDeclaration("fish", "urn:example:fish");

    Name lunchName = envelope.createName("lunch", "meal", "urn:example:meal");
    SOAPElement lunch = body.addBodyElement(lunchName);
    lunch.addNamespaceDeclaration("produce", "urn:example:produce");
    lunch.addNamespaceDeclaration("meal", "urn:example:meal");

    SOAPElement source = SoapUtil.addChildElement(lunch, "padding");
    Element parent = XmlUtil.createElement("urn:example:meal", "lunch");

    // perform the copy
    SoapUtil.copyChildElement(parent, source);
    Element padding = XmlUtil.getElement(parent, "padding");

    // unqualified element
    assertNull(padding.getPrefix());

    // reload
    // parent = writeAndRead(parent);
    padding = XmlUtil.getElement(parent, "padding");

    // unqualified element
    assertNull(padding.getPrefix());
  }

  public void testGetPrefix_soap() throws Exception {
    String xml = "<soap:Envelope xmlns:soap='"
        + SOAPConstants.URI_NS_SOAP_ENVELOPE
        + "'>"
        + " <soap:Body xmlns:produce='urn:example:produce'>"
        + "  <meal:lunch produce:lettuce='0.1lb' fish:fillet='0.25lb' xmlns=''"
        + "   xmlns:fish='urn:example:fish' xmlns:meal='urn:example:meal'/>"
        + " </soap:Body>"
        + "</soap:Envelope>";
    SOAPMessage soapMessage = parseSoap(xml);
    SOAPElement elem = SoapUtil.getElement(soapMessage.getSOAPBody(), "urn:example:meal", "lunch");

    // prefixed declaration
    assertEquals("fish", SoapUtil.getPrefix("urn:example:fish", elem));
    assertEquals("meal", SoapUtil.getPrefix("urn:example:meal", elem));
    // parent prefixed declaration
    assertEquals("produce", SoapUtil.getPrefix("urn:example:produce", elem));
    assertEquals("soap", SoapUtil.getPrefix(SOAPConstants.URI_NS_SOAP_ENVELOPE, elem));
  }

  private static SOAPMessage parseSoap(String xmlString) throws IOException, SOAPException {
    ByteArrayInputStream sourceStream = new ByteArrayInputStream(xmlString.getBytes());
    return MessageFactory.newInstance().createMessage(null, sourceStream);
  }

  private static SOAPMessage writeAndRead(SOAPMessage soapMessage) throws SOAPException,
      IOException {
    // write to memory sink
    ByteArrayOutputStream soapSink = new ByteArrayOutputStream();
    soapMessage.writeTo(soapSink);
    soapSink.writeTo(System.out);
    System.out.println();
    // read from memory source
    return MessageFactory.newInstance().createMessage(null,
        new ByteArrayInputStream(soapSink.toByteArray()));
  }

  // BPEL-124: child elements in the default namespace are incorrectly losing
  // their parent namespaces
  public void testCopy_soapDom_qualifiedNoPrefix() throws Exception {
    String xml = "<ReverseAndConcatNames xmlns='http://my.namespace'>"
        + " <firstName>Martin</firstName>"
        + " <secondName>Steinle</secondName>"
        + "</ReverseAndConcatNames>";
    Element source = XmlUtil.parseText(xml);

    SOAPMessage soapMessage = MessageFactory.newInstance().createMessage();
    SOAPEnvelope envelope = soapMessage.getSOAPPart().getEnvelope();
    SOAPElement target = envelope.getBody().addBodyElement(envelope.createName("detail"));

    // perform the copy
    SoapUtil.copy(target, source);

    assertEquals("http://my.namespace", target.getNamespaceURI(SoapUtil.DEFAULT_NAMESPACE_PREFIX));
    // qualified elements
    SOAPElement firstName = SoapUtil.getElement(target, "http://my.namespace", "firstName");
    assertEquals("Martin", firstName.getValue());
    SOAPElement secondName = SoapUtil.getElement(target, "http://my.namespace", "secondName");
    assertEquals("Steinle", secondName.getValue());
  }

  public void testCopy_soapDom_noOverride() throws Exception {
    String xml = "<part xmlns:produce='urn:example:produce'>"
        + " <lunch produce:lettuce='0.1lb' fish:fillet='0.25lb'"
        + "  xmlns:fish='urn:example:fish' xmlns='urn:example:meal'/>"
        + "</part>";
    Element source = XmlUtil.parseText(xml);

    /*
     * here, notice the 'urn:example:meal' namespace (the default namespace in the source) is mapped
     * to prefix 'fish' which the source maps to namespace 'urn:example:fish'
     */
    // <soap:Envelope xmlns:soap='${SOAPConstants.URI_NS_SOAP_ENVELOPE}'>
    // <soap:Body>"
    // <fish:Operation xmlns:fish='urn:example:meal'>
    // <part />
    // </fish:Operation>
    // </soap:Body>"
    // </soap:Envelope>
    SOAPMessage soapMessage = MessageFactory.newInstance().createMessage();
    SOAPElement operation = soapMessage.getSOAPBody().addChildElement("Operation", "fish",
        "urn:example:meal");
    SOAPElement part = SoapUtil.addChildElement(operation, "part");

    // perform the copy
    SoapUtil.copy(part, source);

    soapMessage = writeAndRead(soapMessage);
    // retrieve relevant elements
    operation = SoapUtil.getElement(soapMessage.getSOAPBody(), "urn:example:meal", "Operation");
    part = SoapUtil.getElement(operation, "part");
    SOAPElement lunch = SoapUtil.getElement(part, "urn:example:meal", "lunch");

    // prefixed declaration
    assertEquals("urn:example:fish", lunch.getNamespaceURI("fish"));
    // parent prefixed declaration
    assertEquals("urn:example:produce", lunch.getNamespaceURI("produce"));
    // default declaration (reassigned)
    assertEquals("urn:example:meal", lunch.getNamespaceURI(SoapUtil.DEFAULT_NAMESPACE_PREFIX));
  }

  public void testCopy_domSoap_qualifiedNoPrefix() throws Exception {
    String xml = "<soapenv:Envelope xmlns:soapenv='"
        + SOAPConstants.URI_NS_SOAP_ENVELOPE
        + "'>"
        + " <soapenv:Body>"
        + "  <response xmlns='"
        + BpelConstants.NS_EXAMPLES
        + "'>"
        + "   <return>"
        + "    <amount>0.0</amount>"
        + "    <branch>140</branch>"
        + "    <capital>10419.18</capital>"
        + "    <status>1</status>"
        + "    <transaction xmlns:xsi='"
        + BpelConstants.NS_XML_SCHEMA_INSTANCE
        + "' xsi:nil='true'/>"
        + "   </return>"
        + "  </response>"
        + " </soapenv:Body>"
        + "</soapenv:Envelope>";
    SOAPMessage soapMessage = parseSoap(xml);
    SOAPElement source = SoapUtil.getElement(soapMessage.getSOAPBody(), BpelConstants.NS_EXAMPLES,
        "response");
    Element target = XmlUtil.createElement("detail");

    // perform the copy
    SoapUtil.copy(target, source);
    target = XmlUtilTest.writeAndRead(target);

    // namespaces
    assertEquals(SOAPConstants.URI_NS_SOAP_ENVELOPE, target.getAttributeNS(BpelConstants.NS_XMLNS,
        "soapenv"));

    // child elements
    Element returnElem = XmlUtil.getElement(target, BpelConstants.NS_EXAMPLES, "return");

    // namespaces
    assertEquals(BpelConstants.NS_EXAMPLES, returnElem.getAttributeNS(BpelConstants.NS_XMLNS,
        "xmlns"));

    // child elements
    assertEquals("0.0", DatatypeUtil.toString(XmlUtil.getElement(returnElem,
        BpelConstants.NS_EXAMPLES, "amount")));
    assertEquals("140", DatatypeUtil.toString(XmlUtil.getElement(returnElem,
        BpelConstants.NS_EXAMPLES, "branch")));
    assertEquals("10419.18", DatatypeUtil.toString(XmlUtil.getElement(returnElem,
        BpelConstants.NS_EXAMPLES, "capital")));
    assertEquals("1", DatatypeUtil.toString(XmlUtil.getElement(returnElem,
        BpelConstants.NS_EXAMPLES, "status")));

    Element transactionElem = XmlUtil.getElement(returnElem, BpelConstants.NS_EXAMPLES,
        "transaction");

    // namespaces
    assertEquals(BpelConstants.NS_XML_SCHEMA_INSTANCE, transactionElem.getAttributeNS(
        BpelConstants.NS_XMLNS, "xsi"));

    // attributes
    assertEquals("true",
        transactionElem.getAttributeNS(BpelConstants.NS_XML_SCHEMA_INSTANCE, "nil"));
  }
}
