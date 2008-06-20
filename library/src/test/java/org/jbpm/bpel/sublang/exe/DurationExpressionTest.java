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

import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.w3c.dom.Element;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.bpel.graph.basic.Wait;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.scope.Scope;
import org.jbpm.bpel.sublang.def.Expression;
import org.jbpm.bpel.variable.def.ElementType;
import org.jbpm.bpel.variable.def.SchemaType;
import org.jbpm.bpel.variable.def.VariableDefinition;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.bpel.xml.util.DatatypeUtil;
import org.jbpm.bpel.xml.util.Duration;
import org.jbpm.bpel.xml.util.XmlUtil;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/10/13 02:53:26 $
 */
public class DurationExpressionTest extends TestCase {

  private Token token;

  private JbpmContext jbpmContext;

  private static final String ELEM_TEXT = "<a>"
      + "  <b interval=\"P12Y75M60D\"/>"
      + "  <c>P8Y12M20DT15H45M30.05S</c>"
      + "</a>";

  protected void setUp() throws Exception {
    /*
     * the process definition accesses the jbpm configuration, so create a context before creating a
     * process definition to avoid loading another configuration from the default resource
     */
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.getInstance("org/jbpm/bpel/graph/exe/test.jbpm.cfg.xml");
    jbpmContext = jbpmConfiguration.createJbpmContext();

    // process and global scope
    BpelProcessDefinition pd = new BpelProcessDefinition("pd", BpelConstants.NS_EXAMPLES);
    Scope scope = pd.getGlobalScope();
    // primary activity
    Activity primary = new Wait();
    scope.setActivity(primary);
    // element variable
    Element elem1 = XmlUtil.parseText(ELEM_TEXT);
    VariableDefinition var1 = new VariableDefinition();
    var1.setName("var1");
    var1.setType(new ElementType(new QName(elem1.getNamespaceURI(), elem1.getLocalName())));
    scope.addVariable(var1);
    // simple variable
    VariableDefinition simple = new VariableDefinition();
    simple.setName("simple");
    simple.setType(new SchemaType(new QName(BpelConstants.NS_XML_SCHEMA, "duration")));
    scope.addVariable(simple);
    // instantiate process
    token = new ProcessInstance(pd).getRootToken();
    token.setNode(primary);
    // initialize variables
    scope.createInstance(token).initializeData();
    var1.setValue(token, elem1);
    simple.setValue(token, "PT28H35M140.7S");
  }

  protected void tearDown() throws Exception {
    jbpmContext.close();
  }

  public void testSimpleExtraction() {
    // exprected value
    Duration duration = new Duration(0, 0, 0, 28, 35, 140, 700);
    // assertion
    assertEquals(duration, eval("$simple"));
  }

  public void testAttributeExtraction() {
    assertEquals(new Duration(12, 75, 60, 0, 0, 0, 0), eval("$var1/b/@interval"));
  }

  public void testContentExtraction() {
    assertEquals(new Duration(8, 12, 20, 15, 45, 30, 50), eval("$var1/c"));
  }

  private Duration eval(String text) {
    Expression expr = new Expression();
    expr.setText(text);
    return DatatypeUtil.toDuration(expr.getEvaluator().evaluate(token));
  }
}