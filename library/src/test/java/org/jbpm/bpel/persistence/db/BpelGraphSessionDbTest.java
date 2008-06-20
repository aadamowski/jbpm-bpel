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
package org.jbpm.bpel.persistence.db;

import java.util.List;

import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.xml.BpelConstants;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/10/13 02:53:30 $
 */
public class BpelGraphSessionDbTest extends AbstractDbTestCase {

  private BpelProcessDefinition a1;
  private BpelProcessDefinition a2;
  private BpelProcessDefinition b1;

  protected void setUp() throws Exception {
    super.setUp();

    a1 = new BpelProcessDefinition("a", BpelConstants.NS_EXAMPLES);
    a2 = new BpelProcessDefinition("a", BpelConstants.NS_EXAMPLES);
    b1 = new BpelProcessDefinition("b", BpelConstants.NS_VENDOR);

    bpelGraphSession.deployProcessDefinition(a1);
    bpelGraphSession.deployProcessDefinition(a2);
    bpelGraphSession.deployProcessDefinition(b1);
  }

  public void testFindLatestProcessDefinition() {
    BpelProcessDefinition processDefinition = bpelGraphSession.findLatestProcessDefinition(
        a2.getName(), a2.getTargetNamespace());

    assertEquals(a2.getVersion(), processDefinition.getVersion());
  }

  public void testFindLatestProcessDefinition_anyNamespace() {
    BpelProcessDefinition processDefinition = bpelGraphSession.findLatestProcessDefinition(
        a2.getName(), null);

    assertEquals(a2.getVersion(), processDefinition.getVersion());
  }

  public void testFindLatestProcessDefinitions() {
    List processDefinitions = bpelGraphSession.findLatestProcessDefinitions();

    assertFalse(processDefinitions.contains(a1));
    assertTrue(processDefinitions.contains(a2));
    assertTrue(processDefinitions.contains(b1));
  }

  public void testFindProcessDefinition() {
    BpelProcessDefinition processDefinition = bpelGraphSession.findProcessDefinition(a1.getName(),
        a1.getTargetNamespace(), a1.getVersion());

    assertEquals(a1.getVersion(), processDefinition.getVersion());
  }

  public void testFindProcessDefinition_anyNamespace() {
    BpelProcessDefinition processDefinition = bpelGraphSession.findProcessDefinition(a1.getName(),
        null, a1.getVersion());

    assertEquals(a1.getVersion(), processDefinition.getVersion());
  }

  public void testDeployProcessDefinition() {
    BpelProcessDefinition b2 = new BpelProcessDefinition(b1.getName(), b1.getTargetNamespace());
    bpelGraphSession.deployProcessDefinition(b2);

    assertEquals(b1.getVersion() + 1, b2.getVersion());
  }
}
