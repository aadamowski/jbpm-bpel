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

import junit.framework.TestCase;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.bpel.graph.basic.Empty;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.def.LinkDefinition;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/10/13 02:53:26 $
 */
public class LinkInstanceTest extends TestCase {

  private LinkDefinition link;
  private Activity target;
  private Token token;

  private JbpmContext jbpmContext;

  private static final String linkName = "testLink";

  protected void setUp() {
    /*
     * the process definition accesses the jbpm configuration, so create a context before creating a
     * process definition to avoid loading another configuration from the default resource
     */
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.getInstance("org/jbpm/bpel/graph/exe/test.jbpm.cfg.xml");
    jbpmContext = jbpmConfiguration.createJbpmContext();

    BpelProcessDefinition pd = new BpelProcessDefinition("pd", BpelConstants.NS_EXAMPLES);
    token = new ProcessInstance(pd).getRootToken();
    target = new Empty("target");
    link = new LinkDefinition(linkName);
    link.createInstance(token);
  }

  protected void tearDown() throws Exception {
    jbpmContext.close();
  }

  public void testExecuteTrue() throws Exception {
    target.addTarget(link);
    link.setTransitionCondition(ActivityExeTest.TRUE_EXPRESSION);
    link.determineStatus(token);

    assertEquals(Boolean.TRUE, link.getInstance(token).getStatus());
  }

  public void testExecuteFalse() throws Exception {
    target.addTarget(link);
    link.setTransitionCondition(ActivityExeTest.FALSE_EXPRESSION);
    link.determineStatus(token);

    assertEquals(Boolean.FALSE, link.getInstance(token).getStatus());
  }
}
