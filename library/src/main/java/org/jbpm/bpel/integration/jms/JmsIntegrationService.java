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
package org.jbpm.bpel.integration.jms;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.Session;
import javax.wsdl.Operation;
import javax.wsdl.Port;
import javax.wsdl.extensions.soap.SOAPAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ibm.wsdl.extensions.soap.SOAPConstants;

import org.jbpm.JbpmContext;
import org.jbpm.bpel.BpelException;
import org.jbpm.bpel.endpointref.EndpointReference;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.exe.BpelFaultException;
import org.jbpm.bpel.integration.IntegrationService;
import org.jbpm.bpel.integration.client.Caller;
import org.jbpm.bpel.integration.client.SoapCaller;
import org.jbpm.bpel.integration.def.InvokeAction;
import org.jbpm.bpel.integration.def.PartnerLinkDefinition;
import org.jbpm.bpel.integration.def.ReceiveAction;
import org.jbpm.bpel.integration.def.ReplyAction;
import org.jbpm.bpel.variable.def.MessageType;
import org.jbpm.bpel.variable.exe.MessageValue;
import org.jbpm.bpel.wsdl.xml.WsdlUtil;
import org.jbpm.graph.exe.Token;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2008/01/30 07:18:22 $
 */
public class JmsIntegrationService implements IntegrationService {

  private final JmsIntegrationServiceFactory factory;

  private List requestListeners = new ArrayList();

  private static final Log log = LogFactory.getLog(JmsIntegrationService.class);
  private static final long serialVersionUID = 1L;

  JmsIntegrationService(JmsIntegrationServiceFactory factory) {
    this.factory = factory;
  }

  public void receive(ReceiveAction receiveAction, Token token, boolean oneShot) {
    IntegrationControl integrationControl = getIntegrationControl(token);
    try {
      jmsReceive(receiveAction, token, integrationControl, oneShot);
    }
    catch (JMSException e) {
      throw new BpelException("could not create request listener", e);
    }
  }

  void jmsReceive(ReceiveAction receiveAction, Token token, IntegrationControl integrationControl,
      boolean oneShot) throws JMSException {
    Session jmsSession = createJmsSession(integrationControl);
    RequestListener requestListener = new RequestListener(receiveAction, token, integrationControl,
        jmsSession, oneShot);
    requestListeners.add(requestListener);
  }

  public void receive(List receivers, Token token) {
    IntegrationControl integrationControl = getIntegrationControl(token);
    try {
      jmsReceive(receivers, token, integrationControl);
    }
    catch (JMSException e) {
      throw new BpelException("could not create request listeners", e);
    }
  }

  void jmsReceive(List receivers, Token token, IntegrationControl integrationControl)
      throws JMSException {
    Session jmsSession = createJmsSession(integrationControl);
    Iterator receiverIt = receivers.iterator();
    while (receiverIt.hasNext()) {
      ReceiveAction receiveAction = (ReceiveAction) receiverIt.next();
      RequestListener requestListener = new RequestListener(receiveAction, token,
          integrationControl, jmsSession);
      requestListeners.add(requestListener);
    }
  }

  private static Session createJmsSession(IntegrationControl integrationControl)
      throws JMSException {
    return integrationControl.getJmsConnection().createSession(false, Session.CLIENT_ACKNOWLEDGE);
  }

  public void cancelReception(ReceiveAction receiveAction, Token token) {
    try {
      jmsCancelReception(receiveAction, token, getIntegrationControl(token));
    }
    catch (JMSException e) {
      log.debug("could not close request listener", e);
    }
  }

  void jmsCancelReception(ReceiveAction receiveAction, Token token,
      IntegrationControl integrationControl) throws JMSException {
    RequestListener requestListener = integrationControl.removeRequestListener(receiveAction, token);
    // some competing thread might have removed the request listener already
    if (requestListener == null)
      return;
    /*
     * this service may or may not have created the request listener; in the affirmative case, the
     * call below prevents the listener from being opened when the service closes; in the negative
     * case, the call has no effect
     */
    requestListeners.remove(requestListener);
    // release listener resources
    requestListener.close();
  }

  public void reply(ReplyAction replyAction, Token token) {
    try {
      replyOutstandingRequest(replyAction, token);
    }
    catch (JMSException e) {
      throw new BpelException("could not send reply", e);
    }
  }

