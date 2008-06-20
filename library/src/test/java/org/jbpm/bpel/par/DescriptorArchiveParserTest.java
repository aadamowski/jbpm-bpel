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
package org.jbpm.bpel.par;

import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipInputStream;

import javax.wsdl.Definition;

import junit.framework.TestCase;

import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.def.Import;
import org.jbpm.jpdl.par.ProcessArchive;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/10/13 02:53:28 $
 */
public class DescriptorArchiveParserTest extends TestCase {

  DescriptorArchiveParser archiveParser = new DescriptorArchiveParser();

  public void testReadFromArchive() throws Exception {
    ProcessArchive archive = createProcessArchive("archiveSample.zip");
    // read archive
    BpelProcessDefinition processDefinition = (BpelProcessDefinition) archiveParser.readFromArchive(
        archive, null);
    assertTrue(archive.getProblems().isEmpty());
    // process location
    assertEquals("processSample.bpel", processDefinition.getLocation());
    // import count
    List imports = processDefinition.getImportDefinition().getImports();
    assertTrue(imports.isEmpty());
  }

  public void testReadFromArchive_1_1() throws Exception {
    ProcessArchive archive = createProcessArchive("archiveSample-1_1.zip");
    // read archive
    BpelProcessDefinition processDefinition = (BpelProcessDefinition) archiveParser.readFromArchive(
        archive, null);
    assertTrue(archive.getProblems().isEmpty());
    // process location
    assertEquals("bpel/processSample-1_1.bpel", processDefinition.getLocation());
    // import count
    List imports = processDefinition.getImportDefinition().getImports();
    assertEquals(1, imports.size());
    // wsdl location
    Import imp = (Import) imports.get(0);
    assertEquals("wsdl/partnerLinkTypeSample-1_1.wsdl", imp.getLocation());
    // wsdl target namespace
    Definition definition = (Definition) imp.getDocument();
    assertEquals("http://manufacturing.org/wsdl/purchase",
        definition.getTargetNamespace());
  }

  public void testReadFromArchive_masterWsdl() throws Exception {
    ProcessArchive archive = createProcessArchive("archiveSample-masterWsdl.zip");
    // read archive
    BpelProcessDefinition processDefinition = (BpelProcessDefinition) archiveParser.readFromArchive(
        archive, null);
    assertTrue(archive.getProblems().isEmpty());
    // process location
    assertEquals("bpel/processSample-1_1.bpel", processDefinition.getLocation());
    // import count
    List imports = processDefinition.getImportDefinition().getImports();
    assertTrue(imports.isEmpty());
  }

  private ProcessArchive createProcessArchive(String resourceName)
      throws Exception {
    InputStream archiveStream = getClass().getResourceAsStream(resourceName);
    ProcessArchive archive = new ProcessArchive(new ZipInputStream(
        archiveStream));
    archiveStream.close();
    return archive;
  }
}
