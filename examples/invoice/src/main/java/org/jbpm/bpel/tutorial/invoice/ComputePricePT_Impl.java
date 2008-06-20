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
package org.jbpm.bpel.tutorial.invoice;

import java.rmi.RemoteException;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.xml.rpc.ServiceException;
import javax.xml.rpc.server.ServiceLifecycle;
import javax.xml.rpc.server.ServletEndpointContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.bpel.tutorial.purchase.CustomerInfo;
import org.jbpm.bpel.tutorial.purchase.PurchaseOrder;
import org.jbpm.bpel.tutorial.purchase.ShippingInfo;

/**
 * Invoice endpoint bean.
 * @author Jeff DeLong
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2006/10/29 06:27:49 $
 */
public class ComputePricePT_Impl implements ComputePricePT, ServiceLifecycle {

  protected Destination invoiceDestination;
  protected Connection jmsConnection;

  protected ServletEndpointContext endpointContext;

  public static final String INVOICE_DESTINATION_NAME = "jms/Invoice";
  public static final String CONNECTION_FACTORY_NAME = "jms/ConnectionFactory";

  protected static final String ORDER_ID_ATTR = "invoice.orderId";
  protected static final String LINE_PRICE_ATTR = "invoice.linePrice";

  private static final Log log = LogFactory.getLog(ComputePricePT_Impl.class);

  public void initiatePriceCalculation(CustomerInfo customerInfo,
      PurchaseOrder purchaseOrder) throws RemoteException {
    ServletContext servletContext = endpointContext.getServletContext();
    servletContext.setAttribute(ORDER_ID_ATTR, new Integer(
        purchaseOrder.getOrderId()));
    // In our system the part number is the unit price!
    servletContext.setAttribute(LINE_PRICE_ATTR, new Float(
        purchaseOrder.getQuantity() * purchaseOrder.getPartNumber()));
  }

  public void sendShippingPrice(ShippingInfo shippingInfo)
      throws RemoteException {
    try {
      sendInvoiceMessage(shippingInfo.getShippingPrice());
    }
    catch (JMSException e) {
      throw new RemoteException("Internal server failure", e);
    }
  }

  protected void sendInvoiceMessage(float shippingPrice) throws JMSException {
    ServletContext servletContext = endpointContext.getServletContext();
    Integer orderId = (Integer) servletContext.getAttribute(ORDER_ID_ATTR);
    Float linePrice = (Float) servletContext.getAttribute(LINE_PRICE_ATTR);
    float amount = linePrice.floatValue() + shippingPrice;

    // create a session
    Session jmsSession = jmsConnection.createSession(false,
        Session.CLIENT_ACKNOWLEDGE);
    try {
      // create the message
      MapMessage invoiceMessage = jmsSession.createMapMessage();
      invoiceMessage.setInt("orderId", orderId.intValue());
      invoiceMessage.setFloat("amount", amount);

      // send it!
      MessageProducer producer = jmsSession.createProducer(invoiceDestination);
      producer.send(invoiceMessage);

      log.debug("Sent invoice message: orderId="
          + orderId
          + ", amount="
          + amount);
    }
    finally {
      jmsSession.close();
    }
  }

  public void init(Object context) throws ServiceException {
    // initialize jms administered objects
    try {
      initJmsObjects();
    }
    catch (Exception e) {
      throw new ServiceException("Could not initialize jms objects", e);
    }

    endpointContext = (ServletEndpointContext) context;
  }

  protected void initJmsObjects() throws NamingException, JMSException {
    Context initialContext = new InitialContext();
    try {
      Context environmentContext = (Context) initialContext.lookup("java:comp/env");
      invoiceDestination = (Destination) environmentContext.lookup(INVOICE_DESTINATION_NAME);
      log.debug("Retrieved destination: " + INVOICE_DESTINATION_NAME);

      ConnectionFactory jmsConnectionFactory = (ConnectionFactory) environmentContext.lookup(CONNECTION_FACTORY_NAME);
      jmsConnection = jmsConnectionFactory.createConnection();
      log.debug("Created JMS connection: factory=" + CONNECTION_FACTORY_NAME);
    }
    finally {
      initialContext.close();
    }
  }

  public void destroy() {
    try {
      jmsConnection.close();
    }
    catch (JMSException e) {
      log.debug("could not close jms connection", e);
    }
    jmsConnection = null;
    invoiceDestination = null;
    endpointContext = null;
  }
}
