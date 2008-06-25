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

import java.io.ByteArrayInputStream;
import java.util.Iterator;
import java.util.List;

import javax.wsdl.Definition;

import org.xml.sax.InputSource;

import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.def.Import;
import org.jbpm.bpel.xml.BpelReader;
import org.jbpm.bpel.xml.DefinitionDescriptorReader;
import org.jbpm.bpel.xml.ProblemCollector;
import org.jbpm.bpel.xml.ProblemHandler;
import org.jbpm.bpel.xml.ProcessArchiveWsdlLocator;
import org.jbpm.bpel.xml.ProcessWsdlLocator;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.jpdl.par.ProcessArchive;
import org.jbpm.jpdl.par.ProcessArchiveParser;
import org.jbpm.jpdl.xml.Problem;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/10/13 02:53:27 $
 */
public class DescriptorArchiveParser implements ProcessArchiveParser {

  public static final String DEFINITION_DESC_NAME = "META-INF/bpel-definition.xml";

  private static final long serialVersionUID = 1L;

  public ProcessDefinition readFromArchive(ProcessArchive archive,
      ProcessDefinition processDefinition) {
    BpelProcessDefinition bpelProcessDefinition = new BpelProcessDefinition();

    // look for definition descriptor
    byte[] descriptorEntry = archive.removeEntry(DEFINITION_DESC_NAME);
    if (descriptorEntry != null) {
      ProblemCollector collector = new ProblemCollector(DEFINITION_DESC_NAME);
      DefinitionDescriptor definitionDescriptor = readDescriptor(descriptorEntry, collector);

      // configure bpel definition
      bpelProcessDefinition.setLocation(definitionDescriptor.getLocation());
      List imports = definitionDescriptor.getImports();
      if (!imports.isEmpty()) {
        // read imported documents
        readDocuments(imports, archive, collector);
        bpelProcessDefinition.getImportDefinition().addImports(imports);
      }

      // move problems from the reader to the archive
      archive.getProblems().addAll(collector.getProblems());
    }
    else {
      // no definition descriptor, look for bpel document
      String bpelEntryName = findBpelEntryName(archive);
      if (bpelEntryName != null)
        bpelProcessDefinition.setLocation(bpelEntryName);
      else
        archive.addProblem(new Problem(Problem.LEVEL_ERROR, "definition descriptor not found"));
    }
    return bpelProcessDefinition;
  }

  protected DefinitionDescriptor readDescriptor(byte[] entry, ProblemHandler problemHandler) {
    DefinitionDescriptorReader reader = DefinitionDescriptorReader.getInstance();
    reader.setProblemHandler(problemHandler);
    try {
      // read definition descriptor
      DefinitionDescriptor definitionDescriptor = new DefinitionDescriptor();
      reader.read(definitionDescriptor, new InputSource(new ByteArrayInputStream(entry)));
      return definitionDescriptor;
    }
    finally {
      // reset error handling behavior
      reader.setProblemHandler(null);
    }
  }

  protected void readDocuments(List imports, ProcessArchive archive, ProblemHandler problemHandler) {
    BpelReader bpelReader = new BpelReader();
    bpelReader.setProblemHandler(problemHandler);

    ProcessArchiveWsdlLocator wsdlLocator;
    wsdlLocator = new ProcessArchiveWsdlLocator(ProcessWsdlLocator.EMPTY_URI, archive);
    wsdlLocator.setProblemHandler(problemHandler);

    for (int i = 0, n = imports.size(); i < n; i++) {
      Import _import = (Import) imports.get(i);
      Import.Type importType = _import.getType();

      if (Import.Type.WSDL.equals(importType)) {
        bpelReader.readImportWsdlDefinition(_import, wsdlLocator);
        Definition def = (Definition) _import.getDocument();

        // import namespace is optional
        String namespace = _import.getNamespace();
        if (namespace == null) {
          // when not present, set it to the definitions target
          _import.setNamespace(def.getTargetNamespace());
        }
        else if (!namespace.equals(def.getTargetNamespace())) {
          // when present, ensure it matches wsdl target namespace
          problemHandler.add(new Problem(Problem.LEVEL_ERROR,
              "import namespace does not match wsdl target namespace"));
        }
      }
    }

    // reset error handling behavior
    bpelReader.setProblemHandler(null);
  }

  protected String findBpelEntryName(ProcessArchive archive) {
    Iterator entryNameIt = archive.getEntries().keySet().iterator();
    while (entryNameIt.hasNext()) {
      String entryName = (String) entryNameIt.next();
      if (entryName.endsWith(".bpel"))
        return entryName;
    }
    return null;
  }
}