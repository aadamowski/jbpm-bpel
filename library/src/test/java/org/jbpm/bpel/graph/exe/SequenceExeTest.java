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

import org.jbpm.graph.exe.Token;

import org.jbpm.bpel.graph.basic.Receive;
import org.jbpm.bpel.graph.struct.Sequence;

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/07/26 00:39:11 $
 */
public class SequenceExeTest extends AbstractExeTestCase {

  private Sequence sequence;
  private Receive receiveA;
  private Receive receiveB;
  private Receive receiveC;

  public void testInitialSequence() throws Exception {
    String xml = "<sequence>"
        + "	<receive name='a' createInstance='yes' partnerLink='aPartner' operation='o'/>"
        + "	<receive name='b' partnerLink='aPartner' operation='o'/>"
        + "	<receive name='c' partnerLink='aPartner' operation='o'/>"
        + "</sequence>";

    sequence = (Sequence) readActivity(xml, true);
    plugInitial(sequence);
    initActivities();
    Token token = executeInitial(receiveA.getReceiveAction());
    assertReceiveAndAdvance(token, receiveB, receiveC);
    assertReceiveAndComplete(token, receiveC);
  }

  public void testNestedSequence() throws Exception {
    String xml = "<sequence>"
        + "	<receive name='a' partnerLink='aPartner' operation='o'/>"
        + "	<receive name='b' partnerLink='aPartner' operation='o'/>"
        + "	<receive name='c' partnerLink='aPartner' operation='o'/>"
        + "</sequence>";

    sequence = (Sequence) readActivity(xml, false);
    plugInner(sequence);
    initActivities();
    Token token = executeInner();
    assertReceiveAndAdvance(token, receiveA, receiveB);
    assertReceiveAndAdvance(token, receiveB, receiveC);
    assertReceiveAndComplete(token, receiveC);
  }

  private void initActivities() {
    receiveA = (Receive) sequence.getNode("a");
    receiveB = (Receive) sequence.getNode("b");
    receiveC = (Receive) sequence.getNode("c");
  }
}
