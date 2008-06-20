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

import org.jbpm.bpel.graph.basic.Empty;
import org.jbpm.bpel.graph.scope.CompensateScope;
import org.jbpm.bpel.graph.scope.Handler;
import org.jbpm.bpel.graph.scope.Scope;
import org.jbpm.bpel.graph.struct.Sequence;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/03/16 00:04:38 $
 */
public class CompensateScopeReaderTest extends AbstractReaderTestCase {

  public void testTarget() throws Exception {
    // inner scope
    Scope inner = new Scope("inner");
    inner.setActivity(new Empty());

    // wrapping sequence
    Sequence sequence = new Sequence("mainseq");
    sequence.addNode(inner);

    // compensation handler
    Handler handler = new Handler();

    // enclosing scope
    scope.setActivity(sequence);
    scope.setCompensationHandler(handler);

    String xml = "<compensateScope target='inner'/>";
    CompensateScope compensate = (CompensateScope) readActivity(
        parseAsBpelElement(xml), handler);

    assertEquals(inner, compensate.getTarget());
  }
}
