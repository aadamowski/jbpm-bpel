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

import javax.xml.namespace.QName;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.jbpm.bpel.graph.basic.Assign;
import org.jbpm.bpel.graph.basic.AssignOperation;
import org.jbpm.bpel.variable.def.VariableDefinition;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.bpel.xml.util.DatatypeUtil;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/07/26 08:06:48 $
 */
public class AssignExeTest extends AbstractExeTestCase {

  private Assign assign;
  private VariableDefinition simpleVar = new VariableDefinition();
  private VariableDefinition elementVar = new VariableDefinition();
  private Token token;

  static final String ASSIGN_TEXT = "<assign>"
      + " <copy>"
      + "  <from><literal>4</literal></from>"
      + "  <to variable='count'/>"
      + " </copy>"
      + " <copy>"
      + "  <from>2 + 2</from>"
      + "  <to variable='count'/>"
      + " </copy>"
      + " <copy>"
      + "  <from><literal>haha</literal></from>"
      + "  <to variable='laughter' xmlns:ex='"
      + BpelConstants.NS_EXAMPLES
      + "'>"
      + "   <query>ex:laugh[last() + 1]</query>"
      + "  </to>"
      + " </copy>"
      + "</assign>";
  private static final QName XSD_INT = new QName(BpelConstants.NS_XML_SCHEMA, "int");
  private static final QName EX_LAUGHTER = new QName(BpelConstants.NS_EXAMPLES, "laughter");

  protected void setUp() throws Exception {
    super.setUp();

    simpleVar.setName("count");
    simpleVar.setType(pd.getImportDefinition().getSchemaType(XSD_INT));

    elementVar.setName("laughter");
    elementVar.setType(pd.getImportDefinition().getElementType(EX_LAUGHTER));

    scope.addVariable(simpleVar);
    scope.addVariable(elementVar);

    assign = (Assign) readActivity(ASSIGN_TEXT, false);
    plugInner(assign);

    token = prepareInner();
  }

  public void testFromLiteralToVariable() {
    AssignOperation copy = (AssignOperation) assign.getOperations().get(0);
    copy.execute(new ExecutionContext(token));

    assertEquals("4", DatatypeUtil.toString(simpleVar.getValue(token)));
  }

  public void testFromExpressionToVariable() {
    AssignOperation copy = (AssignOperation) assign.getOperations().get(1);
    copy.execute(new ExecutionContext(token));

    assertEquals("4", DatatypeUtil.toString(simpleVar.getValue(token)));
  }

  public void testArrayExpansion() {
    AssignOperation copy = (AssignOperation) assign.getOperations().get(2);
    ExecutionContext exeContext = new ExecutionContext(token);
    Element laughter = (Element) elementVar.getValue(token);
    
    for (int count = 1; count <= 5; count++) {
      copy.execute(exeContext);
      
      NodeList laughList = laughter.getElementsByTagNameNS(BpelConstants.NS_EXAMPLES, "laugh");
      assertEquals(count, laughList.getLength());
    }
  }
}
