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
package org.jbpm.bpel.tutorial.shipping;

import java.rmi.RemoteException;
import java.text.NumberFormat;
import java.text.ParseException;

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
import org.jbpm.bpel.tutorial.purchase.ProblemInfo;
import org.jbpm.bpel.tutorial.purchase.ShippingInfo;

/**
 * Shipping endpoint bean.
 * @author Jeff DeLong
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/01/18 14:38:19 $
 */
public class ShippingPT_Impl implements ShippingPT, ServiceLifecycle {

  protected Connection jmsConnection;
  protected Destination shippingDestination;

  protected float shippingPrice;

  public static final String CONNECTION_FACTORY_NAME = "jms/ConnectionFactory";
  public static final String SHIPPING_DESTINATION_NAME = "jms/Shipping";
  public static final String SHIPPING_PRICE_PARAM = "ShippingPrice";

  private static final Log log = LogFactory.getLog(ShippingPT_Impl.class);

  /**
   * Returns shipping price sychronously and date asynchronously.
   */
  public ShippingInfo requestShipping(CustomerInfo customerInfo)
      throws ProblemInfo, RemoteException {
    // check the address before accepting the shipping
    String address = customerInfo.getAddress();
    if (address.indexOf("Elm St") != -1) {
      // we do not ship to Elm St, it is a scary place
      throw new ProblemInfo("Shipping unavailable to address: " + address);
    }

    try {
      sendShippingMessage(customerInfo.getCustomerId());
    }
    catch (JMSException e) {
      throw new RemoteException("Internal server failure", e);
    }

    ShippingInfo shippingInfo = new ShippingInfo();
    shippingInfo.setShippingPrice(shippingPrice);

    return shippingInfo;
  }

  protected void sendShippingMessage(String customerId) throws JMSException {
    // create a session
    Session jmsSession = jmsConnection.createSession(false,
        Session.CLIENT_ACKNOWLEDGE);
    try {
      // create the message
      MapMessage message = jmsSession.createMapMessage();
      message.setString("customerId", customerId);

      // send it!
      MessageProducer producer = jmsSession.createProducer(shippingDestination);
      producer.send(message);

      log.debug("Sent shipping message: customerId=" + customerId);
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

    // retrieve servlet context
    ServletEndpointContext endpointContext = (ServletEndpointContext) context;
    ServletContext servletContext = endpointContext.getServletContext();

    // initialize shipping price parameter
    String shippingPriceString = servletContext.getInitParameter(SHIPPING_PRICE_PARAM);
    if (shippingPriceString == null) {
      throw new ServiceException("Required parameter not found: "
          + SHIPPING_PRICE_PARAM);
    }

    NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
    try {
      shippingPrice = currencyFormat.parse(shippingPriceString).floatValue();
      log.debug("Set parameter: "
          + SHIPPING_PRICE_PARAM
          + ", value="
          + shippingPrice);
    }
    catch (ParseException e) {
      throw new ServiceException("Invalid parameter: " + SHIPPING_PRICE_PARAM,
          e);
    }
  }

  protected void initJmsObjects() throws NamingException, JMSException {
    Context initialContext = new InitialContext();
    try {
      Context environmentContext = (Context) initialContext.lookup("java:comp/env");
      shippingDestination = (Destination) environmentContext.lookup(SHIPPING_DESTINATION_NAME);
      log.debug("Retrieved destination: " + SHIPPING_DESTINATION_NAME);

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
    shippingDestination = null;
  }
}
