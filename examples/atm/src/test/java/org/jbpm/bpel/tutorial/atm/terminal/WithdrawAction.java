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
package org.jbpm.bpel.tutorial.atm.terminal;

import java.awt.event.ActionEvent;
import java.rmi.RemoteException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.bpel.tutorial.atm.FrontEnd;
import org.jbpm.bpel.tutorial.atm.InsufficientFunds;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/11/29 10:31:47 $
 */
public class WithdrawAction extends AbstractAction {

  private static final long serialVersionUID = 1L;

  private static final Log log = LogFactory.getLog(WithdrawAction.class);

  public WithdrawAction() {
    putValue(NAME, "Withdraw");
  }

  public void actionPerformed(ActionEvent event) {
    Map context = AtmTerminal.getContext();
    AtmPanel atmPanel = (AtmPanel) context.get(AtmTerminal.PANEL);

    // capture amount
    NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
    String amountText = JOptionPane.showInputDialog(atmPanel, "Amount", currencyFormat.format(0.0));
    if (amountText == null)
      return;

    try {
      // parse amount
      double amount = currencyFormat.parse(amountText).doubleValue();

      // withdraw funds from account
      FrontEnd atmFrontEnd = (FrontEnd) context.get(AtmTerminal.FRONT_END);
      String customerName = (String) context.get(AtmTerminal.CUSTOMER);
      double balance = atmFrontEnd.withdraw(customerName, amount);

      // update atm panel
      atmPanel.setMessage("Your new balance is " + currencyFormat.format(balance));
    }
    catch (ParseException e) {
      log.debug("invalid amount", e);
      atmPanel.setMessage("Please enter a valid amount.");
    }
    catch (InsufficientFunds e) {
      log.debug("insufficient funds", e);
      atmPanel.setMessage("I could not fulfill your request.\n"
          + "Your current balance is only "
          + currencyFormat.format(e.getAmount()));
    }
    catch (RemoteException e) {
      log.error("remote operation failure", e);
      atmPanel.setMessage("Communication with the bank failed.\n" + "Please log on again.");
      atmPanel.clearActions();
      atmPanel.addAction(new LogOnAction());
      atmPanel.setStatus("connected");
    }
  }
}
