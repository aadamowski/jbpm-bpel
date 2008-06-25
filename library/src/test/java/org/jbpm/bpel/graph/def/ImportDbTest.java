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

import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.def.Import;
import org.jbpm.bpel.persistence.db.AbstractDbTestCase;
import org.jbpm.bpel.xml.BpelConstants;

public class ImportDbTest extends AbstractDbTestCase {

  private BpelProcessDefinition processDefinition;

  protected void setUp() throws Exception {
    super.setUp();
    /*
     * the process definition accesses the jbpm configuration, so create a context before creating a
     * process definition to avoid opening another context
     */
    processDefinition = new BpelProcessDefinition("pd", BpelConstants.NS_EXAMPLES);
  }

  public void testLocation() {
    Import _import = new Import();
    _import.setLocation("location");
    processDefinition.getImportDefinition().addImport(_import);

    processDefinition = saveAndReload(processDefinition);

    _import = (Import) processDefinition.getImportDefinition().getImports().get(0);
    assertEquals("location", _import.getLocation());
  }

  public void testNamespace() {
    Import _import = new Import();
    _import.setNamespace("http://www.enoughisenough.org");
    processDefinition.getImportDefinition().addImport(_import);

    processDefinition = saveAndReload(processDefinition);

    _import = (Import) processDefinition.getImportDefinition().getImports().get(0);
    assertEquals("http://www.enoughisenough.org", _import.getNamespace());
  }

  public void testWsdlType() {
    Import _import = new Import();
    _import.setType(Import.Type.WSDL);
    processDefinition.getImportDefinition().addImport(_import);

    processDefinition = saveAndReload(processDefinition);

    _import = (Import) processDefinition.getImportDefinition().getImports().get(0);
    assertEquals(Import.Type.WSDL, _import.getType());
  }

  public void testSchemaType() {
    Import _import = new Import();
    _import.setType(Import.Type.XML_SCHEMA);
    processDefinition.getImportDefinition().addImport(_import);

    processDefinition = saveAndReload(processDefinition);

    _import = (Import) processDefinition.getImportDefinition().getImports().get(0);
    assertEquals(Import.Type.XML_SCHEMA, _import.getType());
  }
}