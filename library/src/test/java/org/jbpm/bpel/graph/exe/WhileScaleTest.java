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

import org.jbpm.bpel.graph.struct.While;
import org.jbpm.bpel.variable.def.SchemaType;
import org.jbpm.bpel.variable.def.VariableDefinition;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.bpel.xml.util.DatatypeUtil;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2008/01/30 08:15:34 $
 */
public class WhileScaleTest extends AbstractExeTestCase {

  private While _while;
  private VariableDefinition counter = new VariableDefinition();

  static final int COUNTER_TOP = 500;

  protected void setUp() throws Exception {
    super.setUp();

    counter.setName("counter");
    counter.setType(new SchemaType(new QName(BpelConstants.NS_XML_SCHEMA, "int")));
    scope.addVariable(counter);

    String xml = "<while name='while'>"
        + " <condition>$counter &lt; "
        + COUNTER_TOP
        + "</condition>"
        + " <assign name='assign'>"
        + "  <copy>"
        + "   <from>$counter + 1</from>"
        + "   <to>$counter</to>"
        + "  </copy>"
        + " </assign>"
        + "</while>";
    _while = (While) readActivity(xml, false);
    plugInner(_while);
  }

  public void testNoBreak() throws Exception {
    Token normalPath = prepareInner();
    counter.setValue(normalPath, new Integer(0));
    firstActivity.leave(new ExecutionContext(normalPath));
    assertEquals(Integer.toString(COUNTER_TOP),
        DatatypeUtil.toString((Element) counter.getValue(normalPath)));
  }
}
