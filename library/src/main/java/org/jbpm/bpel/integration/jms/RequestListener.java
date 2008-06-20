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

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.QueueReceiver;
import javax.jms.Session;
import javax.xml.namespace.QName;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.StaleStateException;
import org.hibernate.exception.LockAcquisitionException;

import org.jbpm.JbpmContext;
import org.jbpm.bpel.graph.exe.BpelFaultException;
import org.jbpm.bpel.integration.def.Correlations;
import org.jbpm.bpel.integration.def.ReceiveAction;
import org.jbpm.bpel.persistence.db.IntegrationSession;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2008/06/12 08:18:54 $
 */
public class RequestListener implements MessageListener {

  private final long receiveActionId;
  private final long tokenId;

  private final IntegrationControl integrationControl;
  private final Session jmsSession;
  private final MessageConsumer messageConsumer;

  private final boolean oneShot;

  private static final Log log = LogFactory.getLog(RequestListener.class);

  RequestListener(ReceiveAction receiveAction, Token token, IntegrationControl integrationControl,
      Session jmsSession) throws JMSException {
    this(receiveAction, token, integrationControl, jmsSession, true);
  }

  RequestListener(ReceiveAction receiveAction, Token token, IntegrationControl integrationControl,
      Session jmsSession, boolean oneShot) throws JMSException {
    this.receiveActionId = receiveAction.getId();
    this.tokenId = token.getId();

    this.integrationControl = integrationControl;
    this.jmsSession = jmsSession;

    this.oneShot = oneShot;

    // create message consumer
    Destination destination = integrationControl.getPartnerLinkEntry(receiveAction.getPartnerLink())
        .getDestination();
    String selector = formatSelector(receiveAction, token);
    this.messageConsumer = jmsSession.createConsumer(destination, selector);

    integrationControl.addRequestListener(this);
    log.debug("created request listener: receiveAction=" + receiveAction + ", token=" + token);
  }

  /**
   * Formats a message selector including partner link, operation and correlation properties.
   */
  private static String formatSelector(ReceiveAction receiveAction, Token token) {
    StringBuffer selector = new StringBuffer();

    // partner link id
    selector.append(IntegrationConstants.PARTNER_LINK_ID_PROP).append('=').append(
        receiveAction.getPartnerLink().getId());

    // operation name
    selector.append(" AND ").append(IntegrationConstants.OPERATION_NAME_PROP).append("='").append(
        receiveAction.getOperation().getName()).append('\'');

    // reception properties
    Correlations correlations = receiveAction.getCorrelations();
    // BPEL-90: avoid NPE when the receiver was defined with no correlations
    if (correlations != null) {
      // iterate over the property name-value pairs
      for (Iterator i = correlations.getReceptionProperties(token).entrySet().iterator(); i.hasNext();) {
        Map.Entry propertyEntry = (Map.Entry) i.next();
        QName propertyName = (QName) propertyEntry.getKey();
        // property value
        selector.append(" AND ").append(propertyName.getLocalPart()).append("='").append(
            propertyEntry.getValue()).append('\'');
      }
    }
    return selector.toString();
  }

  long getReceiveActionId() {
    return receiveActionId;
  }

  long getTokenId() {
    return tokenId;
  }

  public MessageConsumer getMessageConsumer() {
    return messageConsumer;
  }

  public void open() throws JMSException {
    /*
     * jms could deliver a message immediately after setting this listener, so make sure this
     * listener is fully initialized at this point
     */
    messageConsumer.setMessageListener(this);
    log.debug("opened request listener: receiveAction=" + receiveActionId + ", token=" + tokenId);
  }

  public void onMessage(Message message) {
    if (!(message instanceof ObjectMessage)) {
      log.error("received non-object message: " + message);
      return;
    }
    try {
      ObjectMessage request = (ObjectMessage) message;
      log.debug("delivering request: " + RequestListener.messageToString(request));
      /*
       * LEAK WARNING. This listener must be removed from the integration control before passing
       * control to the inbound message activity, because loop structures could execute the same
       * activity again and overwrite the entry in the integration control with a new listener. If
       * removeRequestListener() was invoked after passing control to the activity, the new listener
       * would get removed instead of this, leaving the listener open but unreachable from
       * application code. CODE ORDER NOTE. Removing this listener early in the process prevents any
       * other thread from closing it. This effect is desirable because the orderly shutdown
       * mechanism of JMS never stops a running listener anyway. Furthermore, the mechanism is
       * specified to *block* the other thread until this listener returns.
       */
      if (oneShot)
        integrationControl.removeRequestListener(this);

      deliverRequest((Map) request.getObject(), request.getJMSReplyTo(), request.getJMSMessageID());
      request.acknowledge();

      if (oneShot)
        close();
    }
    catch (JMSException e) {
      log.error("request delivery failed due to jms exception, giving up", e);
    }
    catch (RuntimeException e) {
      if (isRecoverable(e)) {
        log.warn("request delivery failed due to recoverable exception, attempting recovery");
        try {
          // recover the session manually
          jmsSession.recover();
        }
        catch (JMSException je) {
          log.error("request recovery failed, giving up", je);
        }
      }
      else
        log.error("request delivery failed due to non-recoverable exception, giving up", e);
    }
  }

