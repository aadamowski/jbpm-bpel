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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.wsdl.Binding;
import javax.wsdl.BindingFault;
import javax.wsdl.BindingOperation;
import javax.wsdl.Fault;
import javax.wsdl.Message;
import javax.wsdl.Operation;
import javax.wsdl.Part;
import javax.wsdl.extensions.soap.SOAPBinding;
import javax.wsdl.extensions.soap.SOAPBody;
import javax.wsdl.extensions.soap.SOAPFault;
import javax.wsdl.extensions.soap.SOAPOperation;
import javax.xml.namespace.QName;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.w3c.dom.Element;

import com.ibm.wsdl.extensions.soap.SOAPConstants;

import org.jbpm.bpel.wsdl.xml.WsdlUtil;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.bpel.xml.util.DatatypeUtil;
import org.jbpm.bpel.xml.util.XmlUtil;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2008/01/30 07:18:22 $
 */
public class SoapFormatter {

  private final Binding binding;
  private final FaultFormat faultFormat;

  public SoapFormatter(final Binding binding) {
    this(binding, FaultFormat.DEFAULT);
  }

  public SoapFormatter(final Binding binding, final FaultFormat faultFormat) {
    SOAPBinding soapBinding = (SOAPBinding) WsdlUtil.getExtension(
        binding.getExtensibilityElements(), SOAPConstants.Q_ELEM_SOAP_BINDING);
    // exclude non-soap bindings
    if (soapBinding == null)
      throw new IllegalArgumentException("non-soap binding not supported: " + binding);
    // exclude non-http transport protocols
    String transport = soapBinding.getTransportURI();
    if (!SoapBindConstants.HTTP_TRANSPORT_URI.equals(transport))
      throw new IllegalArgumentException("non-http transport not supported: " + transport);
    // exclude null fault formats
    if (faultFormat == null)
      throw new IllegalArgumentException("fault format must not be null");

    this.binding = binding;
    this.faultFormat = faultFormat;
  }

  public Binding getBinding() {
    return binding;
  }

  public FaultFormat getFaultFormat() {
    return faultFormat;
  }

  public void writeMessage(String operation, SOAPMessage soapMessage, Map parts,
      MessageDirection direction) throws SOAPException {
    // obtain operation binding details
    BindingOperation bindOperation = binding.getBindingOperation(operation, null, null);
    SOAPOperation soapOperation = (SOAPOperation) WsdlUtil.getExtension(
        bindOperation.getExtensibilityElements(), SOAPConstants.Q_ELEM_SOAP_OPERATION);

    // set the value of the SOAPAction HTTP header
    String action = soapOperation.getSoapActionURI();
    if (action != null) {
      /*
       * BP 1.2 R1109: The value of the SOAPAction HTTP header field in a HTTP request MESSAGE MUST
       * be a quoted string
       */
      action = '"' + action + '"';
    }
    else {
      // BP 1.2 section 3.6.3: A WSDL Description that has:
      // <soapbind:operation />
      // results in a SOAPAction HTTP header field as follows:
      // SOAPAction: ""
      action = "\"\"";
    }
    soapMessage.getSOAPPart().setMimeHeader(SoapBindConstants.SOAP_ACTION_HEADER, action);

    // determine whether operation is rpc-oriented or document-oriented
    String style = determineOperationStyle(soapOperation);

    // write env:Body
    if (SoapBindConstants.RPC_STYLE.equals(style))
      writeRpcBody(bindOperation, direction, soapMessage.getSOAPBody(), parts);
    else
      writeDocumentBody(bindOperation, direction, soapMessage.getSOAPBody(), parts);
  }

