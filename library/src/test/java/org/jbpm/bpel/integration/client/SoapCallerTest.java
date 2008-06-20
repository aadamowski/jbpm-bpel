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
package org.jbpm.bpel.integration.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.WSDLException;
import javax.xml.namespace.QName;

import junit.framework.Test;
import junit.framework.TestCase;

import org.w3c.dom.Element;

import org.jbpm.bpel.graph.exe.BpelFaultException;
import org.jbpm.bpel.graph.exe.FaultInstance;
import org.jbpm.bpel.tools.ModuleDeployTestSetup;
import org.jbpm.bpel.variable.exe.MessageValue;
import org.jbpm.bpel.wsdl.xml.WsdlUtil;
import org.jbpm.bpel.xml.util.DatatypeUtil;
import org.jbpm.bpel.xml.util.XmlUtil;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2008/01/30 07:19:06 $
 */
public class SoapCallerTest extends TestCase {

  static final String NS_TRANSLATOR = "http://example.com/translator";
  static final String NS_TYPES = "http://example.com/translator/types";
  static final QName TRANSLATOR_SERVICE = new QName(NS_TRANSLATOR, "translatorService");
  static final String TEXT_WSDL_LOCATION = "http://localhost:8080/translator/text?wsdl";
  static final String DOC_WSDL_LOCATION = "http://localhost:8080/translator/document?wsdl";

  static final Random random = new Random();

  public void testCall_document_requestResponse() throws Exception {
    Caller caller = createCaller(DOC_WSDL_LOCATION, TRANSLATOR_SERVICE, "documentTranslatorPort");
    try {
      String requestText = "<sns:translationRequest targetLanguage='es' xmlns:sns='"
          + NS_TYPES
          + "'>"
          + " <sns:document>"
          + "  <head title='letter' language='en'/>"
          + "  <body>"
          + "   <paragraph>hi</paragraph>"
          + "   <paragraph>bye</paragraph>"
          + "  </body>"
          + " </sns:document>"
          + "</sns:translationRequest>";
      Map inputParts = Collections.singletonMap("translationRequest",
          XmlUtil.parseText(requestText));
      Map outputParts = caller.call("translate", inputParts);

      Element documentPart = (Element) outputParts.get("document");

      Element headElem = XmlUtil.getElement(documentPart, "head");
      assertEquals("carta", headElem.getAttribute("title"));
      assertEquals("es", headElem.getAttribute("language"));

      Element bodyElem = XmlUtil.getElement(documentPart, "body");
      Iterator paragraphElems = XmlUtil.getElements(bodyElem, null, "paragraph");
      assertEquals("hola", DatatypeUtil.toString((Element) paragraphElems.next()));
      assertEquals("adi\u00f3s", DatatypeUtil.toString((Element) paragraphElems.next()));
      assertFalse(paragraphElems.hasNext());
    }
    finally {
      caller.close();
    }
  }

  public void testCall_document_requestFault() throws Exception {
    Caller caller = createCaller(DOC_WSDL_LOCATION, TRANSLATOR_SERVICE, "documentTranslatorPort");
    try {
      String requestText = "<sns:translationRequest targetLanguage='es' xmlns:sns='"
          + NS_TYPES
          + "'>"
          + " <sns:document>"
          + "  <head title='letter' language='en'/>"
          + "  <body>"
          + "   <paragraph>hi</paragraph>"
          + "   <paragraph>wawa</paragraph>"
          + "  </body>"
          + " </sns:document>"
          + "</sns:translationRequest>";
      Map inputParts = Collections.singletonMap("translationRequest",
          XmlUtil.parseText(requestText));
      caller.call("translate", inputParts);

      fail("call should have thrown a fault");
    }
    catch (BpelFaultException e) {
      FaultInstance faultInstance = e.getFaultInstance();
      assertEquals(new QName(NS_TRANSLATOR, "textNotTranslatable"), faultInstance.getName());

      MessageValue message = faultInstance.getMessageValue();
      assertEquals(new QName(NS_TRANSLATOR, "textNotTranslatableFault"), message.getType()
          .getName());

      Element detailPart = message.getPart("detail");
      assertEquals(NS_TYPES, detailPart.getNamespaceURI());
      assertEquals("textNotTranslatable", detailPart.getLocalName());

      Element textElem = XmlUtil.getElement(detailPart, "text");
      assertEquals("wawa", DatatypeUtil.toString(textElem));
    }
    finally {
      caller.close();
    }
  }

