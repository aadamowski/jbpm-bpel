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

import org.jbpm.bpel.graph.def.AbstractActivityDbTestCase;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.scope.CompensateScope;
import org.jbpm.bpel.graph.scope.Handler;
import org.jbpm.bpel.graph.scope.Scope;
import org.jbpm.bpel.xml.BpelConstants;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/10/13 02:53:25 $
 */
public class CompensateScopeDbTest extends AbstractActivityDbTestCase {

  public void testTarget() {
    Scope inner = new Scope("inner");

    CompensateScope compensateScope = createCompensateScope();
    compensateScope.setTarget(inner);

    Handler handler = new Handler();
    handler.setActivity(compensateScope);

    BpelProcessDefinition processDefinition = new BpelProcessDefinition("pd",
        BpelConstants.NS_EXAMPLES);
    Scope global = processDefinition.getGlobalScope();
    global.setCompensationHandler(handler);
    global.setActivity(inner);

    processDefinition = saveAndReload(processDefinition);
    handler = processDefinition.getGlobalScope().getCompensationHandler();
    compensateScope = (CompensateScope) session.load(CompensateScope.class,
        new Long(handler.getActivity().getId()));

    assertEquals("inner", compensateScope.getTarget().getName());
  }

  protected Activity createActivity() {
    return createCompensateScope();
  }

  private CompensateScope createCompensateScope() {
    return new CompensateScope();
  }
}
