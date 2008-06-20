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
package org.jbpm.bpel.graph.def;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.jbpm.bpel.graph.scope.ScopeDefTest;
import org.jbpm.bpel.graph.struct.FlowDefTest;
import org.jbpm.bpel.graph.struct.IfDefTest;
import org.jbpm.bpel.graph.struct.PickDefTest;
import org.jbpm.bpel.graph.struct.RepeatUntilDefTest;
import org.jbpm.bpel.graph.struct.SequenceDefTest;
import org.jbpm.bpel.graph.struct.WhileDefTest;

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2008/01/30 08:15:34 $
 */
public class GraphDefTests {

  public static Test suite() {
    TestSuite suite = new TestSuite("graph definition tests");

    suite.addTestSuite(SequenceDefTest.class);
    suite.addTestSuite(WhileDefTest.class);
    suite.addTestSuite(RepeatUntilDefTest.class);
    suite.addTestSuite(PickDefTest.class);
    suite.addTestSuite(FlowDefTest.class);
    suite.addTestSuite(IfDefTest.class);
    suite.addTestSuite(ScopeDefTest.class);

    return suite;
  }
}
