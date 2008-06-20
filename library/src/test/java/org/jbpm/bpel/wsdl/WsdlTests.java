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
package org.jbpm.bpel.wsdl;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.jbpm.bpel.wsdl.xml.PartnerLinkTypeTest;
import org.jbpm.bpel.wsdl.xml.PropertyAliasTest;
import org.jbpm.bpel.wsdl.xml.PropertyTest;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/06/09 23:36:39 $
 */
public class WsdlTests {

  public static Test suite() {
    TestSuite suite = new TestSuite("wsdl tests");

    // serialization
    suite.addTestSuite(PartnerLinkTypeTest.class);
    suite.addTestSuite(PropertyAliasTest.class);
    suite.addTestSuite(PropertyTest.class);

    return suite;
  }

}
