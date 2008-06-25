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
package org.jbpm.bpel.integration.jms;

import javax.jms.JMSException;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/11/02 16:46:19 $
 */
public class StartListenerTest extends AbstractListenerTestCase {

  private StartListener startListener;

  private static final int RECEPTION_COUNT = 5;

  protected void tearDown() throws Exception {
    // start listener must be closed explicitly
    closeListener();
    // tear down integration control and db
    super.tearDown();
  }

  public void testMultipleReception() throws Exception {
    openListener();
    // start listeners should process any number of requests
    for (int i = 0; i < RECEPTION_COUNT; i++) {
      // send a request message
      sendRequest();
      // wait until reception is verified
      waitForReception();
    }
  }

  protected void openListener() throws JMSException {
    startListener = new StartListener(processDefinition, receiveAction, integrationControl);
    startListener.open();
  }

  protected void closeListener() throws JMSException {
    startListener.close();
  }
}