  private String determineOperationStyle(SOAPOperation soapOperation) {
    /*
     * BP 1.2 section 4.4: A "document-literal operation" is a wsdl:operation child element of
     * wsdl:binding whose soapbind:body descendent elements specifies the use attribute with the
     * value "literal" and, either:
     * 
     * 1. The style attribute with the value "document" is specified on the child soapbind:operation
     * element;
     */
    String style = soapOperation.getStyle();
    if (style == null) {
      /*
       * or 2. The style attribute is not present on the child soapbind:operation element, and the
       * soapbind:binding element in the enclosing wsdl:binding specifies the style attribute with
       * the value "document";
       */
      SOAPBinding soapBinding = (SOAPBinding) WsdlUtil.getExtension(
          getBinding().getExtensibilityElements(), SOAPConstants.Q_ELEM_SOAP_BINDING);
      style = soapBinding.getStyle();
      if (style == null) {
        /*
         * or 3. The style attribute is not present on both the child soapbind:operation element and
         * the soapbind:binding element in the enclosing wsdl:binding.
         */
        style = SoapBindConstants.DOCUMENT_STYLE;
      }
    }
    return style;
  }

  protected void writeRpcBody(BindingOperation bindOperation, MessageDirection direction,
      javax.xml.soap.SOAPBody body, Map parts) throws SOAPException {
    // obtain soapbind:body element
    SOAPBody soapBindBody = direction.getBodyDescription(bindOperation);
    /*
     * BP 1.2 R2706: A wsdl:binding MUST use the value of "literal" for the use attribute in all
     * soapbind:body elements
     */
    if (!SoapBindConstants.LITERAL_USE.equals(soapBindBody.getUse())) {
      throw new SOAPException("binding must use value 'literal' for attribute "
          + "'use' in all soapbind:body elements: "
          + binding);
    }
    /*
     * BP 1.2 R2717: An rpc-literal binding MUST have the namespace attribute specified, the value
     * of which MUST be an absolute URI, on contained soapbind:body elements
     */
    String operationNamespace = soapBindBody.getNamespaceURI();
    if (operationNamespace == null) {
      throw new SOAPException("rpc binding must have the namespace attribute "
          + "specified on contained soapbind:body elements: "
          + binding);
    }
    try {
      if (!new URI(operationNamespace).isAbsolute()) {
        throw new SOAPException("rpc binding must have the namespace attribute specified, "
            + "the value of which must be an absolute URI"
            + binding);
      }
    }
    catch (URISyntaxException e) {
      throw new SOAPException("rpc binding must have the namespace attribute specified, "
          + "the value of which must be an absolute URI"
          + binding, e);
    }
    // create operation element
    Operation operation = bindOperation.getOperation();
    SOAPEnvelope envelope = (SOAPEnvelope) body.getParentElement();
    Name operationQName = envelope.createName(direction.getRpcWrapperLocalName(operation),
        "operationNS", operationNamespace);
    SOAPElement operationElem = body.addBodyElement(operationQName);

    List partNames = direction.getRpcBodyPartNames(soapBindBody, operation);
    List wsdlParts = getWsdlParts(partNames, direction.getMessageDefinition(operation));

    // fill in part values
    for (int i = 0, n = parts.size(); i < n; i++) {
      Part wsdlPart = (Part) wsdlParts.get(i);
      /*
       * BP 1.2 R2203: An rpc-literal binding MUST refer, in its soapbind:body element(s), only to
       * wsdl:part element(s) that have been defined using the type attribute
       */
      if (wsdlPart.getTypeName() == null) {
        throw new SOAPException("rpc binding must refer, in its soapbind:body "
            + "elements, only to wsdl:part elements defined using the type attribute: "
            + binding);
      }
      // copy part to accessor inside operation wrapper
      Element part = (Element) parts.get(wsdlPart.getName());
      SoapUtil.copyChildElement(operationElem, part);
    }
  }

