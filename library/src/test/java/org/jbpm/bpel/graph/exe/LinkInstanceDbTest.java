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

import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.def.LinkDefinition;
import org.jbpm.bpel.graph.struct.Flow;
import org.jbpm.bpel.persistence.db.AbstractDbTestCase;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/08/08 11:09:17 $
 */
public class LinkInstanceDbTest extends AbstractDbTestCase {

  BpelProcessDefinition processDefinition;
  LinkDefinition link;

  protected void setUp() throws Exception {
    super.setUp();
    processDefinition = new BpelProcessDefinition("definition", BpelConstants.NS_EXAMPLES);
    Flow flow = new Flow("flow");
    link = new LinkDefinition("link");
    flow.addLink(link);
    processDefinition.getGlobalScope().setActivity(flow);
    graphSession.saveProcessDefinition(processDefinition);
  }

  private LinkDefinition getLink() {
    return ((Flow) processDefinition.getGlobalScope().getActivity()).getLink("link");
  }

  public void testDefinition() {
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    link.createInstance(processInstance.getRootToken());

    processInstance = saveAndReload(processInstance);

    link = getLink();
    LinkInstance linkInstance = link.getInstance(processInstance.getRootToken());
    assertEquals("link", linkInstance.getDefinition().getName());
  }

  public void testStatus() {
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    Token token = processInstance.getRootToken();
    link.createInstance(token).setStatus(Boolean.TRUE);

    processInstance = saveAndReload(processInstance);

    LinkInstance linkInstance = getLink().getInstance(processInstance.getRootToken());
    assertEquals(Boolean.TRUE, linkInstance.getStatus());
  }

  public void testTargetToken() {
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    Token token = processInstance.getRootToken();
    link.createInstance(token).setTargetToken(token);

    processInstance = saveAndReload(processInstance);

    LinkInstance linkInstance = getLink().getInstance(processInstance.getRootToken());
    assertEquals(processInstance.getRootToken(), linkInstance.getTargetToken());
  }
}
