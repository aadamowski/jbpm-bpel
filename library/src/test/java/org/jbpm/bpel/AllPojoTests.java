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
package org.jbpm.bpel;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.jbpm.bpel.graph.basic.assign.AssignTests;
import org.jbpm.bpel.graph.def.GraphDefTests;
import org.jbpm.bpel.graph.exe.GraphExeTests;
import org.jbpm.bpel.graph.exe.state.StateTests;
import org.jbpm.bpel.integration.IntegrationTests;
import org.jbpm.bpel.sublang.SublangTests;
import org.jbpm.bpel.wsdl.WsdlTests;
import org.jbpm.bpel.xml.XmlTests;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/11/25 13:03:14 $
 */
public class AllPojoTests {

  public static Test suite() {
    TestSuite suite = new TestSuite("all pojo tests");

    suite.addTest(XmlTests.suite());
    suite.addTest(WsdlTests.suite());
    suite.addTest(GraphDefTests.suite());
    suite.addTest(GraphExeTests.suite());
    suite.addTest(StateTests.suite());
    suite.addTest(SublangTests.suite());
    suite.addTest(AssignTests.suite());
    suite.addTest(IntegrationTests.suite());

    return suite;
  }
}
