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

import java.io.StringWriter;

import javax.xml.namespace.QName;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import junit.framework.TestCase;

import org.w3c.dom.Element;

import org.jbpm.bpel.endpointref.wsa.WsaConstants;
import org.jbpm.bpel.endpointref.wsa.WsaEndpointReference;
import org.jbpm.bpel.xml.BpelConstants;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/09/04 06:42:26 $
 */
public class XmlUtilTest extends TestCase {

  public void testRemoveAttributes_dom() throws Exception {
    String xml = "<lunch time='1200' produce:lettuce='0.1lb' fish:fillet='0.25lb' "
        + " xmlns:produce='urn:example:produce' xmlns:fish='urn:example:fish'/>";
    Element element = XmlUtil.parseText(xml);
    // remove the attributes
    XmlUtil.removeAttributes(element);
    // verify remotion
    assertFalse(element.hasAttributes());
  }

  public void testRemoveChildNodes_dom() throws Exception {
    String xml = "<lunch xmlns:produce='urn:example:produce' xmlns='urn:example:meal'>"
        + " <time>1200</time>"
        + " <produce:lettuce>0.1lb</produce:lettuce>"
        + " <fish:fillet xmlns:fish='urn:example:fish'>0.25lb</fish:fillet>"
        + " <padding xmlns=''/>"
        + "</lunch>";
    Element element = XmlUtil.parseText(xml);
    // remove the child nodes
    XmlUtil.removeChildNodes(element);
    // verify remotion
    assertFalse(element.hasChildNodes());
  }

  public void testCopyAttributes_domDom() throws Exception {
    String xml = "<lunch time='1200' produce:lettuce='0.1lb' fish:fillet='0.25lb' "
        + " xmlns:produce='urn:example:produce' xmlns:fish='urn:example:fish'/>";
    Element source = XmlUtil.parseText(xml);
    Element target = XmlUtil.createElement("detail");
    // perform the copy
    XmlUtil.copyAttributes(target, source);
    // qualified attributes
    assertEquals("0.1lb", target.getAttributeNS("urn:example:produce", "lettuce"));
    assertEquals("0.25lb", target.getAttributeNS("urn:example:fish", "fillet"));
    // local attribute
    assertEquals("1200", target.getAttribute("time"));
  }

  public void testCopyNamespaces_domDom() throws Exception {
    String xml = "<part xmlns:produce='urn:example:produce'>"
        + " <lunch produce:lettuce='0.1lb' fish:fillet='0.25lb' "
        + "  xmlns:fish='urn:example:fish' xmlns='urn:example:meal'/>"
        + "</part>";
    Element source = XmlUtil.getElement(XmlUtil.parseText(xml), "urn:example:meal", "lunch");
    Element target = XmlUtil.createElement("detail");

    // perform the copy
    XmlUtil.copyNamespaces(target, source);

    // prefixed declaration
    assertEquals("urn:example:fish", XmlUtil.getNamespaceURI("fish", target));
    // parent prefixed declaration
    assertNull(XmlUtil.getNamespaceURI("produce", target));
    // default declaration
    assertEquals("urn:example:meal", XmlUtil.getNamespaceURI(null, target));
  }

  public void testCopyVisibleNamespaces_domDom() throws Exception {
    String xml = "<part xmlns:produce='urn:example:produce'>"
        + " <lunch produce:lettuce='0.1lb' fish:fillet='0.25lb' "
        + "  xmlns:fish='urn:example:fish' xmlns='urn:example:meal'/>"
        + "</part>";
    Element source = XmlUtil.getElement(XmlUtil.parseText(xml), "urn:example:meal", "lunch");
    Element target = XmlUtil.createElement("detail");

    // perform the copy
    XmlUtil.copyVisibleNamespaces(target, source);

    // prefixed declaration
    assertEquals("urn:example:fish", XmlUtil.getNamespaceURI("fish", target));
    // parent prefixed declaration
    assertEquals("urn:example:produce", XmlUtil.getNamespaceURI("produce", target));
    // default declaration
    assertEquals("urn:example:meal", XmlUtil.getNamespaceURI(null, target));
  }

  public void testCopyVisibleNamespaces_domDom_targetMatch() throws Exception {
    String xml = "<part xmlns:produce='urn:example:produce'>"
        + " <lunch produce:lettuce='0.1lb' fish:fillet='0.25lb' "
        + "  xmlns:fish='urn:example:fish' xmlns='urn:example:meal'/>"
        + "</part>";
    Element source = XmlUtil.getElement(XmlUtil.parseText(xml), "urn:example:meal", "lunch");

    String targetXml = "<detail xmlns:produce='urn:example:produce' xmlns='urn:example:meal'>"
        + " <other:target xmlns:other='urn:example:other'/>"
        + "</detail>";
    Element target = XmlUtil.getElement(XmlUtil.parseText(targetXml), "urn:example:other", "target");

    // perform the copy
    XmlUtil.copyVisibleNamespaces(target, source);

    // local prefixed declaration
    assertEquals("urn:example:fish", target.getAttributeNS(BpelConstants.NS_XMLNS, "fish"));
    // parent prefixed declaration - redundant
    assertNull(target.getAttributeNodeNS(BpelConstants.NS_XMLNS, "produce"));
    // default declaration - redundant
    assertNull(target.getAttributeNode("xmlns"));
  }

