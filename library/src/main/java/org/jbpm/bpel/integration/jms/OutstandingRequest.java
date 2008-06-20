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

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.xml.namespace.QName;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2008/01/30 07:18:22 $
 */
public class OutstandingRequest {

  private Destination replyDestination;
  private String correlationID;

  private static final Log log = LogFactory.getLog(OutstandingRequest.class);

  public OutstandingRequest(Destination replyDestination, String correlationID) {
    this.replyDestination = replyDestination;
    this.correlationID = correlationID;
  }

  public Destination getReplyDestination() {
    return replyDestination;
  }

  public String getCorrelationID() {
    return correlationID;
  }

  public void sendReply(Map parts, QName faultName, Session jmsSession) throws JMSException {
    MessageProducer producer = null;
    try {
      producer = jmsSession.createProducer(replyDestination);
      /*
       * the given parts likely are an instance of PersistentMap which does not serialize nicely;
       * copy the parts to a transient Map implementation
       */
      switch (parts.size()) {
      case 0:
        parts = Collections.EMPTY_MAP;
        break;
      case 1: {
        Map.Entry single = (Entry) parts.entrySet().iterator().next();
        parts = Collections.singletonMap(single.getKey(), single.getValue());
        break;
      }
      default:
        parts = new HashMap(parts);
        break;
      }
      Message responseMsg = jmsSession.createObjectMessage((Serializable) parts);
      responseMsg.setJMSCorrelationID(correlationID);
      // set the fault name, if any
      if (faultName != null) {
        responseMsg.setStringProperty(IntegrationConstants.FAULT_NAME_PROP,
            faultName.getLocalPart());
      }
      // send the response
      producer.send(responseMsg);
      log.debug("sent response: " + RequestListener.messageToString(responseMsg));
    }
    finally {
      if (producer != null) {
        try {
          producer.close();
        }
        catch (JMSException e) {
          log.warn("could not close jms producer", e);
        }
      }
    }
  }

  public String toString() {
    return new ToStringBuilder(this).append("replyDestination", replyDestination).append(
        "correlationID", correlationID).toString();
  }

  public static class Key {

    private final long partnerLinkId;
    private final String operationName;
    private final String messageExchange;

    Key(long partnerLinkId, String operation, String messageExchange) {
      if (operation == null)
        throw new IllegalArgumentException("operation cannot be null");

      this.partnerLinkId = partnerLinkId;
      this.operationName = operation;
      this.messageExchange = messageExchange;
    }

    public long getPartnerLinkId() {
      return partnerLinkId;
    }

    public String getOperationName() {
      return operationName;
    }

    public String getMessageExchange() {
      return messageExchange;
    }

    public boolean equals(Object other) {
      if (this == other)
        return true;
      if (!(other instanceof Key))
        return false;
      final Key that = (Key) other;
      return partnerLinkId == that.partnerLinkId
          && operationName.equals(that.operationName)
          && messageExchange != null ? messageExchange.equals(that.messageExchange)
          : that.messageExchange == null;
    }

    public int hashCode() {
      final int prime = 23;
      int result = 239;
      result = prime * result + (int) (partnerLinkId ^ (partnerLinkId >>> 32));
      result = prime * result + operationName.hashCode();
      result = prime * result + (messageExchange == null ? 0 : messageExchange.hashCode());
      return result;
    }

    public String toString() {
      return new ToStringBuilder(this).append("partnerLinkId", partnerLinkId).append(
          "operationName", operationName).append("messageExchange", messageExchange).toString();
    }
  }
}
