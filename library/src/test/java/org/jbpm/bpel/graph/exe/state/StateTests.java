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
package org.jbpm.bpel.graph.exe.state;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author Juan Cantu
 * @version $Revision$ $Date: 2008/02/04 14:35:48 $
 */
public class StateTests {

  public static Test suite() {
    TestSuite suite = new TestSuite("scope state tests");

    suite.addTestSuite(PerformingPrimaryActivityTest.class);
    suite.addTestSuite(CompletingEventsTest.class);

    suite.addTestSuite(TerminatingPrimaryActivityOnTerminateTest.class);
    suite.addTestSuite(TerminatingWithoutHandlerTest.class);
    suite.addTestSuite(TerminatingWithHandlerTest.class);
    suite.addTestSuite(TerminatingTerminationHandlerTest.class);

    suite.addTestSuite(TerminatingPrimaryActivityOnFaultTest.class);
    suite.addTestSuite(FaultingWithoutHandlerTest.class);
    suite.addTestSuite(FaultingWithHandlerTest.class);
    suite.addTestSuite(TerminatingFaultHandlerTest.class);

    suite.addTestSuite(CompensatingWithoutHandlerTest.class);
    suite.addTestSuite(CompensatingWithHandlerTest.class);
    suite.addTestSuite(TerminatingCompensationHandlerTest.class);

    suite.addTestSuite(CompletedTest.class);

    return suite;
  }

}
