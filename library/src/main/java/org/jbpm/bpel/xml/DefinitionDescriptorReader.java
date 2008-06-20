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

import java.io.IOException;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.jbpm.bpel.graph.def.Import;
import org.jbpm.bpel.par.DefinitionDescriptor;
import org.jbpm.bpel.xml.util.XmlUtil;
import org.jbpm.jpdl.xml.Problem;

/**
 * Converts an definition descriptor in XML format to an
 * {@linkplain DefinitionDescriptor object model}.
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/10/13 02:53:27 $
 */
public class DefinitionDescriptorReader {

  private ProblemHandler problemHandler;

  protected DefinitionDescriptorReader() {
  }

  public void read(DefinitionDescriptor definitionDescriptor, InputSource input) {
    // get the thread-local parser
    DocumentBuilder builder = XmlUtil.getDocumentBuilder();

    // install our problem handler as document parser's error handler
    ProblemHandler problemHandler = getProblemHandler();
    builder.setErrorHandler(problemHandler.asSaxErrorHandler());

    try {
      // parse content
      Element definitionElem = builder.parse(input).getDocumentElement();

      // halt on parse errors
      if (problemHandler.getProblemCount() > 0)
        return;

      // process document location
      String location = definitionElem.getAttribute(BpelConstants.ATTR_LOCATION);
      definitionDescriptor.setLocation(location);
      // private imports
      Element importsElem = XmlUtil.getElement(definitionElem, BpelConstants.NS_DEFINITION_DESCRIPTOR,
          BpelConstants.ELEM_IMPORTS);
      if (importsElem != null)
        readImports(importsElem, definitionDescriptor);
    }
    catch (SAXException e) {
      problemHandler.add(new Problem(Problem.LEVEL_ERROR,
          "definition descriptor contains invalid xml", e));
    }
    catch (IOException e) {
      problemHandler.add(new Problem(Problem.LEVEL_ERROR, "definition descriptor is not readable",
          e));
    }
    finally {
      // reset error handling behavior
      builder.setErrorHandler(null);
    }
  }

  public void readImports(Element importsElem, DefinitionDescriptor definitionDescriptor) {
    Iterator importElemIt = XmlUtil.getElements(importsElem, BpelConstants.NS_DEFINITION_DESCRIPTOR);
    while (importElemIt.hasNext()) {
      Object obj = importElemIt.next();
      Element importElem = (Element) obj;
      Import imp = readImport(importElem);
      definitionDescriptor.addImport(imp);
    }
  }

  public Import readImport(Element importElem) {
    Import imp = new Import();
    imp.setLocation(importElem.getAttribute(BpelConstants.ATTR_LOCATION));
    // namespace is optional as of 1.1.Beta2
    imp.setNamespace(XmlUtil.getAttribute(importElem, BpelConstants.ATTR_NAMESPACE));
    // determine type based on element name
    String importName = importElem.getLocalName();
    Import.Type importType = BpelConstants.ELEM_WSDL.equals(importName) ? Import.Type.WSDL
        : Import.Type.XML_SCHEMA;
    imp.setType(importType);
    return imp;
  }

  public void setProblemHandler(ProblemHandler problemHandler) {
    this.problemHandler = problemHandler;
  }

  public ProblemHandler getProblemHandler() {
    if (problemHandler == null)
      problemHandler = new ProblemCounter();

    return problemHandler;
  }

  public static DefinitionDescriptorReader getInstance() {
    return new DefinitionDescriptorReader();
  }
}