  protected void writeDocumentBody(BindingOperation bindOperation, MessageDirection direction,
      javax.xml.soap.SOAPBody body, Map parts) throws SOAPException {
    // obtain soapbind:body element
    SOAPBody soapBindBody = direction.getBodyDescription(bindOperation);
    /*
     * BP 1.2 R2706: A wsdl:binding MUST use the value of "literal" for the use attribute in all
     * soapbind:body elements
     */
    if (!SoapBindConstants.LITERAL_USE.equals(soapBindBody.getUse())) {
      throw new SOAPException("binding must use value 'literal' for the use "
          + "attribute in all soapbind:body elements: "
          + binding);
    }
    /*
     * BP 1.2 R2716: A document-literal binding MUST NOT have the namespace attribute specified on
     * contained soapbind:body elements
     */
    if (soapBindBody.getNamespaceURI() != null) {
      throw new SOAPException("document binding must not have the namespace "
          + "attribute specified on contained soapbind:body elements: "
          + binding);
    }

    List partNames = soapBindBody.getParts();
    Message message = direction.getMessageDefinition(bindOperation.getOperation());

    if (partNames == null) {
      /*
       * BP 1.2 R2210: If a document-literal binding does not specify the parts attribute on a
       * soapbind:body element, the corresponding abstract wsdl:message MUST define zero or one
       * wsdl:parts
       */
      if (message.getParts().size() > 1) {
        throw new SOAPException("if a document binding does not specify "
            + "attribute 'parts' on a soapbind:body element, the corresponding "
            + "wsdl:message must define zero or one wsdl:parts: "
            + binding);
      }
    }
    else if (partNames.size() > 1) {
      /*
       * BP 1.2 R2201: A document-literal binding MUST, in each of its soapbind:body element(s),
       * have at most one part listed in the parts attribute, if the parts attribute is specified
       */
      throw new SOAPException("document binding must, in its soapbind:body "
          + "elements, have at most one part listed in attribute 'parts': "
          + binding);
    }
    /*
     * BP 1.2 section 4.4.1: For document-literal bindings, the Profile requires that at most one
     * part be serialized into the soap:Body element.
     */
    List wsdlParts = getWsdlParts(partNames, message);
    assert wsdlParts.size() <= 1 : wsdlParts.size();

    // fill in single part value
    if (wsdlParts.size() == 1) {
      Part wsdlPart = (Part) wsdlParts.get(0);
      /*
       * BP 1.2 R2204: A document-literal binding MUST refer, in each of its soapbind:body
       * element(s), only to wsdl:part element(s) that have been defined using the element
       * attribute.
       */
      QName elementName = wsdlPart.getElementName();
      if (elementName == null) {
        throw new SOAPException("document binding must refer, in its "
            + "soapbind:body elements, only to wsdl:part elements defined "
            + "using attribute 'element': "
            + binding);
      }
      /*
       * BP 1.2 R2712: A document binding MUST be serialized as an ENVELOPE with a soap:Body whose
       * child element is an instance of the global element declaration referenced by the
       * corresponding wsdl:message part
       */
      Element part = (Element) parts.get(wsdlPart.getName());
      if (!XmlUtil.nodeNameEquals(part, elementName)) {
        throw new SOAPException("document binding must be serialized as an "
            + "envelope with a soap:Body whose child element is an instance "
            + "of the element declaration referenced by the corresponding "
            + "wsdl:message part: "
            + binding);
      }
      // copy part to element inside env:Body
      SoapUtil.copyChildElement(body, part);
    }
  }

  private static List getWsdlParts(List partNames, Message message) {
    /*
     * WSDL 1.1 section 3.5: If the parts attribute is omitted, then all parts defined by the
     * message are assumed to be included in the SOAP Body portion
     * 
     * BP 1.2 R2214: In a rpc-literal description where the value of the parts attribute of
     * soapbind:body is an empty string, the corresponding ENVELOPE MUST have no part accessor
     * elements
     * 
     * Message.getOrderedParts() implements section 3.5 and R2214 as follows. If partNames is null,
     * then return all wsdl:part elements. Otherwise, if partNames is empty, then return zero
     * wsdl:part elements.
     */
    return message.getOrderedParts(partNames);
  }

