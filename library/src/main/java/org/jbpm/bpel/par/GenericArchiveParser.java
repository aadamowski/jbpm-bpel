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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import org.jbpm.JbpmConfiguration;
import org.jbpm.bpel.xml.BpelParseException;
import org.jbpm.bpel.xml.util.XmlUtil;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.jpdl.JpdlException;
import org.jbpm.jpdl.par.ConfigurableParser;
import org.jbpm.jpdl.par.ProcessArchive;
import org.jbpm.jpdl.par.ProcessArchiveParser;
import org.jbpm.jpdl.xml.Problem;
import org.jbpm.util.ClassLoaderUtil;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/03/03 11:11:10 $
 */
public class GenericArchiveParser implements ProcessArchiveParser {

  private static final Log log = LogFactory.getLog(GenericArchiveParser.class);
  private static List jpdlArchiveParsers = readProcessArchiveParsers("resource.jpdl.parsers");
  private static List bpelArchiveParsers = readProcessArchiveParsers("resource.bpel.parsers");
  private static final long serialVersionUID = 1L;

  public ProcessDefinition readFromArchive(ProcessArchive archive,
      ProcessDefinition definition) {
    if (archive.getEntry("processdefinition.xml") == null) {
      // bpel deployment
      definition = callProcessArchiveParsers(bpelArchiveParsers, archive,
          definition);

      List problems = archive.getProblems();
      if (Problem.containsProblemsOfLevel(problems, Problem.LEVEL_ERROR))
        throw new BpelParseException(problems);
    }
    else {
      // jpdl deployment
      definition = callProcessArchiveParsers(jpdlArchiveParsers, archive,
          definition);

      List problems = archive.getProblems();
      if (Problem.containsProblemsOfLevel(problems, Problem.LEVEL_ERROR))
        throw new JpdlException(problems);
    }
    return definition;
  }

  public static List readProcessArchiveParsers(String objectName) {
    // get parsers resource setting
    String resource = JbpmConfiguration.Configs.getString(objectName);

    // parse parsers document
    Element parsersElem;
    try {
      parsersElem = XmlUtil.parseResource(resource);
    }
    catch (SAXException e) {
      log.error("parsers document contains invalid xml: " + resource, e);
      return Collections.EMPTY_LIST;
    }
    catch (IOException e) {
      log.error("could not read parsers document: " + resource, e);
      return Collections.EMPTY_LIST;
    }

    // walk through parser elements
    ArrayList processArchiveParsers = new ArrayList();
    Iterator parserElemIt = XmlUtil.getElements(parsersElem, null, "parser");
    while (parserElemIt.hasNext()) {
      Element parserElem = (Element) parserElemIt.next();

      // load parser class
      String parserClassName = parserElem.getAttribute("class");
      Class parserClass = ClassLoaderUtil.loadClass(parserClassName);

      if (!ProcessArchiveParser.class.isAssignableFrom(parserClass)) {
        log.warn("not a process archive parser: " + parserClass);
        continue;
      }

      try {
        // instantiate parser
        ProcessArchiveParser processArchiveParser = (ProcessArchiveParser) parserClass.newInstance();

        // let parser read its settings if it is configurable
        if (processArchiveParser instanceof ConfigurableParser)
          ((ConfigurableParser) processArchiveParser).configure(parserElem);

        // register archive parser
        processArchiveParsers.add(processArchiveParser);
        log.debug("registered archive parser: " + parserClassName);
      }
      catch (InstantiationException e) {
        log.warn("parser class not instantiable: " + parserClassName, e);
      }
      catch (IllegalAccessException e) {
        log.warn("parser class or constructor not public: " + parserClassName,
            e);
      }
    }
    return processArchiveParsers;
  }

  public static ProcessDefinition callProcessArchiveParsers(
      List archiveParsers, ProcessArchive archive, ProcessDefinition definition) {
    Iterator archiveParsersIt = archiveParsers.iterator();
    while (archiveParsersIt.hasNext()) {
      ProcessArchiveParser archiveParser = (ProcessArchiveParser) archiveParsersIt.next();
      definition = archiveParser.readFromArchive(archive, definition);
    }
    return definition;
  }
}
