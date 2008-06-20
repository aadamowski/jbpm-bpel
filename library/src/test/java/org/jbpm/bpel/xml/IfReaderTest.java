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
package org.jbpm.bpel.xml;

import java.util.List;

import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.struct.If;

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/07/26 11:19:17 $
 */
public class IfReaderTest extends AbstractReaderTestCase {

  public void testBranches() throws Exception {
    String xml = "<if>"
        + "	<condition>$condition1</condition>"
        + " <empty name='branch1'/>"
        + "	<elseif>"
        + "  <condition>$condition2</condition>"
        + "  <empty name='branch2'/>"
        + " </elseif>"
        + "</if>";
    If _if = (If) readActivity(xml);
    List branches = _if.getBranches();
    assertEquals(2, branches.size());

    Activity branch1 = (Activity) branches.get(0);
    assertEquals("branch1", branch1.getName());
    assertEquals(branch1.getCompositeActivity(), _if);
    assertEquals("$condition1", _if.getCondition(branch1).getText());

    Activity branch2 = (Activity) branches.get(1);
    assertEquals("branch2", branch2.getName());
    assertEquals(branch2.getCompositeActivity(), _if);
    assertEquals("$condition2", _if.getCondition(branch2).getText());
  }

  public void testElse() throws Exception {
    String xml = "<if>"
        + "	<condition>$condition1</condition>"
        + " <empty name='branch1'/>"
        + " <else>"
        + "	 <empty name='o'/>"
        + " </else>"
        + "</if>";
    If _if = (If) readActivity(xml);
    Activity _else = _if.getElse();
    assertEquals("o", _else.getName());
    assertEquals(_else.getCompositeActivity(), _if);
  }

  public void testElseDefault() throws Exception {
    String xml = "<if>"
        + "	<condition>$condition1</condition>"
        + " <empty name='branch1'/>"
        + "</if>";
    If _if = (If) readActivity(xml);
    Activity _else = _if.getElse();
    assertNull(_else);
  }
}