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

import org.jbpm.bpel.graph.basic.Wait;

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/02/19 18:32:39 $
 */
public class WaitReaderTest extends AbstractReaderTestCase {

  public void testFor() throws Exception {
    String xml = "<wait><for>$f</for></wait>";
    Wait wait = (Wait) readActivity(xml);
    assertEquals("$f", wait.getAlarmAction().getFor().getText());
  }

  public void testUntil() throws Exception {
    String xml = "<wait><until>$u</until></wait>";
    Wait wait = (Wait) readActivity(xml);
    assertEquals("$u", wait.getAlarmAction().getUntil().getText());
  }
}
