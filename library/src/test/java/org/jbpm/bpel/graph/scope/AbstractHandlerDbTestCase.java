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
package org.jbpm.bpel.graph.scope;

import org.jbpm.bpel.graph.basic.Empty;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.persistence.db.AbstractDbTestCase;
import org.jbpm.bpel.xml.BpelConstants;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/08/08 11:10:06 $
 */
public abstract class AbstractHandlerDbTestCase extends AbstractDbTestCase {

  protected Handler handler;

  protected void setUp() throws Exception {
    // prepare db stuff
    super.setUp();
    // process, create after opening the jbpm context
    handler = createHandler(new BpelProcessDefinition("process", BpelConstants.NS_EXAMPLES));
  }

  public void testActivity() {
    // prepare persistent objects
    // activity
    Empty activity = new Empty("empty");
    // handler
    handler.setActivity(activity);

    // save objects and load them back
    BpelProcessDefinition process = saveAndReload(handler.getBpelProcessDefinition());
    handler = getHandler(process);

    // verify the retrieved objects
    assertEquals("empty", handler.getActivity().getName());
  }

  public void testEnclosingScope() {
    // save objects and load them back
    BpelProcessDefinition process = saveAndReload(handler.getBpelProcessDefinition());
    handler = getHandler(process);

    // verify the retrieved objects
    assertSame(process.getGlobalScope(), handler.getCompositeActivity());
  }

  protected abstract Handler createHandler(BpelProcessDefinition process);

  protected abstract Handler getHandler(BpelProcessDefinition process);

}