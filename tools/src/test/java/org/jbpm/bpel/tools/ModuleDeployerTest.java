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

import java.net.HttpURLConnection;
import java.net.URL;

import junit.framework.TestCase;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/11/12 00:49:37 $
 */
public class ModuleDeployerTest extends TestCase {

  private ModuleDeployHelper deployHelper = new ModuleDeployHelper();
  private URL remoteUrl;

  private static final String moduleLocation = ModuleDeployerTest.class.getResource("module.war")
      .toExternalForm();

  protected void setUp() throws Exception {
    remoteUrl = new URL("http://localhost:8080/module/text?wsdl");

    // deploy module
    deployHelper.deploy(moduleLocation);
  }

  public void testDeploy() throws Exception {
    // verify web service is available
    HttpURLConnection httpConnection = (HttpURLConnection) remoteUrl.openConnection();
    try {
      assertEquals(HttpURLConnection.HTTP_OK, httpConnection.getResponseCode());
    }
    finally {
      httpConnection.disconnect();
    }

    // undeploy module
    deployHelper.undeploy(moduleLocation);
  }

  public void testUndeploy() throws Exception {
    // undeploy module
    deployHelper.undeploy(moduleLocation);

    // verify web application is unavailable
    HttpURLConnection httpConnection = (HttpURLConnection) remoteUrl.openConnection();
    try {
      assertEquals(HttpURLConnection.HTTP_NOT_FOUND, httpConnection.getResponseCode());
    }
    finally {
      httpConnection.disconnect();
    }
  }
}
