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

import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.struct.While;

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2008/01/30 08:15:35 $
 */
public class WhileReaderTest extends AbstractReaderTestCase {

  public void testActivity() throws Exception {
    String xml = "<while><condition>$c</condition><empty name='looped'/></while>";
    While _while = (While) readActivity(xml);
    Activity activity = (Activity) _while.getNode("looped");
    assertEquals(_while, activity.getCompositeActivity());
  }

  public void testCondition() throws Exception {
    String xml = "<while><condition>$c</condition><empty /></while>";
    While _while = (While) readActivity(xml);
    assertEquals("$c", _while.getCondition().getText());
  }
}
