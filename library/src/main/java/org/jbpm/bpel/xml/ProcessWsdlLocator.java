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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import javax.wsdl.xml.WSDLLocator;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.par.DefinitionDescriptor;
import org.jbpm.bpel.wsdl.xml.WsdlConstants;
import org.jbpm.bpel.xml.util.XmlUtil;
import org.jbpm.jpdl.xml.Problem;

/**
 * Allows the {@linkplain BpelReader BPEL reader} to retrieve descriptions recursively imported by
 * the WSDL documents referenced in the {@linkplain BpelProcessDefinition process definition} or in
 * the {@linkplain DefinitionDescriptor definition descriptor}.
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/10/13 02:53:27 $
 */
public class ProcessWsdlLocator implements WSDLLocator {

  private URI processURI;
  private String baseURI;
  private String latestImportURI;
  private ProblemHandler problemHandler = new ProblemCounter();

  private static Templates wsdlUpgradeTemplates;

  public static final URI EMPTY_URI = URI.create("");

  private static final Log log = LogFactory.getLog(ProcessWsdlLocator.class);

  /**
   * Creates a WSDL locator which retrieves WSDL documents relative to the location of the process
   * document.
   * @param processURI the location of the process document
   */
  public ProcessWsdlLocator(URI processURI) {
    if (processURI == null)
      throw new IllegalArgumentException("process URI cannot be null");

    this.processURI = processURI;
  }

  public URI getProcessURI() {
    return processURI;
  }

  public ProblemHandler getProblemHandler() {
    return problemHandler;
  }

  public void setProblemHandler(ProblemHandler problemHandler) {
    if (problemHandler == null)
      throw new IllegalArgumentException("problem handler cannot be null");

    this.problemHandler = problemHandler;
  }

  public void resolveBaseURI(String baseLocation) {
    try {
      baseURI = processURI.resolve(new URI(baseLocation)).toString();
    }
    catch (URISyntaxException e) {
      log.debug("base location is not a valid URI, setting baseURI to null", e);
      baseURI = null;
    }
  }

  public String getBaseURI() {
    return baseURI;
  }

  public InputSource getBaseInputSource() {
    latestImportURI = baseURI;
    InputSource inputSource = createInputSource(baseURI);
    upgradeWsdlDocumentIfNeeded(inputSource);
    return inputSource;
  }

  public InputSource getImportInputSource(String parentLocation, String importLocation) {
    try {
      // if importLocation is relative, resolve it against parentLocation
      URI importURI = new URI(importLocation);
      if (!importURI.isAbsolute())
        importLocation = new URI(parentLocation).resolve(importURI).toString();

      latestImportURI = importLocation;
      InputSource inputSource = createInputSource(importLocation);
      upgradeWsdlDocumentIfNeeded(inputSource);
      return inputSource;
    }
    catch (URISyntaxException e) {
      log.debug("import location is not a valid URI, returning null source", e);
      return null;
    }
  }

  public String getLatestImportURI() {
    return latestImportURI;
  }

  public void close() {
    // TODO Auto-generated method stub
  }

  protected InputSource createInputSource(String documentLocation) {
    return new InputSource(documentLocation);
  }

  private void upgradeWsdlDocumentIfNeeded(InputSource source) {
    // get the thread-local document parser
    DocumentBuilder documentParser = XmlUtil.getDocumentBuilder();

    // install our problem handler as document parser's error handler
    documentParser.setErrorHandler(problemHandler.asSaxErrorHandler());

    // parse content
    Document document;
    try {
      document = documentParser.parse(source);
      // halt on parse errors
      if (problemHandler.getProblemCount() > 0)
        return;
    }
    catch (IOException e) {
      Problem problem = new Problem(Problem.LEVEL_ERROR, "document is not readable", e);
      problem.setResource(latestImportURI);
      problemHandler.add(problem);
      return;
    }
    catch (SAXException e) {
      Problem problem = new Problem(Problem.LEVEL_ERROR, "document contains invalid xml", e);
      problem.setResource(latestImportURI);
      problemHandler.add(problem);
      return;
    }
    finally {
      // reset error handling behavior
      documentParser.setErrorHandler(null);
    }

    // check whether the wsdl document requires upgrading
    if (hasUpgradableElements(document)) {
      try {
        // create wsdl upgrader
        Transformer wsdlUpgrader = getWsdlUpgradeTemplates().newTransformer();

        // install our problem handler as transformer's error listener
        wsdlUpgrader.setErrorListener(problemHandler.asTraxErrorListener());

        // upgrade into memory stream
        ByteArrayOutputStream resultStream = new ByteArrayOutputStream();
        wsdlUpgrader.transform(new DOMSource(document), new StreamResult(resultStream));

        // replace existing source with upgraded document
        source.setByteStream(new ByteArrayInputStream(resultStream.toByteArray()));

        log.debug("upgraded wsdl document: " + latestImportURI);
      }
      catch (TransformerException e) {
        Problem problem = new Problem(Problem.LEVEL_ERROR, "wsdl upgrade failed", e);
        problem.setResource(latestImportURI);
        problemHandler.add(problem);
      }
    }
    else {
      // if the source is a stream, reset it
      InputStream sourceStream = source.getByteStream();
      if (sourceStream != null) {
        try {
          sourceStream.reset();
        }
        catch (IOException e) {
          log.error("could not reset source stream: " + latestImportURI, e);
        }
      }
    }
  }

  static synchronized Templates getWsdlUpgradeTemplates() throws TransformerException {
    if (wsdlUpgradeTemplates == null)
      wsdlUpgradeTemplates = XmlUtil.createTemplates(ProcessWsdlLocator.class.getResource("wsdl-1-1-converter.xslt"));

    return wsdlUpgradeTemplates;
  }

  private static boolean hasUpgradableElements(Document wsdlDocument) {
    Element definitions = wsdlDocument.getDocumentElement();
    // look for elements in the bpel or partner link type namespaces
    return XmlUtil.getElements(definitions, BpelConstants.NS_BPEL_1_1).hasNext()
        || XmlUtil.getElements(definitions, WsdlConstants.NS_PLNK_1_1).hasNext();
  }
}