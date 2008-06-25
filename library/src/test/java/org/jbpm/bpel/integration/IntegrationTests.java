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

import org.jbpm.bpel.endpointref.wsa.WsaEndpointReferenceTest;
import org.jbpm.bpel.endpointref.wsdl.WsdlEndpointReferenceTest;
import org.jbpm.bpel.integration.catalog.UrlCatalogTest;
import org.jbpm.bpel.integration.exe.CorrelationSetTest;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/10/13 02:53:27 $
 */
public class IntegrationTests {

  public static Test suite() {
    TestSuite suite = new TestSuite("integration tests");
    suite.addTestSuite(UrlCatalogTest.class);
    suite.addTestSuite(CorrelationSetTest.class);
    suite.addTestSuite(WsaEndpointReferenceTest.class);
    suite.addTestSuite(WsdlEndpointReferenceTest.class);
    return suite;
  }
}