  public void testCopyChildNodes_domDom() throws Exception {
    String xml = "<lunch xmlns:produce='urn:example:produce'"
        + " xmlns='urn:example:meal'>"
        + " <time>1200</time>"
        + " <produce:lettuce>0.1lb</produce:lettuce>"
        + " <fish:fillet xmlns:fish='urn:example:fish'>0.25lb</fish:fillet>"
        + " <padding xmlns=''/>"
        + "</lunch>";
    Element source = XmlUtil.parseText(xml);
    Element target = XmlUtil.createElement("detail");
    // perform the copy
    XmlUtil.copyChildNodes(target, source);
    // qualified, prefixless element
    Element time = XmlUtil.getElement(target, "urn:example:meal", "time");
    assertNull(time.getPrefix());
    // qualified, prefixed element
    Element lettuce = XmlUtil.getElement(target, "urn:example:produce", "lettuce");
    assertEquals("produce", lettuce.getPrefix());
    // parent qualified, prefixed element
    Element fillet = XmlUtil.getElement(target, "urn:example:fish", "fillet");
    assertEquals("fish", fillet.getPrefix());
    // unqualified element
    Element padding = XmlUtil.getElement(target, "padding");
    assertNull(padding.getPrefix());
  }

  public void testCopyChildElement_domDom() throws Exception {
    String xml = "<meal:lunch xmlns:meal='urn:example:meal'>" + " <padding />" + "</meal:lunch>";
    Element source = XmlUtil.parseText(xml);
    Element parent = XmlUtil.createElement("urn:example:meal", "lunch");
    // perform the copy
    XmlUtil.copyChildElement(parent, XmlUtil.getElement(source, "padding"));
    Element padding = XmlUtil.getElement(parent, "padding");

    // unqualified element
    assertNull(padding.getPrefix());

    // reload
    parent = writeAndRead(parent);
    padding = XmlUtil.getElement(parent, "padding");

    // unqualified element
    assertNull(padding.getPrefix());
  }

  public void testSetObjectValue_boolean() {
    Element elem = XmlUtil.createElement("elem");
    XmlUtil.setObjectValue(elem, Boolean.TRUE);
    assertEquals("true", elem.getFirstChild().getNodeValue());
  }

  public void testSetObjectValue_string() {
    Element elem = XmlUtil.createElement("elem");
    XmlUtil.setObjectValue(elem, "popcorn");
    assertEquals("popcorn", elem.getFirstChild().getNodeValue());
  }

  public void testSetObjectValue_number() {
    Element elem = XmlUtil.createElement("elem");
    XmlUtil.setObjectValue(elem, new Double(Math.PI));
    assertEquals(Double.toString(Math.PI), elem.getFirstChild().getNodeValue());
  }

  public void testSetObjectValue_endpointref() throws Exception {
    final String address = "http://example.org/xml/util";
    final QName portType = new QName(BpelConstants.NS_EXAMPLES, "pt");

    WsaEndpointReference endpointReference = new WsaEndpointReference();
    endpointReference.setAddress(address);
    endpointReference.setPortTypeName(portType);

    Element elem = XmlUtil.createElement("elem");
    XmlUtil.setObjectValue(elem, endpointReference);
    elem = writeAndRead(elem);

    assertEquals("elem", elem.getLocalName());
    assertNull(elem.getNamespaceURI());

    Element addressElem = XmlUtil.getElement(elem, WsaConstants.NS_ADDRESSING,
        WsaConstants.ELEM_ADDRESS);
    assertEquals(address, DatatypeUtil.toString(addressElem));

    Element portTypeElem = XmlUtil.getElement(elem, WsaConstants.NS_ADDRESSING,
        WsaConstants.ELEM_PORT_TYPE);
    assertEquals(portType, XmlUtil.getQNameValue(portTypeElem));
  }

  public void testSetQNameValue() {
    Element elem = XmlUtil.createElement("elem");
    QName value = new QName(BpelConstants.NS_EXAMPLES, "local");
    XmlUtil.setQNameValue(elem, value);

    String prefixedValue = DatatypeUtil.toString(elem);
    int colonIndex = prefixedValue.indexOf(':');
    // local name
    assertEquals("local", prefixedValue.substring(colonIndex + 1));
    // namespace
    String prefix = prefixedValue.substring(0, colonIndex);
    assertEquals(BpelConstants.NS_EXAMPLES, XmlUtil.getNamespaceURI(prefix, elem));
  }

  public void testGetPrefix_dom() throws Exception {
    String xml = "<part xmlns:produce='urn:example:produce'>"
        + " <lunch produce:lettuce='0.1lb' fish:fillet='0.25lb' "
        + "  xmlns:fish='urn:example:fish' xmlns='urn:example:meal'/>"
        + "</part>";
    Element elem = XmlUtil.getElement(XmlUtil.parseText(xml), "urn:example:meal", "lunch");
    // prefixed declaration
    assertEquals("fish", XmlUtil.getPrefix("urn:example:fish", elem));
    // parent prefixed declaration
    assertEquals("produce", XmlUtil.getPrefix("urn:example:produce", elem));
    // default declaration
    assertEquals("", XmlUtil.getPrefix("urn:example:meal", elem));
    // bogus declaration
    assertNull(XmlUtil.getPrefix("urn:example:bogus", elem));
  }

  public static Element writeAndRead(Element elem) throws Exception {
    // write to memory sink
    StringWriter xmlSink = new StringWriter();
    Transformer tr = XmlUtil.getTransformerFactory().newTransformer();
    tr.transform(new DOMSource(elem), new StreamResult(xmlSink));
    String xml = xmlSink.toString();
    System.out.println(xml);
    // read from memory source
    return XmlUtil.parseText(xml);
  }
}