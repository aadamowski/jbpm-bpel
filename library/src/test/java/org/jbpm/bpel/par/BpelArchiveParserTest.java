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
import org.jbpm.bpel.xml.BpelReader;
import org.jbpm.bpel.xml.ProcessArchiveWsdlLocator;
import org.jbpm.bpel.xml.ProcessWsdlLocator;
import org.jbpm.jpdl.par.ProcessArchive;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/10/13 02:53:28 $
 */
public class BpelArchiveParserTest extends TestCase {

  private BpelArchiveParser archiveParser = new BpelArchiveParser();
  private BpelProcessDefinition processDefinition;

  private JbpmContext jbpmContext;

  protected void setUp() throws Exception {
    /*
     * the process definition accesses the jbpm configuration, so create a context before creating
     * the process definition to avoid loading another configuration from the default resource
     */
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.getInstance("org/jbpm/bpel/graph/exe/test.jbpm.cfg.xml");
    jbpmContext = jbpmConfiguration.createJbpmContext();
    processDefinition = new BpelProcessDefinition();
  }

  protected void tearDown() throws Exception {
    jbpmContext.close();
  }

  public void testReadFromArchive() throws Exception {
    ProcessArchive archive = createProcessArchive("archiveSample.zip");
    // parse process definition
    processDefinition.setLocation("processSample.bpel");
    archiveParser.readFromArchive(archive, processDefinition);
    // problem count
    assertTrue(archive.getProblems().isEmpty());
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
  }

  public void testReadFromArchive_1_1() throws Exception {
    ProcessArchive archive = createProcessArchive("archiveSample-1_1.zip");
    // add wsdl document
    Import _import = new Import();
    _import.setLocation("wsdl/partnerLinkTypeSample-1_1.wsdl");
    _import.setType(Import.Type.WSDL);
    new BpelReader().readImportWsdlDefinition(_import, new ProcessArchiveWsdlLocator(
        ProcessWsdlLocator.EMPTY_URI, archive));
    processDefinition.getImportDefinition().addImport(_import);
    // parse process definition
    processDefinition.setLocation("bpel/processSample-1_1.bpel");
    archiveParser.readFromArchive(archive, processDefinition);
    // problem count
    assertTrue(archive.getProblems().isEmpty());
    // process name
    assertEquals("sampleProcess", processDefinition.getName());
    // import count
    List imports = processDefinition.getImportDefinition().getImports();
    assertEquals(1, imports.size());
    // wsdl location
    _import = (Import) imports.get(0);
    assertEquals("wsdl/partnerLinkTypeSample-1_1.wsdl", _import.getLocation());
    // wsdl target namespace
    Definition definition = (Definition) _import.getDocument();
    assertEquals("http://manufacturing.org/wsdl/purchase", definition.getTargetNamespace());
  }

  public void testReadFromArchive_masterWsdl() throws Exception {
    ProcessArchive archive = createProcessArchive("archiveSample-masterWsdl.zip");
    // parse process definition
    processDefinition.setLocation("bpel/processSample-1_1.bpel");
    archiveParser.readFromArchive(archive, processDefinition);
    // problem count
    assertTrue(archive.getProblems().isEmpty());
    // process name
    assertEquals("sampleProcess", processDefinition.getName());
    // import count
    List imports = processDefinition.getImportDefinition().getImports();
    assertEquals(1, imports.size());
    // wsdl location
    Import _import = (Import) imports.get(0);
    assertEquals("processSample-1_1.wsdl", _import.getLocation());
    // wsdl target namespace
    Definition definition = (Definition) _import.getDocument();
    assertEquals("http://manufacturing.org/wsdl/purchase", definition.getTargetNamespace());
  }

  private ProcessArchive createProcessArchive(String resourceName) throws Exception {
    InputStream archiveStream = getClass().getResourceAsStream(resourceName);
    ProcessArchive archive = new ProcessArchive(new ZipInputStream(archiveStream));
    archiveStream.close();
    return archive;
  }
}
