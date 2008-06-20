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
package org.jbpm.bpel.graph.exe;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.jbpm.bpel.graph.exe.flow.DPE1Test;
import org.jbpm.bpel.graph.exe.flow.DPE2Test;
import org.jbpm.bpel.graph.exe.flow.DPE3Test;
import org.jbpm.bpel.graph.exe.flow.AcyclicGraphTest;
import org.jbpm.bpel.graph.exe.flow.LinkScopingTest;

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2008/01/30 08:15:34 $
 */
public class GraphExeTests {

  public static Test suite() {
    TestSuite suite = new TestSuite("activity execution tests");

    // basic elements
    suite.addTestSuite(ActivityExeTest.class);
    suite.addTestSuite(StructuredActivityTest.class);
    suite.addTestSuite(LinkInstanceTest.class);

    // bpel composite activities
    suite.addTestSuite(ScopeExeTest.class);
    suite.addTestSuite(SequenceExeTest.class);
    suite.addTestSuite(WhileExeTest.class);
    suite.addTestSuite(WhileScaleTest.class);
    suite.addTestSuite(RepeatUntilExeTest.class);
    suite.addTestSuite(RepeatUntilScaleTest.class);
    suite.addTestSuite(PickExeTest.class);
    suite.addTestSuite(FlowExeTest.class);
    suite.addTestSuite(IfExeTest.class);
    suite.addTestSuite(InitialActivitiesTest.class);

    // graph related
    suite.addTest(DPE1Test.suite());
    suite.addTest(DPE2Test.suite());
    suite.addTest(DPE3Test.suite());
    suite.addTest(AcyclicGraphTest.suite());
    suite.addTest(LinkScopingTest.suite());
    suite.addTestSuite(ControlDependencyTest.class);

    // basic activities
    suite.addTestSuite(AssignExeTest.class);

    return suite;
  }
}
