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

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.def.Import;
import org.jbpm.file.def.FileDefinition;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.jpdl.par.ProcessArchive;
import org.jbpm.jpdl.xml.Problem;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/07/20 22:35:40 $
 */
public class ProcessArchiveTest extends TestCase {

  private JbpmContext jbpmContext;

  protected void setUp() throws Exception {
    /*
     * the process definition accesses the jbpm configuration, so create a context before creating a
     * process definition to avoid loading another configuration from the default resource
     */
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.getInstance("org/jbpm/bpel/graph/exe/test.jbpm.cfg.xml");
    jbpmContext = jbpmConfiguration.createJbpmContext();
  }

  protected void tearDown() throws Exception {
    jbpmContext.close();
  }

  public void testParseProcessDefinition() throws Exception {
    // parse definition
    ProcessArchive archive = createProcessArchive("archiveSample.zip");
    BpelProcessDefinition processDefinition = (BpelProcessDefinition) archive.parseProcessDefinition();
    // no problems
    assertTrue(archive.getProblems().isEmpty());
    // bpel location
    assertEquals("processSample.bpel", processDefinition.getLocation());
    // process name
    assertEquals("sampleProcess", processDefinition.getName());
    // import count
    List imports = processDefinition.getImportDefinition().getImports();
    assertEquals(1, imports.size());
    // wsdl location
    Import imp = (Import) imports.get(0);
    assertEquals("wsdl/partnerLinkTypeSample.wsdl", imp.getLocation());
    // wsdl target namespace
    Definition definition = (Definition) imp.getDocument();
    assertEquals("http://manufacturing.org/wsdl/purchase", definition.getTargetNamespace());
    // files (count directories too)
    FileDefinition fileDefinition = processDefinition.getFileDefinition();
    assertEquals(4, fileDefinition.getBytesMap().size());
    assertTrue(fileDefinition.hasFile("processSample.bpel"));
    assertTrue(fileDefinition.hasFile("wsdl/"));
    assertTrue(fileDefinition.hasFile("wsdl/partnerLinkTypeSample.wsdl"));
    assertTrue(fileDefinition.hasFile("META-INF/"));
  }

  public void testParseProcessDefinition_1_1() throws Exception {
    // parse definition
    ProcessArchive archive = createProcessArchive("archiveSample-1_1.zip");
    BpelProcessDefinition processDefinition = (BpelProcessDefinition) archive.parseProcessDefinition();
    // no problems
    assertTrue(archive.getProblems().isEmpty());
    // location
    assertEquals("bpel/processSample-1_1.bpel", processDefinition.getLocation());
    // process name
    assertEquals("sampleProcess", processDefinition.getName());
    // import count
    List imports = processDefinition.getImportDefinition().getImports();
    assertEquals(1, imports.size());
    // wsdl location
    Import imp = (Import) imports.get(0);
    assertEquals("wsdl/partnerLinkTypeSample-1_1.wsdl", imp.getLocation());
    // wsdl target namespace
    Definition definition = (Definition) imp.getDocument();
    assertEquals("http://manufacturing.org/wsdl/purchase", definition.getTargetNamespace());
    // files (count directories too)
    FileDefinition fileDefinition = processDefinition.getFileDefinition();
    assertEquals(5, fileDefinition.getBytesMap().size());
    assertTrue(fileDefinition.hasFile("bpel/"));
    assertTrue(fileDefinition.hasFile("bpel/processSample-1_1.bpel"));
    assertTrue(fileDefinition.hasFile("wsdl/"));
    assertTrue(fileDefinition.hasFile("wsdl/partnerLinkTypeSample-1_1.wsdl"));
    assertTrue(fileDefinition.hasFile("META-INF/"));
  }

  public void testParseProcessDefinition_jpdl() throws Exception {
    // parse definition
    ProcessArchive archive = createProcessArchive("archiveSample-jpdl.zip");
    ProcessDefinition processDefinition = archive.parseProcessDefinition();
    // no problems
    assertFalse(Problem.containsProblemsOfLevel(archive.getProblems(), Problem.LEVEL_ERROR));
    // process name
    assertEquals("websale", processDefinition.getName());
    // nodes
    assertEquals(9, processDefinition.getNodes().size());
    // swimlanes
    assertEquals(4, processDefinition.getTaskMgmtDefinition().getSwimlanes().size());
    // files (count directories too)
    assertEquals(15, processDefinition.getFileDefinition().getBytesMap().size());
  }

  public void testParseProcessDefinition_masterWsdl() throws Exception {
    // parse definition
    ProcessArchive archive = createProcessArchive("archiveSample-masterWsdl.zip");
    BpelProcessDefinition processDefinition = (BpelProcessDefinition) archive.parseProcessDefinition();
    // no problems
    assertTrue(archive.getProblems().isEmpty());
    // location
    assertEquals("bpel/processSample-1_1.bpel", processDefinition.getLocation());
    // process name
    assertEquals("sampleProcess", processDefinition.getName());
    // import count
    List imports = processDefinition.getImportDefinition().getImports();
    assertEquals(1, imports.size());
    // wsdl location
    Import imp = (Import) imports.get(0);
    assertEquals("processSample-1_1.wsdl", imp.getLocation());
    // wsdl target namespace
    Definition definition = (Definition) imp.getDocument();
    assertEquals("http://manufacturing.org/wsdl/purchase", definition.getTargetNamespace());
    // files (count directories too)
    FileDefinition fileDefinition = processDefinition.getFileDefinition();
    assertEquals(5, fileDefinition.getBytesMap().size());
    assertTrue(fileDefinition.hasFile("bpel/"));
    assertTrue(fileDefinition.hasFile("bpel/processSample-1_1.bpel"));
    assertTrue(fileDefinition.hasFile("bpel/processSample-1_1.wsdl"));
    assertTrue(fileDefinition.hasFile("wsdl/"));
    assertTrue(fileDefinition.hasFile("wsdl/partnerLinkTypeSample-1_1.wsdl"));
  }

  private static ProcessArchive createProcessArchive(String resourceName) throws Exception {
    InputStream archiveStream = ProcessArchiveTest.class.getResourceAsStream(resourceName);
    try {
      return new ProcessArchive(new ZipInputStream(archiveStream));
    }
    finally {
      archiveStream.close();
    }
  }
}
