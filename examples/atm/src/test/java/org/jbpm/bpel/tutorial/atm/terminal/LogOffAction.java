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
import java.util.Map;

import javax.swing.AbstractAction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.bpel.tutorial.atm.FrontEnd;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/11/29 10:31:47 $
 */
public class LogOffAction extends AbstractAction {

  private static final long serialVersionUID = 1L;

  private static final Log log = LogFactory.getLog(LogOffAction.class);

  public LogOffAction() {
    putValue(NAME, "Log Off");
  }

  public void actionPerformed(ActionEvent event) {
    Map context = AtmTerminal.getContext();
    AtmPanel atmPanel = (AtmPanel) context.get(AtmTerminal.PANEL);
    FrontEnd atmFrontEnd = (FrontEnd) context.get(AtmTerminal.FRONT_END);

    try {
      // log off customer
      String customerName = (String) context.get(AtmTerminal.CUSTOMER);
      atmFrontEnd.logOff(customerName);
    }
    catch (RemoteException e) {
      log.error("remote operation failure", e);
    }

    // update atm panel
    atmPanel.setMessage("Welcome!\nPlease log on, so we can begin");
    atmPanel.clearActions();
    atmPanel.addAction(new LogOnAction());
    atmPanel.setStatus("connected");
  }
}
