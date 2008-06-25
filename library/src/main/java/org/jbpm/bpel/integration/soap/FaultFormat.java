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
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.soap.Detail;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPMessage;

import org.apache.commons.lang.enums.Enum;
import org.w3c.dom.Element;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2008/01/30 07:18:22 $
 */
public abstract class FaultFormat extends Enum {

  private FaultFormat(String name) {
    super(name);
  }

  abstract SOAPElement addFault(SOAPBody body) throws SOAPException;

  abstract void setCode(SOAPElement faultElem, QName code) throws SOAPException;

  abstract void setReason(SOAPElement faultElem, String reason) throws SOAPException;

  abstract void addDetail(SOAPElement faultElem, Map parts) throws SOAPException;

  abstract boolean hasFault(SOAPMessage message) throws SOAPException;

  abstract SOAPElement getFault(SOAPBody body);

  abstract SOAPElement getDetail(SOAPElement faultElem);

  public static final FaultFormat DEFAULT = new FaultFormat("default") {

    private static final long serialVersionUID = 1L;

    SOAPElement addFault(SOAPBody body) throws SOAPException {
      return body.addFault();
    }

    void setCode(SOAPElement faultElem, QName code) throws SOAPException {
      SOAPFault fault = (SOAPFault) faultElem;
      SOAPEnvelope envelope = (SOAPEnvelope) faultElem.getParentElement().getParentElement();
      fault.setFaultCode(envelope.createName(code.getLocalPart(), code.getPrefix(),
          code.getNamespaceURI()));
    }

    void setReason(SOAPElement faultElem, String reason) throws SOAPException {
      SOAPFault fault = (SOAPFault) faultElem;
      fault.setFaultString(reason);
    }

    void addDetail(SOAPElement faultElem, Map parts) throws SOAPException {
      SOAPFault fault = (SOAPFault) faultElem;
      Detail detail = fault.addDetail();

      // walk through message parts
      for (Iterator i = parts.values().iterator(); i.hasNext();) {
        Element part = (Element) i.next();
        SoapUtil.copyChildElement(detail, part);
      }
    }

    boolean hasFault(SOAPMessage message) throws SOAPException {
      return message.getSOAPBody().hasFault();
    }

    SOAPElement getFault(SOAPBody body) {
      return body.getFault();
    }

    SOAPElement getDetail(SOAPElement faultElem) {
      SOAPFault fault = (SOAPFault) faultElem;
      return fault.getDetail();
    }
  };

  public static final FaultFormat RAW = new FaultFormat("raw") {

    private static final long serialVersionUID = 1L;

    SOAPElement addFault(SOAPBody body) throws SOAPException {
      /*
       * jboss-ws4ee throws ClassCastException upon calling the remote endpoint if child elements
       * other than SOAPBodyElements are added to SOAPBody
       */
      SOAPEnvelope envelope = (SOAPEnvelope) body.getParentElement();
      Name faultName = envelope.createName("Fault", body.getPrefix(), body.getNamespaceURI());
      return body.addBodyElement(faultName);
    }

    void setCode(SOAPElement faultElem, QName code) throws SOAPException {
      SOAPElement codeElem = SoapUtil.addChildElement(faultElem, "faultcode");
      SoapUtil.setQNameValue(codeElem, code);
    }

    void setReason(SOAPElement faultElem, String reason) throws SOAPException {
      SOAPElement stringElem = SoapUtil.addChildElement(faultElem, "faultstring");
      stringElem.setValue(reason);
    }

    void addDetail(SOAPElement faultElem, Map parts) throws SOAPException {
      SOAPElement detail = SoapUtil.addChildElement(faultElem, "detail");

      // walk through message parts
      for (Iterator i = parts.values().iterator(); i.hasNext();) {
        Element part = (Element) i.next();
        SoapUtil.copyChildElement(detail, part);
      }
    }

    boolean hasFault(SOAPMessage message) throws SOAPException {
      return getFault(message.getSOAPBody()) != null;
    }

    SOAPElement getFault(SOAPBody body) {
      return SoapUtil.getElement(body, body.getNamespaceURI(), "Fault");
    }

    SOAPElement getDetail(SOAPElement faultElem) {
      return SoapUtil.getElement(faultElem, "detail");
    }
  };
}