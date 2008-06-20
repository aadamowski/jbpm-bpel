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

import java.util.ArrayList;
import java.util.List;

import javax.wsdl.BindingInput;
import javax.wsdl.BindingOperation;
import javax.wsdl.BindingOutput;
import javax.wsdl.Message;
import javax.wsdl.Operation;
import javax.wsdl.extensions.soap.SOAPBody;

import org.apache.commons.lang.enums.Enum;

import com.ibm.wsdl.extensions.soap.SOAPConstants;

import org.jbpm.bpel.wsdl.xml.WsdlUtil;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/06/09 23:36:41 $
 */
public abstract class MessageDirection extends Enum {

  private MessageDirection(String name) {
    super(name);
  }

  abstract SOAPBody getBodyDescription(BindingOperation bindOperation);

  abstract Message getMessageDefinition(Operation operation);

  abstract List getRpcBodyPartNames(SOAPBody soapBody, Operation operation);

  abstract String getRpcWrapperLocalName(Operation operation);

  public static final MessageDirection INPUT = new MessageDirection("input") {

    private static final long serialVersionUID = 1L;

    SOAPBody getBodyDescription(BindingOperation bindOperation) {
      BindingInput bindInput = bindOperation.getBindingInput();
      return (SOAPBody) WsdlUtil.getExtension(
          bindInput.getExtensibilityElements(), SOAPConstants.Q_ELEM_SOAP_BODY);
    }

    List getRpcBodyPartNames(SOAPBody soapBody, Operation operation) {
      /*
       * WSDL 1.1 section 3.5: The optional parts attribute indicates which
       * parts appear somewhere within the SOAP Body portion of the message
       */
      List partNames = soapBody.getParts();

      List parameterOrder = operation.getParameterOrdering();
      if (parameterOrder == null)
        return partNames;

      /*
       * WSDL 1.1 section 3.5: Parts are arranged in the same order as the
       * parameters of the call
       */
      parameterOrder = new ArrayList(parameterOrder);

      if (partNames == null) {
        /*
         * WSDL 1.1 section 3.5: If the parts attribute is omitted, then all
         * parts defined by the message are assumed to be included in the SOAP
         * Body portion
         */
        parameterOrder.retainAll(getMessageDefinition(operation).getParts()
            .keySet());
      }
      else
        parameterOrder.retainAll(partNames);

      return parameterOrder;
    }

    Message getMessageDefinition(Operation operation) {
      return operation.getInput().getMessage();
    }

    String getRpcWrapperLocalName(Operation operation) {
      return operation.getName();
    }
  };

  public static final MessageDirection OUTPUT = new MessageDirection("output") {

    private static final long serialVersionUID = 1L;

    SOAPBody getBodyDescription(BindingOperation bindOperation) {
      BindingOutput bindOutput = bindOperation.getBindingOutput();
      return (SOAPBody) WsdlUtil.getExtension(
          bindOutput.getExtensibilityElements(), SOAPConstants.Q_ELEM_SOAP_BODY);
    }

    List getRpcBodyPartNames(SOAPBody soapBody, Operation operation) {
      return soapBody.getParts();
    }

    Message getMessageDefinition(Operation operation) {
      return operation.getOutput().getMessage();
    }

    String getRpcWrapperLocalName(Operation operation) {
      /*
       * BP 1.2 R2729: An ENVELOPE described with an rpc-literal binding that is
       * a response MUST have a wrapper element whose name is the corresponding
       * wsdl:operation name suffixed with the string "Response".
       */
      return operation.getName() + "Response";
    }
  };
}