  private void replyOutstandingRequest(ReplyAction replyAction, Token token) throws JMSException {
    // extract the output parts
    Map parts = replyAction.writeMessage(token);

    // obtain the outstanding request for the partner link, operation and
    // message exchange of the replier
    IntegrationControl integrationControl = getIntegrationControl(token);
    OutstandingRequest request = integrationControl.removeOutstandingRequest(replyAction, token);

    Session jmsSession = createJmsSession(integrationControl);
    try {
      request.sendReply(parts, replyAction.getFaultName(), jmsSession);
    }
    finally {
      jmsSession.close();
    }
  }

  public void invoke(InvokeAction invokeAction, Token token) {
    // acquire caller for partner
    IntegrationControl integrationControl = getIntegrationControl(token);
    Caller caller = integrationControl.createCaller(invokeAction, token);
    try {
      // extract input parts
      Map inputParts = invokeAction.writeMessage(token);
      log.debug("sending input " + inputParts + " on " + invokeAction + " for " + token);

      // is this a request/response operation?
      Operation operation = invokeAction.getOperation();
      if (operation.getOutput() != null) {
        try {
          // send input, block for output
          Map outputParts = caller.call(operation.getName(), inputParts);
          log.debug("received output " + outputParts + " on " + invokeAction + " for " + token);
          // assign output parts
          invokeAction.readMessage(token, outputParts);
        }
        catch (BpelFaultException e) {
          replaceMessageType(e, token);
          throw e;
        }
      }
      else {
        // fire and forget
        caller.callOneWay(operation.getName(), inputParts);
      }
    }
    finally {
      integrationControl.removeCaller(invokeAction, token);
      caller.close();
    }
  }

  /**
   * Replaces the transient message type in the given fault exception with the persistent object
   * from the process definition. {@link SoapCaller} produces faults with transient message types.
   */
  private static void replaceMessageType(BpelFaultException faultException, Token token) {
    // extract the message value from the exception
    MessageValue faultData = faultException.getFaultInstance().getMessageValue();

    // BPEL-286 a request/response call that throws SOAPException raises fault with no data
    if (faultData == null)
      return;

    // retrieve persistent type from import definition
    BpelProcessDefinition processDefinition = (BpelProcessDefinition) token.getProcessInstance()
        .getProcessDefinition();
    MessageType persistentType = processDefinition.getImportDefinition().getMessageType(
        faultData.getType().getName());

    // replace transient type with persistent type
    faultData.setType(persistentType);
  }

  public void cancelInvocation(InvokeAction invokeAction, Token token) {
    Caller caller = getIntegrationControl(token).removeCaller(invokeAction, token);
    if (caller != null)
      caller.close();
  }

  public EndpointReference getMyReference(PartnerLinkDefinition partnerLink, Token token) {
    IntegrationControl integrationControl = getIntegrationControl(token);
    EndpointReference myReference = integrationControl.getPartnerLinkEntry(partnerLink)
        .getMyReference();
    // fill in address, if missing
    if (myReference.getAddress() == null) {
      Port myPort = myReference.selectPort(integrationControl.getMyCatalog());
      SOAPAddress soapAddress = (SOAPAddress) WsdlUtil.getExtension(
          myPort.getExtensibilityElements(), SOAPConstants.Q_ELEM_SOAP_ADDRESS);
      if (soapAddress != null)
        myReference.setAddress(soapAddress.getLocationURI());
    }
    return myReference;
  }

  public void close() {
    JbpmContext jbpmContext = factory.getJbpmConfiguration().getCurrentJbpmContext();
    // open request listeners only if transaction is not marked for rollback
    if (!jbpmContext.getServices().getTxService().isRollbackOnly()) {
      try {
        openRequestListeners();
      }
      catch (JMSException e) {
        throw new BpelException("could not open request listeners", e);
      }
    }
    else
      closeRequestListeners();
  }

  private void openRequestListeners() throws JMSException {
    for (int i = 0, n = requestListeners.size(); i < n; i++) {
      RequestListener requestListener = (RequestListener) requestListeners.get(i);
      requestListener.open();
    }
  }

  private void closeRequestListeners() {
    for (int i = 0, n = requestListeners.size(); i < n; i++) {
      RequestListener requestListener = (RequestListener) requestListeners.get(i);
      try {
        requestListener.close();
      }
      catch (JMSException e) {
        log.debug("could not close request listener", e);
      }
    }
  }

  IntegrationControl getIntegrationControl(Token token) {
    return factory.getIntegrationControl(token.getProcessInstance().getProcessDefinition());
  }

  public static JmsIntegrationService get(JbpmContext jbpmContext) {
    return (JmsIntegrationService) jbpmContext.getServices().getService(
        IntegrationService.SERVICE_NAME);
  }
}
