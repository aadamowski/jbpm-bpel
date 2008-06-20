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
package org.jbpm.bpel.integration;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.jbpm.bpel.integration.client.SoapCallerTest;
import org.jbpm.bpel.integration.def.InvokeActionTest;
import org.jbpm.bpel.integration.def.ReplyActionTest;
import org.jbpm.bpel.integration.jms.RequestListenerTest;
import org.jbpm.bpel.integration.jms.StartListenerTest;
import org.jbpm.bpel.integration.server.SoapHandlerTest;
import org.jbpm.bpel.integration.soap.SoapUtilTest;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2008/01/30 07:19:06 $
 */
public class ServerTests {

  public static Test suite() {
    TestSuite suite = new TestSuite("server tests");
    suite.addTestSuite(SoapHandlerTest.class);
    suite.addTestSuite(RequestListenerTest.class);
    suite.addTestSuite(StartListenerTest.class);
    suite.addTestSuite(ReplyActionTest.class);
    suite.addTest(SoapCallerTest.suite());
    suite.addTest(InvokeActionTest.suite());
    suite.addTestSuite(SoapUtilTest.class);
    return suite;
  }
}