  public void testCall_document_oneWay() throws Exception {
    Caller caller = createCaller(DOC_WSDL_LOCATION, TRANSLATOR_SERVICE, "documentTranslatorPort");
    try {
      String clientName = generateClientName();
      String requestText = "<sns:quotationRequest clientName='"
          + clientName
          + "' targetLanguage='es' "
          + " xmlns:sns='"
          + NS_TYPES
          + "'>"
          + " <sns:document>"
          + "  <head title='letter' language='en'/>"
          + "  <body>"
          + "   <paragraph>hi</paragraph>"
          + "   <paragraph>bye</paragraph>"
          + "  </body>"
          + " </sns:document>"
          + "</sns:quotationRequest>";
      Map inputParts = Collections.singletonMap("quotationRequest", XmlUtil.parseText(requestText));
      caller.callOneWay("quoteTranslation", inputParts);

      requestText = "<sns:statusRequest clientName='"
          + clientName
          + "' xmlns:sns='"
          + NS_TYPES
          + "' />";
      inputParts = Collections.singletonMap("statusRequest", XmlUtil.parseText(requestText));
      // quote is a one-way operation, so the status change might not be reflected immediately
      Thread.sleep(500);
      Map outputParts = caller.call("getQuotationStatus", inputParts);

      Element statusPart = (Element) outputParts.get("statusResponse");
      assertEquals("received", statusPart.getAttribute("status"));
    }
    finally {
      caller.close();
    }
  }

  public void testCall_rpc_requestResponse() throws Exception {
    Caller caller = createCaller(TEXT_WSDL_LOCATION, TRANSLATOR_SERVICE, "textTranslatorPort");
    try {
      Map inputParts = new HashMap();
      addRpcPart(inputParts, "text", "hi");
      addRpcPart(inputParts, "sourceLanguage", "en");
      addRpcPart(inputParts, "targetLanguage", "es");
      Map outputParts = caller.call("translate", inputParts);

      Element textPart = (Element) outputParts.get("translatedText");
      assertEquals("hola", DatatypeUtil.toString(textPart));
    }
    finally {
      caller.close();
    }
  }

  private static void addRpcPart(Map parts, String partName, String value) {
    Element part = XmlUtil.createElement(partName);
    XmlUtil.setStringValue(part, value);
    parts.put(partName, part);
  }

  public void testCall_rpc_requestFault() throws Exception {
    Caller caller = createCaller(TEXT_WSDL_LOCATION, TRANSLATOR_SERVICE, "textTranslatorPort");
    try {
      Map inputParts = new HashMap();
      addRpcPart(inputParts, "text", "hi");
      addRpcPart(inputParts, "sourceLanguage", "en");
      addRpcPart(inputParts, "targetLanguage", "ja");
      caller.call("translate", inputParts);

      fail("call should have thrown a fault");
    }
    catch (BpelFaultException e) {
      // check returned fault
      FaultInstance faultInstance = e.getFaultInstance();
      // name
      assertEquals(new QName(NS_TRANSLATOR, "dictionaryNotAvailable"), faultInstance.getName());
      // data type
      MessageValue message = faultInstance.getMessageValue();
      assertEquals(new QName(NS_TRANSLATOR, "dictionaryNotAvailableFault"), message.getType()
          .getName());
      // data content
      Element detailPart = message.getPart("detail");
      assertEquals("http://example.com/translator/types", detailPart.getNamespaceURI());
      assertEquals("dictionaryNotAvailable", detailPart.getLocalName());
    }
    finally {
      caller.close();
    }
  }

  public void testCall_rpc_oneWay() throws Exception {
    Caller caller = createCaller(TEXT_WSDL_LOCATION, TRANSLATOR_SERVICE, "textTranslatorPort");
    try {
      String clientName = generateClientName();
      Map inputParts = new HashMap();
      addRpcPart(inputParts, "clientName", clientName);
      addRpcPart(inputParts, "text", "hi");
      addRpcPart(inputParts, "sourceLanguage", "en");
      addRpcPart(inputParts, "targetLanguage", "ja");
      caller.callOneWay("quoteTranslation", inputParts);

      Element clientNameElem = XmlUtil.createElement("clientName");
      XmlUtil.setStringValue(clientNameElem, clientName);
      inputParts = Collections.singletonMap("clientName", clientNameElem);
      // quote is a one-way operation, so the status change might not be reflected immediately
      Thread.sleep(500);
      Map outputParts = caller.call("getQuotationStatus", inputParts);

      Element statusPart = (Element) outputParts.get("status");
      assertEquals("received", DatatypeUtil.toString(statusPart));
    }
    finally {
      caller.close();
    }
  }

  private static Caller createCaller(String wsdlLocation, QName serviceName, String portName)
      throws WSDLException {
    // read wsdl
    Definition def = WsdlUtil.getFactory().newWSDLReader().readWSDL(wsdlLocation);
    Service service = def.getService(serviceName);
    Port port = service.getPort(portName);

    // configure caller
    return new SoapCaller(port);
  }

  private static String generateClientName() {
    return "client" + random.nextInt(100000);
  }

  public static Test suite() {
    return new ModuleDeployTestSetup(SoapCallerTest.class, SoapCallerTest.class.getResource(
        "translator.war").toExternalForm());
  }
}
