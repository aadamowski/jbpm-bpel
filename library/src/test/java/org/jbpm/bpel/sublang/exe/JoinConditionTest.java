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
package org.jbpm.bpel.sublang.exe;

import java.util.Collections;
import java.util.Set;

import junit.framework.TestCase;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.bpel.graph.basic.Empty;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.def.LinkDefinition;
import org.jbpm.bpel.graph.def.Namespace;
import org.jbpm.bpel.graph.exe.LinkInstance;
import org.jbpm.bpel.sublang.def.JoinCondition;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.bpel.xml.util.DatatypeUtil;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/10/13 02:53:26 $
 */
public class JoinConditionTest extends TestCase {

  private Token token;
  private LinkInstance firstLink;
  private LinkInstance secondLink;
  private LinkInstance thirdLink;
  private Set namespaces;

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
    token = pi.getRootToken();

    Empty activity = new Empty("activity");
    token.setNode(activity);

    activity.addTarget(new LinkDefinition("l1"));
    firstLink = new LinkDefinition("l1").createInstance(token);

    activity.addTarget(new LinkDefinition("l2"));
    secondLink = new LinkDefinition("l2").createInstance(token);

    activity.addTarget(new LinkDefinition("l3"));
    thirdLink = new LinkDefinition("l3").createInstance(token);

    // enclosing element
    namespaces = Collections.singleton(new Namespace("bpws", BpelConstants.NS_BPEL_1_1));
  }

  protected void tearDown() throws Exception {
    jbpmContext.close();
  }

  public void testSimpleEval() {
    firstLink.setStatus(Boolean.TRUE);
    assertTrue(eval("$l1"));
  }

  public void testSimpleFunctionEval() {
    firstLink.setStatus(Boolean.TRUE);
    assertTrue(eval("bpws:getLinkStatus('l1')"));
  }

  public void testOrEval() {
    firstLink.setStatus(Boolean.FALSE);
    secondLink.setStatus(Boolean.TRUE);
    assertTrue(eval("$l1 or $l2"));
  }

  public void testAndEval() {
    firstLink.setStatus(Boolean.FALSE);
    secondLink.setStatus(Boolean.TRUE);
    assertFalse(eval("bpws:getLinkStatus('l2') and $l1"));
  }

  public void testEvalTrue() {
    firstLink.setStatus(Boolean.TRUE);
    secondLink.setStatus(Boolean.FALSE);
    thirdLink.setStatus(Boolean.TRUE);
    assertTrue(eval("($l1 and $l2) or $l3"));
  }

  public void testEvalFalse() {
    firstLink.setStatus(Boolean.TRUE);
    secondLink.setStatus(Boolean.FALSE);
    thirdLink.setStatus(Boolean.FALSE);
    assertFalse(eval("(bpws:getLinkStatus('l3') or $l2) and $l1"));
  }

  private boolean eval(String text) {
    JoinCondition expr = new JoinCondition();
    expr.setText(text);
    expr.setNamespaces(namespaces);
    return DatatypeUtil.toBoolean(expr.getEvaluator().evaluate(token));
  }
}