  public void readMessage(String operation, SOAPMessage message, Map parts,
      MessageDirection direction) throws SOAPException {
    // obtain operation binding details
    BindingOperation bindOperation = binding.getBindingOperation(operation, null, null);
    SOAPOperation soapOperation = (SOAPOperation) WsdlUtil.getExtension(
        bindOperation.getExtensibilityElements(), SOAPConstants.Q_ELEM_SOAP_OPERATION);

    // determine operation style
    String style = soapOperation.getStyle();
    if (style == null) {
      // fall back to value specified in wsdlsoap:binding
      SOAPBinding soapBinding = (SOAPBinding) WsdlUtil.getExtension(
          binding.getExtensibilityElements(), SOAPConstants.Q_ELEM_SOAP_BINDING);
      style = soapBinding.getStyle();
      if (style == null) {
        // wsdlsoap:binding does not specify any style, assume 'document'
        style = SoapBindConstants.DOCUMENT_STYLE;
      }
    }

    // read env:Body
    if (SoapBindConstants.DOCUMENT_STYLE.equals(style))
      readDocumentBody(bindOperation, message.getSOAPBody(), parts, direction);
    else
      readRpcBody(bindOperation, message.getSOAPBody(), parts, direction);
  }

  protected void readRpcBody(BindingOperation bindOperation, javax.xml.soap.SOAPBody body,
      Map parts, MessageDirection direction) throws SOAPException {
    // obtain soapbind:body element
    SOAPBody soapBindBody = direction.getBodyDescription(bindOperation);
    /*
     * BP 1.2 R2706: A wsdl:binding MUST use the value of "literal" for the use attribute in all
     * soapbind:body elements
     */
    if (!SoapBindConstants.LITERAL_USE.equals(soapBindBody.getUse())) {
      throw new SOAPException("binding must use value 'literal' for attribute "
          + "'use' in all soapbind:body elements: "
          + binding);
    }
    /*
     * BP 1.2 R2717: An rpc-literal binding MUST have the namespace attribute specified, the value
     * of which MUST be an absolute URI, on contained soapbind:body elements
     */
    String operationNamespaceUri = soapBindBody.getNamespaceURI();
    if (operationNamespaceUri == null) {
      throw new SOAPException("rpc binding must have the namespace attribute "
          + "specified on contained soapbind:body elements: "
          + binding);
    }
    // get operation wrapper
    Operation operation = bindOperation.getOperation();
    SOAPElement operationWrapper = SoapUtil.getElement(body, operationNamespaceUri,
        direction.getRpcWrapperLocalName(operation));

    /*
     * WSDL 1.1 section 2.4.6: Note that [the parameterOrder attribute] serves as a "hint" and may
     * safely be ignored by those not concerned with RPC signatures.
     * 
     * BPEL is not concerned with RPC signatures, so ignore parameterOrder
     */
    List partNames = soapBindBody.getParts();
    List wsdlParts = getWsdlParts(partNames, direction.getMessageDefinition(operation));

    // fill in parts
    for (int i = 0, n = wsdlParts.size(); i < n; i++) {
      Part wsdlPart = (Part) wsdlParts.get(i);
      /*
       * BP 1.2 R2203: An rpc-literal binding MUST refer, in its soapbind:body element(s), only to
       * wsdl:part element(s) that have been defined using the type attribute
       */
      if (wsdlPart.getTypeName() == null) {
        throw new SOAPException("rpc binding must refer, in its soapbind:body "
            + "elements, only to wsdl:part elements defined using attribute 'type': "
            + binding);
      }

      // create part
      String partName = wsdlPart.getName();
      Element part = XmlUtil.createElement(partName);
      parts.put(partName, part);

      SOAPElement accessor = SoapUtil.getElement(operationWrapper, partName);
      /*
       * BPEL-243 - XML Schema Part 1 Second Edition, section 2.6.2: An element may be ·valid·
       * without content if it has the attribute xsi:nil with the value true
       */
      Boolean nil = DatatypeUtil.parseBoolean(accessor.getAttributeNS(
          BpelConstants.NS_XML_SCHEMA_INSTANCE, BpelConstants.ATTR_NIL));
      if (nil != Boolean.TRUE) {
        // copy accessor inside operation wrapper to part
        SoapUtil.copy(part, accessor);
      }
      else {
        /*
         * XML Schema Part 1 Second Edition, section 2.6.2: An element so labeled must be empty, but
         * can carry attributes if permitted by the corresponding complex type
         */
        SoapUtil.copyAttributes(part, accessor); // copies xsi:nil as well
        // do not copy child nodes
      }
    }
  }

