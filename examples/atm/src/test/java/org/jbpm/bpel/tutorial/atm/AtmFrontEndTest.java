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
package org.jbpm.bpel.tutorial.atm;

import java.rmi.RemoteException;

import javax.naming.InitialContext;

import junit.framework.Test;
import junit.framework.TestCase;

import org.jbpm.bpel.tools.ModuleDeployTestSetup;

/**
 * Test for common ATM usage scenarios.
 * @author Juan Cantu
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/11/29 10:31:47 $
 */
public class AtmFrontEndTest extends TestCase {

  private FrontEnd frontEnd;

  protected void setUp() throws Exception {
    InitialContext iniCtx = new InitialContext();
    /*
     * "service/ATM" is the JNDI name of the service interface instance relative to the client
     * environment context. This name matches the <service-ref-name> in application-client.xml
     */
    AtmFrontEndService frontEndService = (AtmFrontEndService) iniCtx.lookup("java:comp/env/service/ATM");

    // obtain dynamic proxy for web service port
    frontEnd = frontEndService.getFrontEndPort();
  }

  public void testConnect() throws RemoteException {
    // connect to bank
    int ticketNumber = frontEnd.connect();
    assertTrue(ticketNumber > 0);

    // check atm is connected
    String status = frontEnd.status(ticketNumber);
    assertEquals("connected", status);

    // disconnect from bank
    frontEnd.disconnect(ticketNumber);
  }

  public void testLogOnAuthorized() throws RemoteException {
    // connect to bank
    int ticketNumber = frontEnd.connect();

    // begin customer session
    final String customerName = "koen";
    try {
      frontEnd.logOn(ticketNumber, customerName);
    }
    catch (UnauthorizedAccess e) {
      fail("log on of authorized customer should succeed");
    }

    // end customer session
    frontEnd.logOff(customerName);

    // disconnect from bank
    frontEnd.disconnect(ticketNumber);
  }

  public void testLogOnUnauthorized() throws RemoteException {
    // connect to bank
    int ticketNumber = frontEnd.connect();

    // begin customer session
    final String customerName = "nobody";
    try {
      frontEnd.logOn(ticketNumber, customerName);
      fail("log on of unauthorized customer should fail");
    }
    catch (UnauthorizedAccess e) {
      assertEquals(customerName, e.getCustomerName());
    }

    // disconnect from bank
    frontEnd.disconnect(ticketNumber);
  }

  public void testDeposit() throws RemoteException, UnauthorizedAccess {
    // connect to bank
    int ticketNumber = frontEnd.connect();

    // begin customer session
    final String customerName = "tom";
    frontEnd.logOn(ticketNumber, customerName);

    // get current balance
    double previousBalance = frontEnd.getBalance(customerName);

    // deposit some funds
    double newBalance = frontEnd.deposit(customerName, 10);
    // check the new balance is correct
    assertEquals(previousBalance + 10, newBalance, 0);

    // end customer session
    frontEnd.logOff(customerName);

    // disconnect from bank
    frontEnd.disconnect(ticketNumber);
  }

  public void testWithdrawUnderBalance() throws RemoteException, UnauthorizedAccess {
    // connect to bank
    int ticketNumber = frontEnd.connect();

    // begin customer session
    final String customerName = "tom";
    frontEnd.logOn(ticketNumber, customerName);

    // get current balance
    double previousBalance = frontEnd.getBalance(customerName);

    // withdraw some funds
    try {
      double newBalance = frontEnd.withdraw(customerName, 10);
      // check new balance is correct
      assertEquals(previousBalance - 10, newBalance, 0);
    }
    catch (InsufficientFunds e) {
      fail("withdraw under balance should succeed");
    }

    // end customer session
    frontEnd.logOff(customerName);

    // disconnect from bank
    frontEnd.disconnect(ticketNumber);
  }

  public void testWithdrawOverBalance() throws RemoteException, UnauthorizedAccess {
    // connect to bank
    int ticketNumber = frontEnd.connect();

    // begin customer session
    final String customerName = "fady";
    frontEnd.logOn(ticketNumber, customerName);

    // get current balance
    double previousBalance = frontEnd.getBalance(customerName);

    // try to withdraw an amount greater than current balance
    try {
      frontEnd.withdraw(customerName, previousBalance + 1);
      fail("withdraw over balance should fail");
    }
    catch (InsufficientFunds e) {
      assertEquals(customerName, e.getCustomerName());
      // check account balance has not changed
      assertEquals(previousBalance, e.getAmount(), 0);
    }

    // end customer session
    frontEnd.logOff(customerName);

    // disconnect from bank
    frontEnd.disconnect(ticketNumber);
  }

  public static Test suite() {
    return new ModuleDeployTestSetup(AtmFrontEndTest.class, "atm-client.jar");
  }
}