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
package org.jbpm.bpel.tools;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/11/06 22:06:25 $
 */
public class ToolTests {

  public static Test suite() {
    TestSuite suite = new TestSuite("Test for org.jbpm.bpel.tools");
    suite.addTest(WsdlServiceToolTest.suite());
    suite.addTestSuite(WscompileToolTest.class);
    suite.addTest(WebServicesDescriptorToolTest.suite());
    suite.addTest(WebAppDescriptorToolTest.suite());
    suite.addTest(WebModuleBuilderTest.suite());
    suite.addTestSuite(ModuleDeployerTest.class);
    return suite;
  }
}
