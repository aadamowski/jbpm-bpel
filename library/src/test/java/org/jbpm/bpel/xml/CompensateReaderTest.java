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

import org.jbpm.bpel.graph.scope.Compensate;

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/03/22 13:32:55 $
 */
public class CompensateReaderTest extends AbstractReaderTestCase {

  public void testName() throws Exception {
    String xml = "<compensate name='comp' />";
    Compensate compensate = (Compensate) readActivity(xml);
    assertEquals("comp", compensate.getName());
  }
}
