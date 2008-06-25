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
import java.util.zip.ZipInputStream;

import junit.framework.TestCase;

import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.file.def.FileDefinition;
import org.jbpm.jpdl.par.FileArchiveParser;
import org.jbpm.jpdl.par.ProcessArchive;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/10/13 02:53:28 $
 */
public class FileArchiveParserTest extends TestCase {

  private FileArchiveParser archiveParser = new FileArchiveParser();
  private BpelProcessDefinition processDefinition = new BpelProcessDefinition();

  public void testReadFromArchive() throws Exception {
    ProcessArchive archive = createProcessArchive("archiveSample.zip");
    // read archive
    archiveParser.readFromArchive(archive, processDefinition);
    assertTrue(archive.getProblems().isEmpty());
    // verify files
    FileDefinition fileDefinition = processDefinition.getFileDefinition();
    assertEquals(5, fileDefinition.getBytesMap().size());
    assertTrue(fileDefinition.hasFile("processSample.bpel"));
    assertTrue(fileDefinition.hasFile("wsdl/"));
    assertTrue(fileDefinition.hasFile("wsdl/partnerLinkTypeSample.wsdl"));
    assertTrue(fileDefinition.hasFile("META-INF/"));
    assertTrue(fileDefinition.hasFile("META-INF/bpel-definition.xml"));
  }

  public void testReadFromArchive_1_1() throws Exception {
    ProcessArchive archive = createProcessArchive("archiveSample-1_1.zip");
    // read archive
    archiveParser.readFromArchive(archive, processDefinition);
    assertTrue(archive.getProblems().isEmpty());
    // verify files
    FileDefinition fileDefinition = processDefinition.getFileDefinition();
    assertEquals(6, fileDefinition.getBytesMap().size());
    assertTrue(fileDefinition.hasFile("bpel/"));
    assertTrue(fileDefinition.hasFile("bpel/processSample-1_1.bpel"));
    assertTrue(fileDefinition.hasFile("wsdl/"));
    assertTrue(fileDefinition.hasFile("wsdl/partnerLinkTypeSample-1_1.wsdl"));
    assertTrue(fileDefinition.hasFile("META-INF/"));
    assertTrue(fileDefinition.hasFile("META-INF/bpel-definition.xml"));
  }

  public void testReadFromArchive_masterWsdl() throws Exception {
    ProcessArchive archive = createProcessArchive("archiveSample-masterWsdl.zip");
    // read archive
    archiveParser.readFromArchive(archive, processDefinition);
    assertTrue(archive.getProblems().isEmpty());
    // verify files
    FileDefinition fileDefinition = processDefinition.getFileDefinition();
    assertEquals(5, fileDefinition.getBytesMap().size());
    assertTrue(fileDefinition.hasFile("bpel/"));
    assertTrue(fileDefinition.hasFile("bpel/processSample-1_1.bpel"));
    assertTrue(fileDefinition.hasFile("bpel/processSample-1_1.wsdl"));
    assertTrue(fileDefinition.hasFile("wsdl/"));
    assertTrue(fileDefinition.hasFile("wsdl/partnerLinkTypeSample-1_1.wsdl"));
  }

  private ProcessArchive createProcessArchive(String resourceName) throws Exception {
    InputStream archiveStream = getClass().getResourceAsStream(resourceName);
    try {
      return new ProcessArchive(new ZipInputStream(archiveStream));
    }
    finally {
      archiveStream.close();
    }
  }
}
