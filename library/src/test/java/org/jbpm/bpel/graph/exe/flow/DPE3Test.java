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
package org.jbpm.bpel.graph.exe.flow;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.w3c.dom.Element;

import org.jbpm.bpel.graph.basic.Receive;
import org.jbpm.bpel.graph.exe.AbstractExeTestCase;
import org.jbpm.bpel.graph.struct.Flow;
import org.jbpm.bpel.xml.util.XmlUtil;
import org.jbpm.graph.exe.Token;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/11/25 13:03:14 $
 */
public class DPE3Test extends AbstractExeTestCase {

  private Flow flow;
  private Receive a;
  private Flow b;
  private Receive x;
  private Receive c;

  private static Element flowElem;

  protected void setUp() throws Exception {
    super.setUp();
    flow = (Flow) readActivity(flowElem, true);
    flow.setSuppressJoinFailure(Boolean.TRUE);
    plugInitial(flow);
    a = (Receive) flow.getNode("A");
    b = (Flow) flow.getNode("B");
    x = (Receive) b.getNode("X");
    c = (Receive) flow.getNode("C");
  }

  public void testEliminateNestedFlow() {
    Token startToken = executeInitial(a.getReceiveAction());
    Token tokenB = findToken(startToken, flow.getEnd());
    Token tokenC = findToken(startToken, c);

    // activity b is eliminated
    assertReceiveDisabled(tokenB, x);
    // activity c receives message and completes
    assertReceiveAndComplete(tokenC, c);
  }

  public static Test suite() {
    return new Setup();
  }

  private static class Setup extends TestSetup {

    private Setup() {
      super(new TestSuite(DPE3Test.class));
    }

    protected void setUp() throws Exception {
      flowElem = (Element) XmlUtil.parseResource("dpe3.xml", DPE3Test.class).getNode();
    }

    protected void tearDown() throws Exception {
      flowElem = null;
    }
  }
}