  protected void readDocumentBody(BindingOperation bindOperation, javax.xml.soap.SOAPBody body,
      Map parts, MessageDirection direction) throws SOAPException {
    // obtain soapbind:body element
    SOAPBody soapBindBody = direction.getBodyDescription(bindOperation);
    /*
     * BP 1.2 R2706: A wsdl:binding MUST use the value of "literal" for the use attribute in all
     * soapbind:body elements
     */
    if (!SoapBindConstants.LITERAL_USE.equals(soapBindBody.getUse())) {
      throw new SOAPException("binding must use value 'literal' for attribute "
          + "'use' in all soapbind:body elements: "
          + binding);
    }
    /*
     * BP 1.2 R2716: A document-literal binding MUST NOT have the namespace attribute specified on
     * contained soapbind:body elements
     */
    if (soapBindBody.getNamespaceURI() != null) {
      throw new SOAPException("document binding must not have attribute "
          + "'namespace' specified on contained soapbind:body elements: "
          + binding);
    }

    /*
     * WSDL 1.1 section 2.4.6: Note that [the parameterOrder attribute] serves as a "hint" and may
     * safely be ignored by those not concerned with RPC signatures.
     * 
     * BPEL is not concerned with RPC signatures, so ignore parameterOrder
     */
    List partNames = soapBindBody.getParts();
    Message message = direction.getMessageDefinition(bindOperation.getOperation());
    if (partNames == null) {
      /*
       * BP 1.2 R2210: If a document-literal binding does not specify the parts attribute on a
       * soapbind:body element, the corresponding abstract wsdl:message MUST define zero or one
       * wsdl:parts
       */
      if (message.getParts().size() > 1) {
        throw new SOAPException("if a document binding does not specify "
            + "attribute 'parts' on a soapbind:body element, the corresponding "
            + "wsdl:message must define zero or one wsdl:parts: "
            + binding);
      }
    }
    else if (partNames.size() > 1) {
      /*
       * BP 1.2 R2201: A document-literal binding MUST, in each of its soapbind:body element(s),
       * have at most one part listed in the parts attribute, if the parts attribute is specified
       */
      throw new SOAPException("document binding must, in its soapbind:body "
          + "elements, have at most one part listed in attribute 'parts': "
          + binding);
    }
    List wsdlParts = getWsdlParts(partNames, message);

    /*
     * BP 1.2 section 4.4.1: For document-literal bindings, the Profile requires that at most one
     * part, abstractly defined with the element attribute, be serialized into the soap:Body
     * element.
     */
    assert wsdlParts.size() <= 1 : wsdlParts.size();

    if (wsdlParts.size() == 1) {
      Part wsdlPart = (Part) wsdlParts.get(0);
      /*
       * BP 1.2 R2204: A document-literal binding MUST refer, in each of its soapbind:body
       * element(s), only to wsdl:part element(s) that have been defined using the element
       * attribute.
       */
      QName elementName = wsdlPart.getElementName();
      if (elementName == null) {
        throw new SOAPException("document binding must refer, in its "
            + "soapbind:body elements, only to wsdl:part elements defined "
            + "using attribute 'element': "
            + binding);
      }
      // create part
      Element part = XmlUtil.createElement(elementName);
      parts.put(wsdlPart.getName(), part);
      /*
       * BP 1.2 R2712: A document-literal binding MUST be serialized as an ENVELOPE with a soap:Body
       * whose child element is an instance of the global element declaration referenced by the
       * corresponding wsdl:message part
       */
      SOAPElement element = SoapUtil.getElement(body, elementName);
      if (element == null) {
        throw new SOAPException("document binding must be serialized as an "
            + "envelope with a soap:Body whose child element is an instance "
            + "of the element declaration referenced by the corresponding "
            + "wsdl:message part: "
            + binding);
      }
      /*
       * BPEL-243 - XML Schema Part 1 Second Edition, section 2.6.2: An element may be ·valid·
       * without content if it has the attribute xsi:nil with the value true
       */
      Boolean nil = DatatypeUtil.parseBoolean(element.getAttributeNS(
          BpelConstants.NS_XML_SCHEMA_INSTANCE, BpelConstants.ATTR_NIL));
      if (nil != Boolean.TRUE) {
        // copy element inside env:Body to part
        SoapUtil.copy(part, element);
      }
      else {
        /*
         * XML Schema Part 1 Second Edition, section 2.6.2: An element so labeled must be empty, but
         * can carry attributes if permitted by the corresponding complex type
         */
        SoapUtil.copyAttributes(part, element); // copies xsi:nil as well
        // do not copy child nodes
      }
    }
  }

