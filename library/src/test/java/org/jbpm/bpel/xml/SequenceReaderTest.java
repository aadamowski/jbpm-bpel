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
import org.jbpm.bpel.graph.struct.Sequence;

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/07/26 11:19:17 $
 */
public class SequenceReaderTest extends AbstractReaderTestCase {

  public void testActivities() throws Exception {
    String xml = "<sequence>"
        + "	<empty name='b-s'/>"
        + "	<if name='s-s'>"
        + "  <condition>$condition</condition>"
        + "  <empty/>"
        + " </if>"
        + " <sequence name='s-b'>"
        + "  <empty/>"
        + " </sequence>"
        + " <empty name='b-b'/>"
        + " <empty />"
        + "</sequence>";
    Sequence sequence = (Sequence) readActivity(xml);

    assertEquals(1, sequence.getBegin().getLeavingTransitions().size());
    assertEquals(1, sequence.getEnd().getArrivingTransitions().size());

    List activities = sequence.getNodes();
    assertEquals(5, activities.size());

    Activity n = (Activity) activities.get(0);
    assertEquals(sequence, n.getCompositeActivity());

    for (int i = 1; i < 5; i++) {
      Activity np1 = (Activity) activities.get(i);
      assertEquals(sequence, np1.getCompositeActivity());

      assertTrue(n.isConnected(np1));

      assertEquals(1, n.getLeavingTransitions().size());
      assertEquals(1, np1.getArrivingTransitions().size());

      n = np1;
    }
  }
}
