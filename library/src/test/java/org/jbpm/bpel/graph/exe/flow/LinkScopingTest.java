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
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/11/25 13:03:14 $
 */
public class LinkScopingTest extends AbstractExeTestCase {

  private Flow f1;
  private Flow f2;
  private Receive r1;
  private Receive r2;
  private Receive nestedR1;
  private Receive nestedR2;

  private static Element flowElem;

  private void initActivities() {
    f2 = (Flow) f1.getNode("F2");
    r1 = (Receive) f1.getNode("R1");
    r2 = (Receive) f1.getNode("R2");
    nestedR1 = (Receive) f2.getNode("R1");
    nestedR2 = (Receive) f2.getNode("R2");
  }

  public void testNested() throws Exception {
    f1 = (Flow) readActivity(flowElem, false);
    plugInner(f1);
    initActivities();

    Token startToken = executeInner();
    Token r1Token = findToken(startToken, r1);
    Token r2Token = findToken(startToken, r2);
    Token nestedR1Token = findToken(startToken, nestedR1);
    Token nestedR2Token = findToken(startToken, nestedR2);

    assertReceiveDisabled(r2Token, r2);
    assertReceiveDisabled(nestedR2Token, nestedR2);

    // r1 is started, it must move to the end of the flow
    // r2 target link is determined
    assertReceiveAndAdvance(r1Token, r1, f1.getEnd());

    // validate outer L1 link is determined and inner is unset
    assertEquals(Boolean.FALSE, f1.getLink("L1").getInstance(r1Token).getStatus());
    assertEquals(null, f2.getLink("L1").getInstance(nestedR1Token).getStatus());

    // r2 is not executed due to its targets; it is skipped
    assertReceiveDisabled(r2Token, r2);
    assertReceiveDisabled(nestedR2Token, nestedR2);
    assertReceiveAndAdvance(nestedR1Token, nestedR1, f2.getEnd());
    assertReceiveAndComplete(nestedR2Token, nestedR2);
  }

  public void testInitial() throws Exception {
    f1 = (Flow) readActivity(flowElem, true);
    plugInitial(f1);

    initActivities();
    r1.setCreateInstance(true);
    r2.setCreateInstance(true);
    nestedR1.setCreateInstance(true);
    nestedR2.setCreateInstance(true);

    Token startToken = executeInitial(nestedR1.getReceiveAction());
    Token f1LinksToken = startToken.getChild(f1.getName());
    Token f2Token = f1LinksToken.getChild(f2.getName());
    Token f2LinksToken = f2Token.getChild(f2.getName());
    /*
     * nested r1 is started, it must be at the end of the flow; nested r2 target is determined
     */
    Token nestedR1Token = f2LinksToken.getChild(nestedR1.getName());
    assertSame(f2.getEnd(), nestedR1Token.getNode());

    // validate inner L1 link is determined and outer is unset
    assertEquals(Boolean.TRUE, f2.getLink("L1").getInstance(f2LinksToken).getStatus());
    assertEquals(null, f1.getLink("L1").getInstance(f1LinksToken).getStatus());

    Token nestedR2Token = f2LinksToken.getChild(nestedR2.getName());
    // nested r2 receives message and moves to f1 end (f2 flow is completed)
    assertReceiveAndAdvance(nestedR2Token, nestedR2, f2.getEnd());
    assertEquals(f1.getEnd(), f2Token.getNode());

    Token r2Token = f1LinksToken.getChild(r2.getName());
    // r2 receives haven't executed since its link (f1r1 is not yet resolved)
    assertEquals(r2, r2Token.getNode());
    assertReceiveDisabled(r2Token, r2);

    Token r1Token = f1LinksToken.getChild(r1.getName());
    assertReceiveAndComplete(r1Token, r1);
    // r2 was skipped (never executed since r1 resolved this link as negative)
    assertReceiveDisabled(r2Token, r2);
  }

  public static Test suite() {
    return new Setup();
  }

  private static class Setup extends TestSetup {

    private Setup() {
      super(new TestSuite(LinkScopingTest.class));
    }

    protected void setUp() throws Exception {
      flowElem = (Element) XmlUtil.parseResource("linkScoping.bpel", LinkScopingTest.class)
          .getNode();
    }

    protected void tearDown() throws Exception {
      flowElem = null;
    }
  }
}
