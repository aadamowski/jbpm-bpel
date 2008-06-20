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

import javax.wsdl.Fault;
import javax.wsdl.Message;
import javax.wsdl.Operation;
import javax.wsdl.OperationType;
import javax.wsdl.PortType;
import javax.xml.namespace.QName;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;

import org.jbpm.bpel.graph.basic.Reply;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.CompositeActivity;
import org.jbpm.bpel.integration.def.PartnerLinkDefinition;
import org.jbpm.bpel.integration.def.ReplyAction;
import org.jbpm.bpel.variable.def.VariableDefinition;
import org.jbpm.bpel.wsdl.PartnerLinkType.Role;
import org.jbpm.bpel.xml.util.XmlUtil;

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/09/04 06:42:26 $
 */
public class ReplyReader extends ActivityReader {

  /**
   * Loads the activity properties from the given DOM element
   */
  public Activity read(Element activityElem, CompositeActivity parent) {
    Reply reply = new Reply();
    readStandardProperties(activityElem, reply, parent);
    readReply(activityElem, reply);
    return reply;
  }

  public void readReply(Element replyElem, Reply reply) {
    validateNonInitial(replyElem, reply);
    reply.setAction(readReplyAction(replyElem, reply.getCompositeActivity()));
  }

  public ReplyAction readReplyAction(Element replyElem, CompositeActivity parent) {
    ReplyAction replyAction = new ReplyAction();

    // partner link
    String partnerLinkName = replyElem.getAttribute(BpelConstants.ATTR_PARTNER_LINK);
    PartnerLinkDefinition partnerLink = parent.findPartnerLink(partnerLinkName);
    if (partnerLink == null) {
      bpelReader.getProblemHandler().add(new ParseProblem("partner link not found", replyElem));
      return replyAction;
    }
    replyAction.setPartnerLink(partnerLink);

    // port type
    Role myRole = partnerLink.getMyRole();
    // BPEL-181 detect absence of my role
    if (myRole == null) {
      bpelReader.getProblemHandler().add(
          new ParseProblem("partner link does not indicate my role", replyElem));
      return replyAction;
    }
    PortType portType = bpelReader.getMessageActivityPortType(replyElem, partnerLink.getMyRole());

    // operation
    Operation operation = bpelReader.getMessageActivityOperation(replyElem, portType);
    if (operation.getStyle() != OperationType.REQUEST_RESPONSE) {
      bpelReader.getProblemHandler().add(
          new ParseProblem("not a request/response operation", replyElem));
      return replyAction;
    }
    replyAction.setOperation(operation);

    // message exchange
    // BPEL-74: map the empty message exchange to null for compatibility with Oracle
    replyAction.setMessageExchange(XmlUtil.getAttribute(replyElem,
        BpelConstants.ATTR_MESSAGE_EXCHANGE));

    // fault name
    Message replyMessage;
    Attr faultNameAttr = replyElem.getAttributeNode(BpelConstants.ATTR_FAULT_NAME);
    if (faultNameAttr != null) {
      QName faultName = XmlUtil.getQNameValue(faultNameAttr);
      replyAction.setFaultName(faultName);

      Fault fault = operation.getFault(faultName.getLocalPart());
      if (fault == null) {
        bpelReader.getProblemHandler().add(new ParseProblem("fault not found", replyElem));
        return replyAction;
      }
      replyMessage = fault.getMessage();
    }
    else
      replyMessage = operation.getOutput().getMessage();

    // variable
    VariableDefinition variable = bpelReader.getMessageActivityVariable(replyElem,
        BpelConstants.ATTR_VARIABLE, parent, replyMessage);
    replyAction.setVariable(variable);

    // correlations
    Element correlationsElement = XmlUtil.getElement(replyElem, BpelConstants.NS_BPEL,
        BpelConstants.ELEM_CORRELATIONS);
    if (correlationsElement != null)
      replyAction.setCorrelations(bpelReader.readCorrelations(correlationsElement, parent, variable));

    return replyAction;
  }
}
