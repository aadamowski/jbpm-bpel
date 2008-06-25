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
package org.jbpm.bpel.tutorial.scheduling;

import java.rmi.Remote;
import java.rmi.RemoteException;

import org.jbpm.bpel.tutorial.purchase.CustomerInfo;
import org.jbpm.bpel.tutorial.purchase.PurchaseOrder;
import org.jbpm.bpel.tutorial.purchase.ScheduleInfo;

/**
 * Production scheduling endpoint bean.
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2006/10/29 06:27:49 $
 */
public class SchedulingPT_Impl implements SchedulingPT, Remote {

  public void requestProductionScheduling(CustomerInfo customerInfo,
      PurchaseOrder purchaseOrder) throws RemoteException {
    // TODO Auto-generated method stub
  }

  public void sendShippingSchedule(ScheduleInfo schedule)
      throws RemoteException {
    // TODO Auto-generated method stub
  }
}
