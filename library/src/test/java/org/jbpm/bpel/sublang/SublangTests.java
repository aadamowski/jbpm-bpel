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
package org.jbpm.bpel.sublang;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.jbpm.bpel.sublang.exe.BooleanExpressionTest;
import org.jbpm.bpel.sublang.exe.DeadlineExpressionTest;
import org.jbpm.bpel.sublang.exe.DurationExpressionTest;
import org.jbpm.bpel.sublang.exe.GeneralExpressionTest;
import org.jbpm.bpel.sublang.exe.JoinConditionTest;
import org.jbpm.bpel.sublang.exe.VariableAccessTest;
import org.jbpm.bpel.sublang.xpath.GetLinkStatusTest;
import org.jbpm.bpel.sublang.xpath.GetVariableDataTest;
import org.jbpm.bpel.sublang.xpath.GetVariablePropertyTest;
import org.jbpm.bpel.variable.exe.MessageValueTest;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/08/08 11:13:28 $
 */
public class SublangTests {

  public static Test suite() {
    TestSuite suite = new TestSuite("sublanguage tests");
    // $JUnit-BEGIN$
    // sublanguage execution
    suite.addTestSuite(BooleanExpressionTest.class);
    suite.addTestSuite(DeadlineExpressionTest.class);
    suite.addTestSuite(DurationExpressionTest.class);
    suite.addTestSuite(GeneralExpressionTest.class);
    suite.addTestSuite(JoinConditionTest.class);
    suite.addTestSuite(MessageValueTest.class);
    suite.addTestSuite(VariableAccessTest.class);
    // xpath functions
    suite.addTestSuite(GetLinkStatusTest.class);
    suite.addTestSuite(GetVariableDataTest.class);
    suite.addTestSuite(GetVariablePropertyTest.class);
    // $JUnit-END$
    return suite;
  }

}