  private void deliverRequest(Map parts, Destination replyTo, String requestId) {
    JbpmContext jbpmContext = integrationControl.getIntegrationServiceFactory()
        .getJbpmConfiguration()
        .createJbpmContext();
    try {
      // load receive action via integration session
      ReceiveAction receiveAction = IntegrationSession.getContextInstance(jbpmContext)
          .loadReceiveAction(receiveActionId);

      // load the token and have it saved automatically
      Token token = jbpmContext.loadTokenForUpdate(tokenId);

      try {
        // file outstanding request, in case operation has output
        if (receiveAction.getOperation().getOutput() != null) {
          // encapsulate the fields needed to reply
          OutstandingRequest outRequest = new OutstandingRequest(replyTo, requestId);
          // register the request in the integration control
          integrationControl.addOutstandingRequest(receiveAction, token, outRequest);
        }

        // pass control to inbound message activity
        receiveAction.deliverMessage(token, parts);
      }
      catch (BpelFaultException e) {
        log.debug("request delivery caused a fault", e);
        token.getNode().raiseException(e, new ExecutionContext(token));
      }
    }
    catch (RuntimeException e) {
      jbpmContext.setRollbackOnly();
      throw e;
    }
    finally {
      // end transaction, close all services
      jbpmContext.close();
    }
  }

  public void close() throws JMSException {
    messageConsumer.close();
    log.debug("closed request listener: receiveAction=" + receiveActionId + ", token=" + tokenId);
  }

  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    try {
      QueueReceiver queueReceiver = (QueueReceiver) messageConsumer;
      builder.append("queue", queueReceiver.getQueue()).append("selector",
          messageConsumer.getMessageSelector());
    }
    catch (JMSException e) {
      log.debug("could not fill request listener fields", e);
    }
    return builder.toString();
  }

  public static String messageToString(Message message) throws JMSException {
    StringBuffer result = new StringBuffer();
    // ID & destination
    result.append("id=").append(message.getJMSMessageID()).append(", destination=").append(
        message.getJMSDestination());
    // replyTo & correlationID
    Destination replyTo = message.getJMSReplyTo();
    if (replyTo != null) {
      result.append(", replyTo=").append(replyTo).append(", correlationId=").append(
          message.getJMSCorrelationID());
    }
    // properties
    Enumeration propertyNames = message.getPropertyNames();
    while (propertyNames.hasMoreElements()) {
      String propertyName = (String) propertyNames.nextElement();
      result.append(", ").append(propertyName).append('=').append(
          message.getObjectProperty(propertyName));
    }
    return result.toString();
  }


  public static boolean isRecoverable(RuntimeException exception) {
    for (Throwable throwable = exception; throwable != null; throwable = throwable.getCause()) {
      if (throwable instanceof StaleStateException || throwable instanceof LockAcquisitionException)
        return true;
    }
    return false;
  }

  public static class Key {

    private final long receiveActionId;
    private final long tokenId;

    Key(long receiverId, long tokenId) {
      this.receiveActionId = receiverId;
      this.tokenId = tokenId;
    }

    public long getReceiveActionId() {
      return receiveActionId;
    }

    public long getTokenId() {
      return tokenId;
    }

    public boolean equals(Object other) {
      if (this == other)
        return true;
      if (!(other instanceof Key))
        return false;
      final Key that = (Key) other;
      return receiveActionId == that.receiveActionId && tokenId == that.tokenId;
    }

    public int hashCode() {
      final int prime = 5;
      int result = 863;
      result = prime * result + (int) (receiveActionId ^ (receiveActionId >>> 32));
      result = prime * result + (int) (tokenId ^ (tokenId >>> 32));
      return result;
    }

    public String toString() {
      return new ToStringBuilder(this).append("receiveActionId", receiveActionId).append("tokenId",
          tokenId).toString();
    }
  }
}
