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
package org.jbpm.bpel.tutorial.purchase.ejb;

import java.rmi.RemoteException;
import java.util.Calendar;

import javax.ejb.MessageDrivenBean;
import javax.ejb.MessageDrivenContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.rpc.ServiceException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.bpel.tutorial.purchase.PurchaseOrderService;
import org.jbpm.bpel.tutorial.purchase.ScheduleInfo;
import org.jbpm.bpel.tutorial.purchase.ShippingCallbackPT;

/**
 * Asynchronous shipping callback bean.
 * @author Jeff DeLong
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/11/29 10:31:47 $
 */
public class ShippingCallbackMessageBean implements MessageDrivenBean, MessageListener {

  protected MessageDrivenContext messageContext;
  protected ShippingCallbackPT shippingRequester;

  private static final Log log = LogFactory.getLog(ShippingCallbackMessageBean.class);
  private static final long serialVersionUID = 1L;

  /**
   * Process the shipping message.
   */
  public void onMessage(Message msg) {
    try {
      // populate schedule info; for now the message contents are irrelevant
      ScheduleInfo schedule = new ScheduleInfo();
      Calendar shipDate = Calendar.getInstance();
      schedule.setShipDate(shipDate);
      
      // send schedule back to requester
      shippingRequester.sendSchedule(schedule);
      log.debug("sent schedule: shipDate=" + shipDate);
    }
    catch (RemoteException e) {
      messageContext.setRollbackOnly();
      log.error("could not send schedule", e);
    }
  }

  public void setMessageDrivenContext(MessageDrivenContext messageContext) {
    this.messageContext = messageContext;
  }

  public void ejbCreate() {
    try {
      PurchaseOrderService service = lookupPurchaseService();
      shippingRequester = service.getShippingRequesterPort();
    }
    catch (NamingException e) {
      log.error("could not retrieve purchase order service", e);
    }
    catch (ServiceException e) {
      log.error("could not get shipping requester endpoint", e);
    }
  }

  public void ejbRemove() {
    messageContext = null;
    shippingRequester = null;
  }

  protected PurchaseOrderService lookupPurchaseService() throws NamingException {
    Context initialContext = new InitialContext();
    try {
      return (PurchaseOrderService) initialContext.lookup("java:comp/env/service/PurchaseOrder");
    }
    finally {
      initialContext.close();
    }
  }
}
