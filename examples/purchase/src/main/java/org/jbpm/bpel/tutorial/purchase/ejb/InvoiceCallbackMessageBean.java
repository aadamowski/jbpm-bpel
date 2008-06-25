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

import javax.ejb.MessageDrivenBean;
import javax.ejb.MessageDrivenContext;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.rpc.ServiceException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.bpel.tutorial.purchase.Invoice;
import org.jbpm.bpel.tutorial.purchase.InvoiceCallbackPT;
import org.jbpm.bpel.tutorial.purchase.PurchaseOrderService;

/**
 * Asynchronous invoice callback bean.
 * @author Jeff DeLong
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/11/29 10:31:47 $
 */
public class InvoiceCallbackMessageBean implements MessageDrivenBean, MessageListener {

  protected MessageDrivenContext messageContext;
  protected InvoiceCallbackPT invoiceRequester;

  private static final Log log = LogFactory.getLog(InvoiceCallbackMessageBean.class);
  private static final long serialVersionUID = 1L;

  /**
   * Process the invoice callback message.
   */
  public void onMessage(Message message) {
    if (!(message instanceof MapMessage)) {
      log.error("received non-map message: " + message);
      messageContext.setRollbackOnly();
      return;
    }
    try {
      // extract contents
      MapMessage invoiceMessage = (MapMessage) message;
      int orderId = invoiceMessage.getInt("orderId");
      float amount = invoiceMessage.getFloat("amount");

      // populate invoice with contents
      Invoice inv = new Invoice();
      inv.setOrderId(orderId);
      inv.setAmount(amount);

      // send invoice back to requester
      invoiceRequester.sendInvoice(inv);

      log.debug("sent invoice: orderId=" + orderId + ", amount=" + amount);
    }
    catch (JMSException e) {
      messageContext.setRollbackOnly();
      log.error("could not read invoice message", e);
    }
    catch (RemoteException e) {
      messageContext.setRollbackOnly();
      log.error("could not send invoice", e);
    }
  }

  public void setMessageDrivenContext(MessageDrivenContext messageContext) {
    this.messageContext = messageContext;
  }

  public void ejbCreate() {
    try {
      PurchaseOrderService service = lookupPurchaseOrderService();
      invoiceRequester = service.getInvoiceRequesterPort();
      log.debug("got invoice callback endpoint: " + invoiceRequester);
    }
    catch (NamingException e) {
      log.error("could not retrieve purchase order service", e);
    }
    catch (ServiceException e) {
      log.error("could not get invoice requester endpoint", e);
    }
  }

  public void ejbRemove() {
    messageContext = null;
    invoiceRequester = null;
  }

  protected PurchaseOrderService lookupPurchaseOrderService() throws NamingException {
    InitialContext initialContext = new InitialContext();
    try {
      return (PurchaseOrderService) initialContext.lookup("java:comp/env/service/PurchaseOrder");
    }
    finally {
      initialContext.close();
    }
  }
}
