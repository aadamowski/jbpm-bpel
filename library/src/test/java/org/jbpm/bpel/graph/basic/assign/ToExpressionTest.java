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
package org.jbpm.bpel.graph.basic.assign;

import java.util.Collections;

import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.w3c.dom.Element;

import org.jbpm.bpel.graph.basic.Assign;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.def.Namespace;
import org.jbpm.bpel.graph.scope.Scope;
import org.jbpm.bpel.sublang.def.Expression;
import org.jbpm.bpel.variable.def.ElementType;
import org.jbpm.bpel.variable.def.VariableDefinition;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.bpel.xml.util.XmlUtil;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/10/13 02:53:27 $
 */
public class ToExpressionTest extends TestCase {

  private ToExpression to = new ToExpression();
  private VariableDefinition var1 = new VariableDefinition();
  private Token token;

  public ToExpressionTest(String name) {
    super(name);
  }

  protected void setUp() throws Exception {
    // process and global scope
    BpelProcessDefinition pd = new BpelProcessDefinition("pd", BpelConstants.NS_EXAMPLES);
    Scope scope = pd.getGlobalScope();
    // primary activity
    Activity primary = new Assign();
    scope.setActivity(primary);
    // element variable
    var1.setName("var1");
    var1.setType(new ElementType(new QName("a")));
    scope.addVariable(var1);
    // instantiate process
    token = new ProcessInstance(pd).getRootToken();
    token.setNode(primary);
    // initialize variables
    scope.createInstance(token).initializeData();
  }

  public void testAssign() {
    // query
    Expression query = new Expression();
    query.setText("$var1/c/@name");
    query.setNamespaces(Collections.singleton(new Namespace("bpws", BpelConstants.NS_BPEL_1_1)));
    // to
    to.setExpression(query);
    // instance data
    to.assign(token, "mars");
    // assertions
    assertEquals("mars", XmlUtil.getElement((Element) var1.getValue(token), "c").getAttribute(
        "name"));
  }
}