  public void writeFault(String operation, SOAPMessage message, String fault, Map parts,
      QName code, String reason) throws SOAPException {
    SOAPElement faultElem = faultFormat.addFault(message.getSOAPBody());
    faultFormat.setCode(faultElem, code);
    faultFormat.setReason(faultElem, reason);

    if (fault == null)
      return; // no fault detail

    BindingOperation bindOperation = binding.getBindingOperation(operation, null, null);
    BindingFault bindFault = bindOperation.getBindingFault(fault);
    if (bindFault == null)
      throw new SOAPException("fault '" + fault + "' not found: " + binding);

    // obtain soapbind:fault element
    SOAPFault soapBindFault = (SOAPFault) WsdlUtil.getExtension(
        bindFault.getExtensibilityElements(), SOAPConstants.Q_ELEM_SOAP_FAULT);
    /*
     * BP 1.2 R2721: A wsdl:binding MUST have the name attribute specified on all contained
     * soapbind:fault elements
     */
    if (soapBindFault.getName() == null) {
      throw new SOAPException("a wsdl:binding must have attribute 'name' "
          + "specified on all contained soapbind:fault elements: "
          + binding);
    }
    /*
     * BP 1.2 R2754: the value of the name attribute on a soapbind:fault element MUST match the
     * value of the name attribute on its parent wsdl:fault element.
     */
    if (!soapBindFault.getName().equals(fault)) {
      throw new SOAPException("value of attribute 'name' on a "
          + "soapbind:fault element must match the value of attribute "
          + "'name' on its parent wsdl:fault element: "
          + binding);
    }
    /*
     * BP 1.2 R2706: A wsdl:binding MUST use the value of "literal" for the use attribute in all
     * soapbind:fault elements
     */
    if (!SoapBindConstants.LITERAL_USE.equals(soapBindFault.getUse())) {
      throw new SOAPException("binding must use value 'literal' for attribute "
          + "'use' in all soapbind:fault elements: "
          + binding);
    }
    /*
     * BP 1.2 R2716: A document-literal binding MUST NOT have the namespace attribute specified on
     * contained soapbind:fault elements
     */
    if (soapBindFault.getNamespaceURI() != null) {
      throw new SOAPException("document binding must not have attribute "
          + "'namespace' specified on contained soapbind:fault elements: "
          + binding);
    }
    /*
     * WSDL 1.1 section 3.6: The fault message MUST have a single part
     */
    Fault wsdlFault = bindOperation.getOperation().getFault(fault);
    Map wsdlParts = wsdlFault.getMessage().getParts();
    if (wsdlParts.size() != 1)
      throw new SOAPException("fault messages must have a single part: " + binding);
    /*
     * BP 1.2 R2205: A wsdl:binding MUST refer, in each of its soapbind:fault elements, only to
     * wsdl:part element(s) that have been defined using the element attribute
     */
    Part wsdlPart = (Part) wsdlParts.values().iterator().next();
    QName elementName = wsdlPart.getElementName();
    if (elementName == null) {
      throw new SOAPException("binding must refer, in each of its "
          + "soapbind:fault elements, only to wsdl:parts that have been "
          + "defined using attribute 'element': "
          + binding);
    }
    /*
     * WSDL 1.1 section 3.6: The soapbind:fault element specifies the contents of the SOAP Fault
     * detail element
     */
    Element part = (Element) parts.get(wsdlPart.getName());
    if (!XmlUtil.nodeNameEquals(part, elementName)) {
      throw new SOAPException("soapbind:fault element does not match the "
          + "given contents: "
          + binding);
    }
    faultFormat.addDetail(faultElem, parts);
  }

