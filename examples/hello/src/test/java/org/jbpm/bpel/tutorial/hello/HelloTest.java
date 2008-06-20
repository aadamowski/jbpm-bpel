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
package org.jbpm.bpel.tutorial.hello;

import javax.naming.InitialContext;
import javax.xml.namespace.QName;
import javax.xml.rpc.Call;

import junit.framework.Test;
import junit.framework.TestCase;

import org.jbpm.bpel.tools.ModuleDeployTestSetup;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/11/06 22:06:25 $
 */
public class HelloTest extends TestCase {

  private HelloWorldService helloService;

  protected void setUp() throws Exception {
    InitialContext iniCtx = new InitialContext();
    /*
     * "service/Hello" is the JNDI name of the service interface instance relative to the client
     * environment context. This name matches the <service-ref-name> in application-client.xml
     */
    helloService = (HelloWorldService) iniCtx.lookup("java:comp/env/service/Hello");
  }

  public void testSayHello_proxy() throws Exception {
    // obtain dynamic proxy for web service port
    Greeter proxy = helloService.getGreeterPort();
    // use proxy as local java object
    String greeting = proxy.sayHello("Popeye");
    // check proper greeting
    assertEquals("Hello, Popeye!", greeting);
  }

  public void testSayHello_dii() throws Exception {
    String portTypeNS = "http://jbpm.org/examples/hello";
    // obtain dynamic invocation instance
    Call call = helloService.createCall(new QName(portTypeNS, "GreeterPort"), "sayHello");
    // invoke operation using request/response interaction mode
    String greeting = (String) call.invoke(new Object[] { "Olive" });
    // check proper greeting
    assertEquals("Hello, Olive!", greeting);
  }

  public static Test suite() {
    return new ModuleDeployTestSetup(HelloTest.class, "hello-client.jar");
  }
}