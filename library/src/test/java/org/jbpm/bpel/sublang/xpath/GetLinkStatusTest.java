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
package org.jbpm.bpel.sublang.xpath;

import junit.framework.TestCase;

import org.jaxen.Context;
import org.jaxen.ContextSupport;
import org.jaxen.SimpleVariableContext;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.def.LinkDefinition;
import org.jbpm.bpel.graph.exe.LinkInstance;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/10/13 02:53:25 $
 */
public class GetLinkStatusTest extends TestCase {

  private Context context;

  private LinkInstance positive;
  private LinkInstance negative;
  private LinkInstance unset;

  private JbpmContext jbpmContext;

  protected void setUp() throws Exception {
    /*
     * the process definition accesses the jbpm configuration, so create a context before creating a
     * process definition to avoid loading another configuration from the default resource
     */
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.getInstance("org/jbpm/bpel/graph/exe/test.jbpm.cfg.xml");
    jbpmContext = jbpmConfiguration.createJbpmContext();

    // process and context
    ProcessDefinition pd = new BpelProcessDefinition("pd", BpelConstants.NS_EXAMPLES);
    ProcessInstance pi = new ProcessInstance(pd);
    Token rootToken = pi.getRootToken();

    positive = new LinkDefinition("positive").createInstance(rootToken);
    positive.setStatus(Boolean.TRUE);

    negative = new LinkDefinition("negative").createInstance(rootToken);
    negative.setStatus(Boolean.FALSE);

    unset = new LinkDefinition("unset").createInstance(rootToken);

    // jaxen context
    ContextSupport sup = new ContextSupport();
    SimpleVariableContext simpleContext = new SimpleVariableContext();

    simpleContext.setVariableValue("positive", positive.getStatus());
    simpleContext.setVariableValue("negative", negative.getStatus());
    simpleContext.setVariableValue("unset", unset.getStatus());

    sup.setVariableContext(simpleContext);
    context = new Context(sup);
  }

  protected void tearDown() throws Exception {
    jbpmContext.close();
  }

  public void testEvaluatePositiveLink() throws Exception {
    assertSame(positive.getStatus(), GetLinkStatusFunction.evaluate("positive", context));
  }

  public void testEvaluateNegativeLink() throws Exception {
    assertSame(negative.getStatus(), GetLinkStatusFunction.evaluate("negative", context));
  }

  public void testEvaluateUnsetLink() throws Exception {
    assertSame(unset.getStatus(), GetLinkStatusFunction.evaluate("unset", context));
  }
}
