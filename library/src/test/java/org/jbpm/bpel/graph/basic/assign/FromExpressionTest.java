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
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/10/13 02:53:27 $
 */
public class FromExpressionTest extends TestCase {

  private FromExpression from = new FromExpression();
  private Token token;

  private static final String ELEM1_TEXT = "<a>"
      + "  <b on=\"true\">true</b>"
      + "  <c name=\"venus\"/>"
      + "  <d amount=\"20\"/>"
      + "  <e>30</e>"
      + "</a>";
  private static final String ELEM2_TEXT = "<a>"
      + "  <b on=\"\"/>"
      + "  <c name=\"mars\"/>"
      + "  <d amount=\"30\"/>"
      + "  <e>40</e>"
      + "</a>";

  protected void setUp() throws Exception {
    // process and global scope
    BpelProcessDefinition pd = new BpelProcessDefinition("pd", BpelConstants.NS_EXAMPLES);
    Scope scope = pd.getGlobalScope();
    // primary activity
    Activity primary = new Assign();
    scope.setActivity(primary);
    // element variable 1
    Element elem1 = XmlUtil.parseText(ELEM1_TEXT);
    VariableDefinition var1 = new VariableDefinition();
    var1.setName("var1");
    var1.setType(new ElementType(new QName(elem1.getNamespaceURI(), elem1.getLocalName())));
    scope.addVariable(var1);
    // element variable 2
    Element elem2 = XmlUtil.parseText(ELEM2_TEXT);
    VariableDefinition var2 = new VariableDefinition();
    var2.setName("var2");
    var2.setType(new ElementType(new QName(elem2.getNamespaceURI(), elem2.getLocalName())));
    scope.addVariable(var2);
    // instantiate process
    token = new ProcessInstance(pd).getRootToken();
    token.setNode(primary);
    // initialize global scope
    scope.createInstance(token).initializeData();
    var1.setValue(token, elem1);
    var2.setValue(token, elem2);
  }

  public void testExtract() {
    // expression
    Expression expr = new Expression();
    expr.setText("concat(bpws:getVariableData('var1')/c/@name, 'and', bpws:getVariableData('var2')/c/@name)");
    expr.setNamespaces(Collections.singleton(new Namespace("bpws", BpelConstants.NS_BPEL_1_1)));
    // from
    from.setExpression(expr);
    // verify extraction
    assertEquals("venusandmars", from.extract(token));
  }
}
