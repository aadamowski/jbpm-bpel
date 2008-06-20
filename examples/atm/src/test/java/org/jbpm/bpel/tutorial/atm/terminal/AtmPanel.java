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

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/11/29 10:31:47 $
 */
public class AtmPanel extends JPanel {

  private JTextArea messageTextArea = new JTextArea();
  private JLabel statusLabel = new JLabel();
  private JPanel actionsPanel = new JPanel();

  private static final long serialVersionUID = 1L;

  public AtmPanel() {
    // prevent edition of message area
    messageTextArea.setEditable(false);
    messageTextArea.setColumns(40);
    // set layout of actions panel
    actionsPanel.setLayout(new GridLayout(4, 1, 5, 5));
    // add components to panel
    setLayout(new BorderLayout());
    add(statusLabel, BorderLayout.SOUTH);
    add(messageTextArea, BorderLayout.CENTER);
    add(actionsPanel, BorderLayout.EAST);
  }

  public String getMessage() {
    return messageTextArea.getText();
  }

  public void setMessage(String message) {
    messageTextArea.setText(message);
  }

  public String getStatus() {
    return statusLabel.getText();
  }

  public void setStatus(String status) {
    statusLabel.setText(status);
  }

  public void addAction(Action action) {
    JButton actionButton = new JButton(action);
    actionsPanel.add(actionButton);
  }

  public void clearActions() {
    actionsPanel.removeAll();
  }
}
