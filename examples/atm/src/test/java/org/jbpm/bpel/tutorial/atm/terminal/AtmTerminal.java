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

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.xml.rpc.ServiceException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.bpel.tools.DeploymentException;
import org.jbpm.bpel.tools.ModuleDeployHelper;
import org.jbpm.bpel.tutorial.atm.AtmFrontEndService;
import org.jbpm.bpel.tutorial.atm.FrontEnd;

public class AtmTerminal {

  public static final String PANEL = "panel";
  public static final String FRONT_END = "frontEnd";
  public static final String TICKET = "ticket";
  public static final String CUSTOMER = "customer";

  private static Map context = new HashMap();

  private static Log log = LogFactory.getLog(AtmTerminal.class);

  public static Map getContext() {
    return context;
  }

  public static void main(String[] args) {
    deployClient();
    FrontEnd atmFrontEnd = createAtmFrontEnd();

    selectNativeLookAndFeel();
    AtmPanel atmPanel = createAtmPanel(atmFrontEnd);

    JFrame mainFrame = new JFrame("ATM");
    mainFrame.addWindowListener(AtmFrameListener.INSTANCE);
    mainFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    mainFrame.getContentPane().add(atmPanel);
    mainFrame.pack();
    mainFrame.setVisible(true);
  }

  private static void deployClient() {
    try {
      ModuleDeployHelper deployer = new ModuleDeployHelper();
      deployer.deploy("atm-client.jar");
    }
    catch (DeploymentException e) {
      log.error("could not deploy client module", e);
    }
  }

  private static FrontEnd createAtmFrontEnd() {
    try {
      InitialContext jndiContext = new InitialContext();
      AtmFrontEndService service = (AtmFrontEndService) jndiContext.lookup("java:comp/env/service/ATM");
      jndiContext.close();
  
      return service.getFrontEndPort();
    }
    catch (NamingException e) {
      log.error("could not retrieve service instance", e);
      return null;
    }
    catch (ServiceException e) {
      log.error("could not get port proxy", e);
      return null;
    }
  }

  private static void selectNativeLookAndFeel() {
    try {
      // set native system look and feel
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }
    catch (ClassNotFoundException e) {
      log.debug("system look and feel missing", e);
    }
    catch (InstantiationException e) {
      // should not happen
      throw new AssertionError(e);
    }
    catch (IllegalAccessException e) {
      // should not happen
      throw new AssertionError(e);
    }
    catch (UnsupportedLookAndFeelException e) {
      // should not happen
      throw new AssertionError(e);
    }
  }

  private static AtmPanel createAtmPanel(FrontEnd atmFrontEnd) {
    AtmPanel atmPanel = new AtmPanel();
    context.put(PANEL, atmPanel);

    if (atmFrontEnd != null) {
      context.put(FRONT_END, atmFrontEnd);

      try {
        int ticketNo = atmFrontEnd.connect();
        context.put(TICKET, new Integer(ticketNo));

        atmPanel.setMessage("Welcome!\nPlease log on, so we can begin");
        atmPanel.addAction(new LogOnAction());
        atmPanel.setStatus(atmFrontEnd.status(ticketNo));
      }
      catch (RemoteException e) {
        atmPanel.setMessage("Communication with the bank failed.\n"
            + "Please use another terminal.");
        atmPanel.setStatus("unavailable");
      }
    }
    else {
      atmPanel.setMessage("Bootstrap procedure failed.\n" + "Please use another terminal");
      atmPanel.setStatus("unavailable");
    }
    return atmPanel;
  }

  private static class AtmFrameListener extends WindowAdapter {

    static final WindowListener INSTANCE = new AtmFrameListener();

    private AtmFrameListener() {
    }

    public void windowClosing(WindowEvent event) {
      FrontEnd atmFrontEnd = (FrontEnd) context.get(FRONT_END);
      Integer ticketNo = (Integer) context.get(TICKET);

      if (atmFrontEnd != null && ticketNo != null) {
        try {
          atmFrontEnd.disconnect(ticketNo.intValue());
        }
        catch (RemoteException e) {
          log.error("remote disconnect failure", e);
        }
      }

      undeployClient();
    }
  }

  private static void undeployClient() {
    try {
      ModuleDeployHelper deployer = new ModuleDeployHelper();
      deployer.undeploy("atm-client.jar");
    }
    catch (DeploymentException e) {
      log.error("could not undeploy client module", e);
    }
  }
}