  public Fault readFault(String operation, SOAPMessage message, Map parts) throws SOAPException {
    SOAPElement faultElem = faultFormat.getFault(message.getSOAPBody());
    SOAPElement detail = faultFormat.getDetail(faultElem);
    if (detail == null)
      throw new SOAPException("soap fault does not include a detail element");

    BindingOperation bindOperation = binding.getBindingOperation(operation, null, null);
    // look for a wsdl fault which matches detail content
    for (Iterator i = bindOperation.getOperation().getFaults().values().iterator(); i.hasNext();) {
      Fault wsdlFault = (Fault) i.next();

      // obtain definition of part which appears inside detail
      Map wsdlParts = wsdlFault.getMessage().getParts();
      if (wsdlParts.size() != 1)
        throw new SOAPException("multiple parts not supported in fault");

      /*
       * BP 1.2 R2205: A wsdl:binding MUST refer, in each of its soapbind:fault elements, only to
       * wsdl:part element(s) that have been defined using the element attribute
       */
      Part wsdlPart = (Part) wsdlParts.values().iterator().next();
      QName elementName = wsdlPart.getElementName();
      if (elementName == null) {
        throw new SOAPException("binding must refer, in each of its "
            + "soapbind:fault elements, only to wsdl:part elements that have "
            + "been defined using attribute 'element': "
            + binding);
      }

      // obtain element inside detail
      SOAPElement element = SoapUtil.getElement(detail, elementName);
      if (element == null)
        continue; // this is not the wsdl:fault we are looking for

      // obtain soapbind:fault element
      String faultName = wsdlFault.getName();
      BindingFault bindFault = bindOperation.getBindingFault(faultName);
      SOAPFault soapBindFault = (SOAPFault) WsdlUtil.getExtension(
          bindFault.getExtensibilityElements(), SOAPConstants.Q_ELEM_SOAP_FAULT);
      /*
       * BP 1.2 R2721: A wsdl:binding MUST have the name attribute specified on all contained
       * soapbind:fault elements
       */
      if (soapBindFault.getName() == null) {
        throw new SOAPException("binding must have attribute 'name' "
            + "specified on all contained soapbind:fault elements: "
            + binding);
      }
      /*
       * BP 1.2 R2754: the value of the name attribute on a soapbind:fault element MUST match the
       * value of the name attribute on its parent wsdl:fault element.
       */
      if (!soapBindFault.getName().equals(faultName)) {
        throw new SOAPException("value of attribute 'name' on a "
            + "soapbind:fault element must match the value of attribute "
            + "'name' on its parent wsdl:fault element: "
            + binding);
      }
      /*
       * BP 1.2 R2706: A wsdl:binding MUST use the value of "literal" for the use attribute in all
       * soapbind:fault elements
       */
      if (!SoapBindConstants.LITERAL_USE.equals(soapBindFault.getUse())) {
        throw new SOAPException("binding must use value 'literal' for "
            + "attribute 'use' in all soapbind:fault elements: "
            + binding);
      }

      // create part
      Element part = XmlUtil.createElement(elementName);
      parts.put(wsdlPart.getName(), part);

      // copy element inside detail to part
      SoapUtil.copy(part, element);

      return wsdlFault;
    }
    throw new SOAPException("no wsdl fault matches the detail element content");
  }

  public boolean hasFault(SOAPMessage message) throws SOAPException {
    return faultFormat.hasFault(message);
  }
}
