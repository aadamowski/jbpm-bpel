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
package org.jbpm.bpel.graph.def;

import org.jbpm.bpel.persistence.db.AbstractDbTestCase;
import org.jbpm.bpel.xml.BpelConstants;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/10/13 02:53:25 $
 */
public class BpelProcessDefinitionDbTest extends AbstractDbTestCase {

  private BpelProcessDefinition processDefinition = new BpelProcessDefinition("pd",
      BpelConstants.NS_EXAMPLES);

  public void testGlobalScope() {
    // cause the global scope to be created
    processDefinition.getGlobalScope();

    processDefinition = saveAndReload(processDefinition);

    assertSame(processDefinition, processDefinition.getGlobalScope().getProcessDefinition());
  }

  public void testImports() {
    ImportDefinition importDefinition = processDefinition.getImportDefinition();

    processDefinition = saveAndReload(processDefinition);
    importDefinition = processDefinition.getImportDefinition();

    assertSame(processDefinition, importDefinition.getProcessDefinition());
  }

  public void testExpressionLanguage() {
    processDefinition.setExpressionLanguage("xpath");

    processDefinition = saveAndReload(processDefinition);

    assertEquals("xpath", processDefinition.getExpressionLanguage());
  }

  public void testQueryLanguage() {
    processDefinition.setQueryLanguage("java");

    processDefinition = saveAndReload(processDefinition);

    assertEquals("java", processDefinition.getQueryLanguage());
  }
}
