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
package org.jbpm.bpel.tutorial.account;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Account system endpoint implementation bean.
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2006/09/27 03:53:01 $
 */
public class AccountSystem_Impl implements AccountSystem {
  
  private static Map accounts = new HashMap();
  
  public boolean checkAccess(String customerName) throws RemoteException {
    return accounts.containsKey(customerName);
  }
  
  public double queryBalance(String customerName) throws RemoteException {
    return getBalance(customerName);
  }
  
  public double updateBalance(AccountOperation body) throws RemoteException {
    String customerName = body.getCustomerName();
    double newBalance = getBalance(customerName) + body.getAmount();
    accounts.put(customerName, new Double(newBalance));
    return newBalance;
  }
  
  private double getBalance(String customerName) {
    Double balance = (Double) accounts.get(customerName);
    return balance.doubleValue();
  }
  
  static {
    try {
      // parse the accounts document
      DocumentBuilder domBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document accountsDocument = domBuilder.parse(AccountSystem_Impl.class.getResource("accounts.xml").toExternalForm());
      // give everyone an initial balance of $50
      Double initialBalance = new Double(50);
      // iterate over the accounts
      Element accountsElem = accountsDocument.getDocumentElement();
      NodeList accountElems = accountsElem.getElementsByTagName("account");
      for (int i = 0, n = accountElems.getLength(); i < n; i++) {
        Element accountElem = (Element) accountElems.item(i);
        // create account, assign initial balance
        String customerName = accountElem.getAttribute("holder");
        accounts.put(customerName, initialBalance);
      }      
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}
