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
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.wsdl.Definition;
import javax.wsdl.Message;
import javax.wsdl.Operation;
import javax.wsdl.OperationType;
import javax.wsdl.PortType;
import javax.wsdl.WSDLException;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.iterators.FilterIterator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.jbpm.JbpmConfiguration;
import org.jbpm.bpel.BpelException;
import org.jbpm.bpel.alarm.AlarmAction;
import org.jbpm.bpel.graph.basic.Empty;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.def.CompositeActivity;
import org.jbpm.bpel.graph.def.Import;
import org.jbpm.bpel.graph.def.ImportDefinition;
import org.jbpm.bpel.graph.scope.Catch;
import org.jbpm.bpel.graph.scope.Handler;
import org.jbpm.bpel.graph.scope.OnAlarm;
import org.jbpm.bpel.graph.scope.OnEvent;
import org.jbpm.bpel.graph.scope.Scope;
import org.jbpm.bpel.integration.def.Correlation;
import org.jbpm.bpel.integration.def.CorrelationSetDefinition;
import org.jbpm.bpel.integration.def.Correlations;
import org.jbpm.bpel.integration.def.PartnerLinkDefinition;
import org.jbpm.bpel.integration.def.ReceiveAction;
import org.jbpm.bpel.integration.def.Correlation.Initiate;
import org.jbpm.bpel.sublang.def.Expression;
import org.jbpm.bpel.sublang.def.PropertyQuery;
import org.jbpm.bpel.variable.def.ElementType;
import org.jbpm.bpel.variable.def.MessageType;
import org.jbpm.bpel.variable.def.SchemaType;
import org.jbpm.bpel.variable.def.VariableDefinition;
import org.jbpm.bpel.variable.def.VariableType;
import org.jbpm.bpel.wsdl.PartnerLinkType;
import org.jbpm.bpel.wsdl.Property;
import org.jbpm.bpel.wsdl.PropertyAlias;
import org.jbpm.bpel.wsdl.PartnerLinkType.Role;
import org.jbpm.bpel.wsdl.xml.WsdlConstants;
import org.jbpm.bpel.wsdl.xml.WsdlUtil;
import org.jbpm.bpel.xml.util.DatatypeUtil;
import org.jbpm.bpel.xml.util.NodeIterator;
import org.jbpm.bpel.xml.util.XmlUtil;
import org.jbpm.jpdl.par.ProcessArchive;
import org.jbpm.jpdl.xml.Problem;
import org.jbpm.util.ClassLoaderUtil;

/**
 * Converts a process document in XML format to a
 * {@linkplain BpelProcessDefinition process definition}.
 * @author Juan Cantú
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/11/29 10:16:30 $
 */
public class BpelReader {

  private Map activityReaders = createActivityReaders();
  private ProblemHandler problemHandler = new ProblemCounter();

  public static final String RESOURCE_ACTIVITY_READERS = "resource.activity.readers";

  private static final Log log = LogFactory.getLog(BpelReader.class);
  private static final Map activityReaderClasses = readActivityReaderClasses();
  private static Templates bpelUpgradeTemplates;

  /**
   * Reads a BPEL document into a process definition.
   * @param processDefinition the definition to read into
   * @param source an input source pointing to the BPEL document
   */
  public void read(BpelProcessDefinition processDefinition, InputSource source) {
    DocumentBuilder documentBuilder = XmlUtil.getDocumentBuilder();

    // capture parse errors in our problem handler
    documentBuilder.setErrorHandler(problemHandler.asSaxErrorHandler());

    // save the document location
    String location = source.getSystemId();
    processDefinition.setLocation(location);
    try {
      // parse content
      Document document = documentBuilder.parse(source);

      // halt on parse errors
      if (problemHandler.getProblemCount() > 0)
        return;

      // prepare a locator of imported documents, relative to the process document location
      ProcessWsdlLocator wsdlLocator = null;
      if (location != null) {
        try {
          wsdlLocator = new ProcessWsdlLocator(new URI(location));
          wsdlLocator.setProblemHandler(problemHandler);
        }
        catch (URISyntaxException e) {
          problemHandler.add(new Problem(Problem.LEVEL_ERROR,
              "source system identifier is invalid: " + location, e));
        }
      }

      // read process definition
      read(processDefinition, document.getDocumentElement(), wsdlLocator);
    }
    catch (SAXException e) {
      problemHandler.add(new Problem(Problem.LEVEL_ERROR, "bpel document contains invalid xml: "
          + location, e));
    }
    catch (IOException e) {
      problemHandler.add(new Problem(Problem.LEVEL_ERROR, "bpel document is not readable: "
          + location, e));
    }
    finally {
      // reset error handling behavior
      documentBuilder.setErrorHandler(null);
    }
  }

  /**
   * Reads a process archive containing a BPEL document into a process definition.
   * @param processDefinition the definition to read into; its <code>location</code> property
   * specifies the process document entry within the archive
   * @param archive the archive to read
   */
  public void read(BpelProcessDefinition processDefinition, ProcessArchive archive) {
    String location = processDefinition.getLocation();
    byte[] bpelEntry = archive.getEntry(location);

    // check whether the bpel document exists in the archive
    if (bpelEntry == null) {
      problemHandler.add(new Problem(Problem.LEVEL_ERROR, "process document entry not found"));
      return;
    }

    DocumentBuilder documentBuilder = XmlUtil.getDocumentBuilder();

    // capture parse errors in our problem handler
    documentBuilder.setErrorHandler(problemHandler.asSaxErrorHandler());

    try {
      // parse content
      Document document = documentBuilder.parse(new ByteArrayInputStream(bpelEntry));

      // halt on parse errors
      if (problemHandler.getProblemCount() > 0)
        return;

      /*
       * prepare a locator of imported documents, relative to the process document location within
       * the archive
       */
      ProcessWsdlLocator wsdlLocator = new ProcessArchiveWsdlLocator(new URI(location), archive);
      wsdlLocator.setProblemHandler(problemHandler);

      // read process definition
      read(processDefinition, document.getDocumentElement(), wsdlLocator);
    }
    catch (SAXException e) {
      problemHandler.add(new Problem(Problem.LEVEL_ERROR, "bpel document contains invalid xml: "
          + location, e));
    }
    catch (IOException e) {
      problemHandler.add(new Problem(Problem.LEVEL_ERROR, "bpel document is not readable: "
          + location, e));
    }
    catch (URISyntaxException e) {
      problemHandler.add(new Problem(Problem.LEVEL_ERROR,
          "process definition location is invalid: " + location, e));
    }
    finally {
      // reset error handling behavior
      documentBuilder.setErrorHandler(null);
    }
  }

  public void read(BpelProcessDefinition processDefinition, DOMSource source) {
    // save the document location
    String location = source.getSystemId();
    processDefinition.setLocation(location);

    // prepare a locator of imported documents, relative to the process document location
    ProcessWsdlLocator wsdlLocator = null;
    if (location != null) {
      try {
        wsdlLocator = new ProcessWsdlLocator(new URI(location));
        wsdlLocator.setProblemHandler(problemHandler);
      }
      catch (URISyntaxException e) {
        problemHandler.add(new Problem(Problem.LEVEL_ERROR, "source system id is invalid"));
      }
    }

    Element processElem;
    Node node = source.getNode();

    switch (node.getNodeType()) {
    case Node.DOCUMENT_NODE:
      processElem = ((Document) node).getDocumentElement();
      break;
    case Node.ELEMENT_NODE:
      processElem = (Element) node;
      break;
    default:
      problemHandler.add(new Problem(Problem.LEVEL_ERROR,
          "source node must be either an element or a document: " + node));
      return;
    }

    // read process definition
    read(processDefinition, processElem, wsdlLocator);
  }

  protected void read(BpelProcessDefinition processDefinition, Element processElem,
      ProcessWsdlLocator wsdlLocator) {
    // see if the bpel document requires updating
    if (BpelConstants.NS_BPEL_1_1.equals(processElem.getNamespaceURI())) {
      try {
        // create bpel upgrader
        Transformer bpelUpgrader = getBpelUpgradeTemplates().newTransformer();

        // install our problem handler as transformer's error listener
        bpelUpgrader.setErrorListener(problemHandler.asTraxErrorListener());

        // upgrade into dom document
        Document resultDocument = XmlUtil.createDocument();
        bpelUpgrader.transform(new DOMSource(processElem.getOwnerDocument()), new DOMResult(
            resultDocument));
        processElem = resultDocument.getDocumentElement();

        log.debug("upgraded bpel document: " + processDefinition.getLocation());
      }
      catch (TransformerException e) {
        Problem problem = new Problem(Problem.LEVEL_ERROR, "bpel upgrade failed", e);
        problem.setResource(processDefinition.getLocation());
        problemHandler.add(problem);
      }

      // halt on transform errors
      if (problemHandler.getProblemCount() > 0)
        return;
    }

    // read attributes in the process element
    readProcessAttributes(processElem, processDefinition);

    // read imported documents
    ImportDefinition importDefinition = processDefinition.getImportDefinition();
    readImports(processElem, importDefinition, wsdlLocator);

    // halt on import parse errors
    if (problemHandler.getProblemCount() > 0)
      return;

    try {
      // registration gets the query language from the process definition
      registerPropertyAliases(importDefinition);

      // finally read the global scope
      readScope(processElem, processDefinition.getGlobalScope());
      log.debug("read bpel document: " + processDefinition.getLocation());
    }
    catch (BpelException e) {
      problemHandler.add(new Problem(Problem.LEVEL_ERROR, "bpel process is invalid", e));
    }
  }

  static synchronized Templates getBpelUpgradeTemplates() throws TransformerException {
    if (bpelUpgradeTemplates == null)
      bpelUpgradeTemplates = XmlUtil.createTemplates(BpelReader.class.getResource("bpel-1-1-converter.xslt"));

    return bpelUpgradeTemplates;
  }

  public void registerPropertyAliases(ImportDefinition importDefinition) {
    // register property aliases in each wsdl document
    List imports = importDefinition.getImports();
    for (int i = 0, n = imports.size(); i < n; i++) {
      Import _import = (Import) imports.get(i);
      if (Import.Type.WSDL.equals(_import.getType()))
        registerPropertyAliases(importDefinition, (Definition) _import.getDocument());
    }
  }

  private void registerPropertyAliases(ImportDefinition importDefinition, Definition wsdlDef) {
    BpelProcessDefinition processDefinition = (BpelProcessDefinition) importDefinition.getProcessDefinition();

    // first deal with local extensibility elements
    for (Iterator i = WsdlUtil.getExtensions(wsdlDef.getExtensibilityElements(),
        WsdlConstants.Q_PROPERTY_ALIAS); i.hasNext();) {
      PropertyAlias alias = (PropertyAlias) i.next();

      // property
      Property property = alias.getProperty();
      if (!property.isUndefined())
        importDefinition.addProperty(property);
      else {
        Problem problem = new Problem(Problem.LEVEL_ERROR, "property not found: "
            + property.getQName());
        problem.setResource(wsdlDef.getDocumentBaseURI());
        problemHandler.add(problem);
      }

      // message
      Message message = alias.getMessage();
      if (message != null) {
        // check the message definition exists
        if (!message.isUndefined()) {
          // register property alias in message type
          MessageType type = importDefinition.getMessageType(message.getQName());
          type.addPropertyAlias(alias);
        }
        else {
          Problem problem = new Problem(Problem.LEVEL_ERROR, "message not found: "
              + message.getQName());
          problem.setResource(wsdlDef.getDocumentBaseURI());
          problemHandler.add(problem);
        }
      }
      else {
        QName element = alias.getElement();
        if (element != null)
          importDefinition.getElementType(element).addPropertyAlias(alias);
        else {
          QName type = alias.getType();
          if (type != null)
            importDefinition.getSchemaType(type).addPropertyAlias(alias);
          else {
            Problem problem = new Problem(Problem.LEVEL_ERROR,
                "neither message, element nor type specified in property alias");
            problem.setResource(wsdlDef.getDocumentBaseURI());
            problemHandler.add(problem);
          }
        }
      }

      // query
      PropertyQuery query = alias.getQuery();
      if (query != null) {
        // namespaces
        query.setNamespaces(processDefinition.addNamespaces(query.getNamespaces()));
        // language
        if (query.getLanguage() == null)
          query.setLanguage(processDefinition.getQueryLanguage());
        // syntax
        try {
          query.parse();
        }
        catch (BpelException e) {
          Problem problem = new Problem(Problem.LEVEL_ERROR, "could not parse query: "
              + query.getText(), e);
          problem.setResource(wsdlDef.getDocumentBaseURI());
          problemHandler.add(problem);
        }
      }
    }

    // deal with imported definitions
    for (Iterator l = wsdlDef.getImports().values().iterator(); l.hasNext();) {
      List imports = (List) l.next();
      for (int i = 0, n = imports.size(); i < n; i++) {
        javax.wsdl.Import _import = (javax.wsdl.Import) imports.get(i);
        registerPropertyAliases(importDefinition, _import.getDefinition());
      }
    }
  }

  // process properties
  // //////////////////////////////////////////////////////////////

  protected void readProcessAttributes(Element processElem, BpelProcessDefinition processDefinition) {
    // name & namespace
    processDefinition.setName(processElem.getAttribute(BpelConstants.ATTR_NAME));
    processDefinition.setTargetNamespace(processElem.getAttribute(BpelConstants.ATTR_TARGET_NAMESPACE));
    // query & expression language
    processDefinition.setQueryLanguage(XmlUtil.getAttribute(processElem,
        BpelConstants.ATTR_QUERY_LANGUAGE));
    processDefinition.setExpressionLanguage(XmlUtil.getAttribute(processElem,
        BpelConstants.ATTR_EXPRESSION_LANGUAGE));
    // suppress join failure
    Attr suppressAttr = processElem.getAttributeNode(BpelConstants.ATTR_SUPPRESS_JOIN_FAILURE);
    processDefinition.getGlobalScope().setSuppressJoinFailure(
        readTBoolean(suppressAttr, Boolean.FALSE));
  }

  protected void readImports(Element processElem, ImportDefinition importDefinition,
      ProcessWsdlLocator wsdlLocator) {
    Iterator importElemIt = XmlUtil.getElements(processElem, BpelConstants.NS_BPEL,
        BpelConstants.ELEM_IMPORT);
    if (importElemIt.hasNext()) {
      // process document explicitly imports documents
      // read only those documents
      do {
        Element importElem = (Element) importElemIt.next();
        Import _import = readImport(importElem);
        importDefinition.addImport(_import);

        if (Import.Type.WSDL.equals(_import.getType())) {
          // read wsdl document
          readImportWsdlDefinition(_import, wsdlLocator);

          // verify the wsdl target namespace matches the import namespace
          Definition def = (Definition) _import.getDocument();
          if (!_import.getNamespace().equals(def.getTargetNamespace())) {
            problemHandler.add(new ParseProblem("namespace does not match wsdl target namespace",
                importElem));
          }
        }
      } while (importElemIt.hasNext());
    }
    else if (importDefinition.getImports().isEmpty()) {
      /*
       * process document has no explicit imports and no imports were supplied by the definition
       * descriptor; fall back to master wsdl definition
       */
      Import masterImport = createMasterWsdlImport(wsdlLocator);
      importDefinition.addImport(masterImport);

      // read wsdl document
      readImportWsdlDefinition(masterImport, wsdlLocator);

      // set the import namespace to the wsdl target namespace
      Definition masterWsdl = (Definition) masterImport.getDocument();
      masterImport.setNamespace(masterWsdl.getTargetNamespace());
    }
  }

  protected Import readImport(Element importElem) {
    Import _import = new Import();
    _import.setNamespace(importElem.getAttribute(BpelConstants.ATTR_NAMESPACE));
    _import.setLocation(importElem.getAttribute(BpelConstants.ATTR_LOCATION));
    _import.setType(Import.Type.valueOf(importElem.getAttribute(BpelConstants.ATTR_IMPORT_TYPE)));
    return _import;
  }

  protected Import createMasterWsdlImport(ProcessWsdlLocator wsdlLocator) {
    // determine master wsdl location
    String processPath = wsdlLocator.getProcessURI().getPath();

    int beginIndex = processPath.lastIndexOf('/');
    if (beginIndex != -1)
      ++beginIndex;
    else
      beginIndex = 0;

    int endIndex = processPath.lastIndexOf('.');
    if (endIndex == -1 || beginIndex >= endIndex) {
      problemHandler.add(new Problem(Problem.LEVEL_ERROR,
          "cannot extract file name from process path: " + processPath));
      return new Import();
    }

    String wsdlFile = processPath.substring(beginIndex, endIndex) + ".wsdl";

    // create import for master wsdl
    Import masterImport = new Import();
    masterImport.setLocation(wsdlFile);
    masterImport.setType(Import.Type.WSDL);

    return masterImport;
  }

  public void readImportWsdlDefinition(Import _import, ProcessWsdlLocator wsdlLocator) {
    String location = _import.getLocation();
    Definition definition;
    try {
      // set import location as the base URI of the given WSDL locator
      wsdlLocator.resolveBaseURI(location);

      // read imported WSDL document
      WSDLReader reader = WsdlUtil.getFactory().newWSDLReader();
      definition = reader.readWSDL(wsdlLocator);
      log.debug("read wsdl document: " + location);
    }
    catch (WSDLException e) {
      problemHandler.add(new Problem(Problem.LEVEL_ERROR, "could not read wsdl document", e));

      // patch missing definition
      definition = WsdlUtil.getFactory().newDefinition();
      definition.setDocumentBaseURI(location);
      if (_import.getNamespace() != null)
        definition.setTargetNamespace(_import.getNamespace());
    }
    _import.setDocument(definition);
  }

  public Boolean readTBoolean(Attr attribute, Boolean defaultValue) {
    if (attribute == null)
      return defaultValue;

    String text = attribute.getValue();
    return BpelConstants.YES.equals(text) ? Boolean.TRUE
        : BpelConstants.NO.equals(text) ? Boolean.FALSE : defaultValue;
  }

  public Expression readExpression(Element enclosingElem, CompositeActivity parent) {
    Expression expression = new Expression();
    readExpression(enclosingElem, parent, expression);
    return expression;
  }

  protected void readExpression(Element enclosingElem, CompositeActivity parent,
      Expression expression) {
    // text content
    expression.setText(DatatypeUtil.toString(enclosingElem));

    // namespaces
    BpelProcessDefinition processDefinition = parent.getBpelProcessDefinition();
    expression.setNamespaces(processDefinition.addNamespaces(XmlUtil.findNamespaceDeclarations(enclosingElem)));

    // language
    String language = XmlUtil.getAttribute(enclosingElem, BpelConstants.ATTR_EXPRESSION_LANGUAGE);
    if (language == null)
      language = processDefinition.getExpressionLanguage();
    expression.setLanguage(language);

    // parsing
    try {
      expression.parse();
    }
    catch (BpelException e) {
      problemHandler.add(new ParseProblem("could not parse expression", enclosingElem, e));
    }
  }

  // scope definition properties
  // //////////////////////////////////////////////////////////////

  public void readScope(Element scopeElem, Scope scope) {
    scope.installFaultExceptionHandler();

    // partner links
    Element partnerLinksElem = XmlUtil.getElement(scopeElem, BpelConstants.NS_BPEL,
        BpelConstants.ELEM_PARTNER_LINKS);
    if (partnerLinksElem != null)
      scope.setPartnerLinks(readPartnerLinks(partnerLinksElem, scope));

    // variables
    Element variablesElem = XmlUtil.getElement(scopeElem, BpelConstants.NS_BPEL,
        BpelConstants.ELEM_VARIABLES);
    if (variablesElem != null)
      scope.setVariables(readVariables(variablesElem, scope));

    // correlation sets
    Element setsElem = XmlUtil.getElement(scopeElem, BpelConstants.NS_BPEL,
        BpelConstants.ELEM_CORRELATION_SETS);
    if (setsElem != null)
      scope.setCorrelationSets(readCorrelationSets(setsElem, scope));

    /*
     * read activity before FCT handlers; compensateScope requires the activity be present in order
     * to locate the nested scope
     */
    Element activityElem = getActivityElement(scopeElem);
    readActivity(activityElem, scope);

    // fault handlers
    Element faultHandlersElem = XmlUtil.getElement(scopeElem, BpelConstants.NS_BPEL,
        BpelConstants.ELEM_FAULT_HANDLERS);
    if (faultHandlersElem != null)
      readFaultHandlers(faultHandlersElem, scope);

    // compensation handler
    Element compensationHandlerElem = XmlUtil.getElement(scopeElem, BpelConstants.NS_BPEL,
        BpelConstants.ELEM_COMPENSATION_HANDLER);
    if (compensationHandlerElem != null)
      readCompensationHandler(compensationHandlerElem, scope);

    // termination handler
    Element terminationHandlerElem = XmlUtil.getElement(scopeElem, BpelConstants.NS_BPEL,
        BpelConstants.ELEM_TERMINATION_HANDLER);
    if (terminationHandlerElem != null) {
      readTerminationHandler(terminationHandlerElem, scope);
    }
    else {
      // install BPEL4WS 1.1 forced termination handler
      Catch forcedTerminationHandler = scope.selectCatch(BpelConstants.FAULT_FORCED_TERMINATION);
      if (forcedTerminationHandler != null)
        scope.setTerminationHandler(forcedTerminationHandler);
    }

    // event handlers
    Element eventHandlersElem = XmlUtil.getElement(scopeElem, BpelConstants.NS_BPEL,
        BpelConstants.ELEM_EVENT_HANDLERS);
    if (eventHandlersElem != null)
      readEventHandlers(eventHandlersElem, scope);
  }

  public void readCompensationHandler(Element handlerElem, Scope scope) {
    Handler handler = new Handler();

    // register handler before reading activity so that scope definitions are
    // available
    scope.setCompensationHandler(handler);

    // read handler activity
    Activity activity = readActivity(getActivityElement(handlerElem), handler);
    handler.setActivity(activity);
  }

  protected void readTerminationHandler(Element handlerElem, Scope scope) {
    Handler handler = new Handler();

    // register handler before reading activity so that scope definitions are
    // available
    scope.setTerminationHandler(handler);

    // read handler activity
    Activity activity = readActivity(getActivityElement(handlerElem), handler);
    handler.setActivity(activity);
  }

  public void readFaultHandlers(Element faultHandlersElem, Scope scope) {
    // catch elements
    Iterator catchElemIt = XmlUtil.getElements(faultHandlersElem, BpelConstants.NS_BPEL,
        BpelConstants.ELEM_CATCH);
    while (catchElemIt.hasNext()) {
      Element catchElem = (Element) catchElemIt.next();
      readCatch(catchElem, scope);
    }

    // catchAll element
    Element catchAllElem = XmlUtil.getElement(faultHandlersElem, BpelConstants.NS_BPEL,
        BpelConstants.ELEM_CATCH_ALL);
    if (catchAllElem != null)
      readCatchAll(catchAllElem, scope);
  }

  protected void readCatch(Element catchElem, Scope scope) {
    // the valid configuration for a catch is:
    // faultName OR (faultVariable AND (elementType XOR messageType))
    Catch catcher = new Catch();

    // fault name
    QName faultName = getFaultName(catchElem);
    catcher.setFaultName(faultName);

    // fault variable
    VariableDefinition faultVariable = getFaultVariable(catchElem, scope);
    if (faultVariable != null) {
      catcher.setFaultVariable(faultVariable);
    }
    else if (faultName == null) {
      problemHandler.add(new ParseProblem("catch must specify faultName, faultVariable or both",
          catchElem));
    }

    // register handler before reading activity so that variable, partnerLink
    // and correlationSet definitions are available
    scope.addCatch(catcher);

    // activity
    Activity activity = readActivity(getActivityElement(catchElem), catcher);
    catcher.setActivity(activity);
  }

  private QName getFaultName(Element catchElem) {
    Attr faultName = catchElem.getAttributeNode(BpelConstants.ATTR_FAULT_NAME);
    return faultName != null ? XmlUtil.getQNameValue(faultName) : null;
  }

  private VariableDefinition getFaultVariable(Element catchElem, Scope scope) {
    VariableDefinition faultVariable = null;
    String faultVariableAttr = XmlUtil.getAttribute(catchElem, BpelConstants.ATTR_FAULT_VARIABLE);
    if (faultVariableAttr != null) {
      VariableType faultType = getFaultType(catchElem, scope.getBpelProcessDefinition()
          .getImportDefinition());
      if (faultType != null) {
        // create variable local to handler
        faultVariable = new VariableDefinition();
        faultVariable.setName(faultVariableAttr);
        faultVariable.setType(faultType);
      }
      // BPEL-199 parse BPEL4WS 1.1 fault handler
      else {
        // retrieve variable from enclosing scope
        faultVariable = scope.findVariable(faultVariableAttr);
        // check variable exists
        if (faultVariable == null) {
          problemHandler.add(new ParseProblem("variable not found", catchElem));
        }
        // check variable is of message type
        else if (!faultVariable.getType().isMessage()) {
          problemHandler.add(new ParseProblem("catch must reference a message variable", catchElem));
        }
      }
    }
    return faultVariable;
  }

  private VariableType getFaultType(Element catchElem, ImportDefinition importDefinition) {
    VariableType type = null;

    Attr messageType = catchElem.getAttributeNode(BpelConstants.ATTR_FAULT_MESSAGE_TYPE);
    Attr elementName = catchElem.getAttributeNode(BpelConstants.ATTR_FAULT_ELEMENT);

    if (messageType != null) {
      if (elementName != null)
        problemHandler.add(new ParseProblem("found more than one fault type specifier", catchElem));
      type = getMessageType(catchElem, messageType, importDefinition);
    }
    else if (elementName != null)
      type = getElementType(catchElem, elementName, importDefinition);

    return type;
  }

  protected void readCatchAll(Element catchAllElem, Scope scope) {
    Handler handler = new Handler();

    // register handler before reading activity so that in-scope definitions are available
    scope.setCatchAll(handler);

    // read handler activity
    Activity activity = readActivity(getActivityElement(catchAllElem), handler);
    handler.setActivity(activity);
  }

  protected void readEventHandlers(Element eventHandlersElem, Scope scope) {
    // onEvents
    for (Iterator i = XmlUtil.getElements(eventHandlersElem, BpelConstants.NS_BPEL,
        BpelConstants.ELEM_ON_EVENT); i.hasNext();) {
      OnEvent onEvent = new OnEvent();
      scope.addOnEvent(onEvent);
      readOnEvent((Element) i.next(), onEvent);
    }
    // onAlarms
    for (Iterator i = XmlUtil.getElements(eventHandlersElem, BpelConstants.NS_BPEL,
        BpelConstants.ELEM_ON_ALARM); i.hasNext();) {
      OnAlarm onAlarm = new OnAlarm();
      scope.addOnAlarm(onAlarm);
      readOnAlarm((Element) i.next(), onAlarm);
    }
  }

  protected void readOnEvent(Element onEventElem, OnEvent onEvent) {
    // the attribute messageType indicates a variable declaration
    Attr messageType = onEventElem.getAttributeNode(BpelConstants.ATTR_MESSAGE_TYPE);
    if (messageType != null) {
      VariableDefinition variable = new VariableDefinition();
      // name
      String name = XmlUtil.getAttribute(onEventElem, BpelConstants.ATTR_VARIABLE);
      variable.setName(name);
      // type
      VariableType type = getMessageType(onEventElem, messageType,
          onEvent.getBpelProcessDefinition().getImportDefinition());
      variable.setType(type);
      // onEvent
      onEvent.setVariableDefinition(variable);
    }
    // receiver
    ReceiveAction receiveAction = readReceiveAction(onEventElem, onEvent);
    onEvent.setAction(receiveAction);
    // activity
    Element activityElem = getActivityElement(onEventElem);
    onEvent.setActivity(readActivity(activityElem, onEvent));
  }

  protected OnAlarm readOnAlarm(Element onAlarmElem, OnAlarm onAlarm) {
    // alarm
    onAlarm.setAction(readAlarmAction(onAlarmElem, onAlarm));
    // activity
    Element activityElem = getActivityElement(onAlarmElem);
    onAlarm.setActivity(readActivity(activityElem, onAlarm));
    return onAlarm;
  }

  protected Map readVariables(Element variablesElem, CompositeActivity parent) {
    Map variables = new HashMap();
    ImportDefinition importDefinition = parent.getBpelProcessDefinition().getImportDefinition();

    for (Iterator i = XmlUtil.getElements(variablesElem, BpelConstants.NS_BPEL,
        BpelConstants.ELEM_VARIABLE); i.hasNext();) {
      Element variableElem = (Element) i.next();
      VariableDefinition variable = readVariable(variableElem, importDefinition);
      String variableName = variable.getName();

      if (!variables.containsKey(variableName))
        variables.put(variableName, variable);
      else
        problemHandler.add(new ParseProblem("duplicate local name", variableElem));
    }
    return variables;
  }

  protected VariableDefinition readVariable(Element variableElem, ImportDefinition importDefinition) {
    VariableDefinition variable = new VariableDefinition();
    // name
    variable.setName(variableElem.getAttribute(BpelConstants.ATTR_NAME));
    // type
    variable.setType(getVariableType(variableElem, importDefinition));
    return variable;
  }

  private VariableType getVariableType(Element variableElem, ImportDefinition importDefinition) {
    VariableType type;

    Attr schemaType = variableElem.getAttributeNode(BpelConstants.ATTR_TYPE);
    Attr elementName = variableElem.getAttributeNode(BpelConstants.ATTR_ELEMENT);
    Attr messageType = variableElem.getAttributeNode(BpelConstants.ATTR_MESSAGE_TYPE);

    if (messageType != null) {
      if (schemaType != null || elementName != null)
        problemHandler.add(new ParseProblem("more than one type specifier present", variableElem));
      type = getMessageType(variableElem, messageType, importDefinition);
    }
    else if (schemaType != null) {
      if (elementName != null)
        problemHandler.add(new ParseProblem("more than one type specifier present", variableElem));
      type = getSchemaType(variableElem, schemaType, importDefinition);
    }
    else if (elementName != null) {
      type = getElementType(variableElem, elementName, importDefinition);
    }
    else {
      problemHandler.add(new ParseProblem("no type specifier present", variableElem));
      type = null;
    }
    return type;
  }

  private MessageType getMessageType(Element contextElem, Attr typeAttr,
      ImportDefinition importDefinition) {
    QName typeName = XmlUtil.getQNameValue(typeAttr);
    MessageType type = importDefinition.getMessageType(typeName);

    if (type == null) {
      // patch missing type
      Message message = WsdlUtil.getSharedDefinition().createMessage();
      message.setQName(typeName);
      type = new MessageType(message);
      problemHandler.add(new ParseProblem("message type not found", contextElem));
    }
    return type;
  }

  private ElementType getElementType(Element contextElem, Attr elementAttr,
      ImportDefinition importDefinition) {
    QName elementName = XmlUtil.getQNameValue(elementAttr);
    ElementType type = importDefinition.getElementType(elementName);

    if (type == null) {
      // patch missing type
      type = new ElementType(elementName);
      problemHandler.add(new ParseProblem("element not found", contextElem));
    }
    return type;
  }

  private SchemaType getSchemaType(Element contextElem, Attr typeAttr,
      ImportDefinition importDefinition) {
    QName typeName = XmlUtil.getQNameValue(typeAttr);
    SchemaType type = importDefinition.getSchemaType(typeName);

    if (type == null) {
      // patch missing type
      type = new SchemaType(typeName);
      problemHandler.add(new ParseProblem("schema type not found", contextElem));
    }
    return type;
  }

  // service properties
  // //////////////////////////////////////////////////////////////

  protected Map readCorrelationSets(Element setsElem, CompositeActivity superState) {
    Map correlationSets = new HashMap();
    ImportDefinition importDefinition = superState.getBpelProcessDefinition().getImportDefinition();

    for (Iterator i = XmlUtil.getElements(setsElem, BpelConstants.NS_BPEL,
        BpelConstants.ELEM_CORRELATION_SET); i.hasNext();) {
      Element setElem = (Element) i.next();
      CorrelationSetDefinition set = readCorrelationSet(setElem, importDefinition);
      String setName = set.getName();

      if (!correlationSets.containsKey(setName))
        correlationSets.put(setName, set);
      else
        problemHandler.add(new ParseProblem("duplicate local name", setElem));
    }
    return correlationSets;
  }

  protected CorrelationSetDefinition readCorrelationSet(Element setElem,
      ImportDefinition importDefinition) {
    CorrelationSetDefinition set = new CorrelationSetDefinition();
    // properties
    String[] propertyNames = setElem.getAttribute(BpelConstants.ATTR_PROPERTIES).split("\\s");
    for (int p = 0; p < propertyNames.length; p++) {
      QName propertyName = XmlUtil.parseQName(propertyNames[p], setElem);
      Property property = importDefinition.getProperty(propertyName);
      if (property == null) {
        problemHandler.add(new ParseProblem("property not found: " + propertyName, setElem));
        // patch the missing property
        try {
          property = (Property) WsdlUtil.getSharedExtensionRegistry().createExtension(
              Definition.class, WsdlConstants.Q_PROPERTY);
          property.setQName(propertyName);
        }
        catch (WSDLException e) {
          // should not happen
          throw new AssertionError(e);
        }
      }
      set.addProperty(property);
    }
    // name
    set.setName(setElem.getAttribute(BpelConstants.ATTR_NAME));
    return set;
  }

  protected Map readPartnerLinks(Element partnerLinksElem, CompositeActivity parent) {
    Map partnerLinks = new HashMap();

    for (Iterator i = XmlUtil.getElements(partnerLinksElem, BpelConstants.NS_BPEL,
        BpelConstants.ELEM_PARTNER_LINK); i.hasNext();) {
      Element partnerLinkElem = (Element) i.next();
      PartnerLinkDefinition partnerLink = readPartnerLink(partnerLinkElem, parent);
      String plinkName = partnerLink.getName();

      if (!partnerLinks.containsKey(plinkName))
        partnerLinks.put(plinkName, partnerLink);
      else
        problemHandler.add(new ParseProblem("duplicate local name", partnerLinkElem));
    }
    return partnerLinks;
  }

  protected PartnerLinkDefinition readPartnerLink(Element partnerLinkElem, CompositeActivity parent) {
    PartnerLinkDefinition partnerLink = new PartnerLinkDefinition();
    partnerLink.setName(partnerLinkElem.getAttribute(BpelConstants.ATTR_NAME));

    // partner link type
    QName typeName = XmlUtil.getQNameValue(partnerLinkElem.getAttributeNode(BpelConstants.ATTR_PARTNER_LINK_TYPE));
    PartnerLinkType type = parent.getBpelProcessDefinition()
        .getImportDefinition()
        .getPartnerLinkType(typeName);
    if (type == null) {
      problemHandler.add(new ParseProblem("partner link type not found", partnerLinkElem));
      return partnerLink;
    }
    partnerLink.setPartnerLinkType(type);

    String myRoleName = XmlUtil.getAttribute(partnerLinkElem, BpelConstants.ATTR_MY_ROLE);
    String partnerRoleName = XmlUtil.getAttribute(partnerLinkElem, BpelConstants.ATTR_PARTNER_ROLE);
    boolean partnerFirst = false;

    // first role
    Role role = type.getFirstRole();
    String roleName = role.getName();
    if (roleName.equals(myRoleName)) {
      partnerLink.setMyRole(role);
    }
    else if (roleName.equals(partnerRoleName)) {
      partnerLink.setPartnerRole(role);
      partnerFirst = true;
    }
    else {
      problemHandler.add(new ParseProblem(
          "neither my role nor partner role match the first partner link type role",
          partnerLinkElem));
      return partnerLink;
    }

    PortType portType = role.getPortType();
    if (portType.isUndefined()) {
      problemHandler.add(new ParseProblem("port type not found: " + portType.getQName(),
          partnerLinkElem));
    }

    // second role
    role = type.getSecondRole();
    if (role != null) {
      roleName = role.getName();
      if (partnerFirst) {
        if (!roleName.equals(myRoleName)) {
          problemHandler.add(new ParseProblem(
              "my role does not match the second partner link type role", partnerLinkElem));
        }
        partnerLink.setMyRole(role);
      }
      else {
        if (!roleName.equals(partnerRoleName)) {
          problemHandler.add(new ParseProblem(
              "partner role does not match the second partner link type role", partnerLinkElem));
        }
        partnerLink.setPartnerRole(role);
      }

      portType = role.getPortType();
      if (portType.isUndefined()) {
        problemHandler.add(new ParseProblem("port type not found: " + portType.getQName(),
            partnerLinkElem));
      }
    }
    else if (partnerFirst ? myRoleName != null : partnerRoleName != null)
      problemHandler.add(new ParseProblem("partner link type has one role only", partnerLinkElem));

    return partnerLink;
  }

  public ReceiveAction readReceiveAction(Element receiveElem, CompositeActivity parent) {
    ReceiveAction receiveAction = new ReceiveAction();

    // partner link
    String partnerLinkName = receiveElem.getAttribute(BpelConstants.ATTR_PARTNER_LINK);
    PartnerLinkDefinition partnerLink = parent.findPartnerLink(partnerLinkName);
    if (partnerLink == null) {
      problemHandler.add(new ParseProblem("partner link not found", receiveElem));
      return receiveAction;
    }
    receiveAction.setPartnerLink(partnerLink);

    // port type
    Role myRole = partnerLink.getMyRole();
    // BPEL-181 detect absence of my role
    if (myRole == null) {
      problemHandler.add(new ParseProblem("my role not found", receiveElem));
      return receiveAction;
    }
    PortType portType = getMessageActivityPortType(receiveElem, myRole);

    // operation
    Operation operation = getMessageActivityOperation(receiveElem, portType);
    receiveAction.setOperation(operation);

    // message exchange
    // BPEL-74: map the empty message exchange to null for compatibility with Oracle
    receiveAction.setMessageExchange(XmlUtil.getAttribute(receiveElem,
        BpelConstants.ATTR_MESSAGE_EXCHANGE));

    // variable
    VariableDefinition variable = getMessageActivityVariable(receiveElem,
        BpelConstants.ATTR_VARIABLE, parent, operation.getInput().getMessage());
    receiveAction.setVariable(variable);

    // correlations
    Element correlationsElem = XmlUtil.getElement(receiveElem, BpelConstants.NS_BPEL,
        BpelConstants.ELEM_CORRELATIONS);
    if (correlationsElem != null)
      receiveAction.setCorrelations(readCorrelations(correlationsElem, parent, variable));

    return receiveAction;
  }

  public Correlations readCorrelations(Element correlationsElem, CompositeActivity parent,
      VariableDefinition variable) {
    Correlations correlations = new Correlations();
    Iterator correlationElemIt = XmlUtil.getElements(correlationsElem, BpelConstants.NS_BPEL,
        BpelConstants.ELEM_CORRELATION);
    while (correlationElemIt.hasNext()) {
      Element correlationElem = (Element) correlationElemIt.next();
      Correlation correlation = readCorrelation(correlationElem, parent);
      checkVariableProperties(variable, correlation.getSet(), correlationElem);
      correlations.addCorrelation(correlation);
    }
    return correlations;
  }

  public Correlation readCorrelation(Element correlationElem, CompositeActivity parent) {
    Correlation correlation = new Correlation();
    // correlation set
    String setName = correlationElem.getAttribute(BpelConstants.ATTR_SET);
    CorrelationSetDefinition set = parent.findCorrelationSet(setName);
    if (set == null) {
      problemHandler.add(new ParseProblem("correlation set not found", correlationElem));
      set = new CorrelationSetDefinition();
      set.setName(setName);
    }
    correlation.setSet(set);
    // initiate mode
    correlation.setInitiate(Initiate.valueOf(XmlUtil.getAttribute(correlationElem,
        BpelConstants.ATTR_INITIATE)));
    return correlation;
  }

  PortType getMessageActivityPortType(Element serviceElem, Role role) {
    PortType effectivePortType = role.getPortType();

    // validate port type attribute, if present
    Attr portTypeAttr = serviceElem.getAttributeNode(BpelConstants.ATTR_PORT_TYPE);
    if (portTypeAttr != null) {
      QName portTypeName = XmlUtil.getQNameValue(portTypeAttr);
      if (!effectivePortType.getQName().equals(portTypeName)) {
        problemHandler.add(new ParseProblem(
            "port type mismatch between message activity and partner link", serviceElem));
      }
    }

    return effectivePortType;
  }

  Operation getMessageActivityOperation(Element serviceElem, PortType portType) {
    String operationName = serviceElem.getAttribute(BpelConstants.ATTR_OPERATION);
    Operation operation = portType.getOperation(operationName, null, null);
    if (operation == null) {
      problemHandler.add(new ParseProblem("operation not found", serviceElem));
      // patch missing operation
      operation = WsdlUtil.getSharedDefinition().createOperation();
      operation.setName(operationName);
    }
    else {
      OperationType style = operation.getStyle();
      if (style == OperationType.SOLICIT_RESPONSE || style == OperationType.NOTIFICATION)
        problemHandler.add(new ParseProblem("operation style not supported", serviceElem));
    }
    return operation;
  }

  VariableDefinition getMessageActivityVariable(Element serviceElem, String variableAttr,
      CompositeActivity parent, Message activityMessage) {
    VariableDefinition variable;
    // get variable name
    String variableName = XmlUtil.getAttribute(serviceElem, variableAttr);
    if (variableName == null) {
      problemHandler.add(new ParseProblem(variableAttr + " attribute is missing", serviceElem));
      // patch missing variable
      variable = new VariableDefinition();
      variable.setName("unnamed");
      return variable;
    }
    // find variable definition
    variable = parent.findVariable(variableName);
    if (variable == null) {
      problemHandler.add(new ParseProblem(variableAttr + " not found", serviceElem));
      // create a variable stub
      variable = new VariableDefinition();
      variable.setName(variableName);
      variable.setType(parent.getBpelProcessDefinition().getImportDefinition().getMessageType(
          activityMessage.getQName()));
    }
    // validate type
    else if (!variable.getType().getName().equals(activityMessage.getQName())) {
      problemHandler.add(new ParseProblem(variableAttr
          + " type is not applicable for the operation", serviceElem));
    }
    return variable;
  }

  void checkVariableProperties(VariableDefinition variable, CorrelationSetDefinition set,
      Element correlationElem) {
    Map variableProperties = variable.getType().getPropertyAliases();

    for (Iterator i = set.getProperties().iterator(); i.hasNext();) {
      Property property = (Property) i.next();
      QName propertyName = property.getQName();
      if (!variableProperties.containsKey(propertyName)) {
        problemHandler.add(new ParseProblem("property '"
            + propertyName
            + "' does not appear in variable '"
            + variable.getName()
            + "'", correlationElem));
      }
    }
  }

  // alarm properties
  // //////////////////////////////////////////////////////////////

  public AlarmAction readAlarmAction(Element element, CompositeActivity parent) {
    AlarmAction alarmAction = new AlarmAction();

    // for
    Element forElem = XmlUtil.getElement(element, BpelConstants.NS_BPEL, BpelConstants.ELEM_FOR);
    if (forElem != null) {
      alarmAction.setFor(readExpression(forElem, parent));
    }
    else {
      // until
      Element untilElem = XmlUtil.getElement(element, BpelConstants.NS_BPEL,
          BpelConstants.ELEM_UNTIL);
      if (untilElem != null)
        alarmAction.setUntil(readExpression(untilElem, parent));
    }

    // repeat every
    Element repeatEveryElem = XmlUtil.getElement(element, BpelConstants.NS_BPEL,
        BpelConstants.ELEM_REPEAT_EVERY);
    if (repeatEveryElem != null)
      alarmAction.setRepeatEvery(readExpression(repeatEveryElem, parent));

    return alarmAction;
  }

  // child activities
  // //////////////////////////////////////////////////////////////

  public Activity readActivity(Element activityElem, CompositeActivity parent) {
    if (activityElem == null) {
      // patch the missing activity
      return new Empty();
    }
    ActivityReader parser = (ActivityReader) activityReaders.get(activityElem.getLocalName());
    return parser.read(activityElem, parent);
  }

  protected Element getActivityElement(Element parent) {
    for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (ActivityElementPredicate.evaluate(child))
        return (Element) child;
    }
    problemHandler.add(new ParseProblem("activity not found", parent));
    return null;
  }

  protected Iterator getActivityElements(Element parent) {
    return new FilterIterator(new NodeIterator(parent), ActivityElementPredicate.INSTANCE);
  }

  protected ActivityReader getActivityReader(String name) {
    return (ActivityReader) activityReaders.get(name);
  }

  private Map createActivityReaders() {
    HashMap activityReaders = new HashMap();
    // walk through registered reader classes
    Iterator readerClassIt = activityReaderClasses.entrySet().iterator();
    while (readerClassIt.hasNext()) {
      Entry readerClassEntry = (Entry) readerClassIt.next();
      Object activityName = readerClassEntry.getKey();
      Class readerClass = (Class) readerClassEntry.getValue();
      try {
        // instantiate activity reader
        ActivityReader reader = (ActivityReader) readerClass.newInstance();
        // associate activity reader with this bpel reader
        reader.setBpelReader(this);
        activityReaders.put(activityName, reader);
      }
      catch (InstantiationException e) {
        log.warn("reader class not instantiable: " + readerClass.getName(), e);
      }
      catch (IllegalAccessException e) {
        log.warn("reader class or constructor not public: " + readerClass.getName(), e);
      }
    }
    return activityReaders;
  }

  public ProblemHandler getProblemHandler() {
    return problemHandler;
  }

  public void setProblemHandler(ProblemHandler problemHandler) {
    this.problemHandler = problemHandler;
  }

  private static Map readActivityReaderClasses() {
    // get activity readers resource name
    String resource = JbpmConfiguration.Configs.getString(RESOURCE_ACTIVITY_READERS);

    // parse activity readers document
    Element readersElem;
    try {
      readersElem = XmlUtil.parseResource(resource);
    }
    catch (SAXException e) {
      log.error("activity readers document contains invalid xml: " + resource, e);
      return Collections.EMPTY_MAP;
    }
    catch (IOException e) {
      log.error("could not read activity readers document: " + resource, e);
      return Collections.EMPTY_MAP;
    }

    // walk through activityReader elements
    HashMap activityReaderClasses = new HashMap();
    Iterator readerElemIt = XmlUtil.getElements(readersElem, null, "activityReader");
    while (readerElemIt.hasNext()) {
      Element readerElem = (Element) readerElemIt.next();
      String activityName = readerElem.getAttribute("name");

      // load reader class
      String readerClassName = readerElem.getAttribute("class");
      Class readerClass = ClassLoaderUtil.loadClass(readerClassName);

      // validate reader class
      if (!ActivityReader.class.isAssignableFrom(readerClass)) {
        log.warn("not an activity reader: " + readerClassName);
        continue;
      }

      // register reader class
      activityReaderClasses.put(activityName, readerClass);
      log.debug("registered activity reader: name=" + activityName + ", class=" + readerClassName);
    }
    return activityReaderClasses;
  }

  private static class ActivityElementPredicate implements Predicate {

    private static String[] BPEL_2_ACTIVITIES = { BpelConstants.ELEM_EMPTY,
        BpelConstants.ELEM_RECEIVE, BpelConstants.ELEM_REPLY, BpelConstants.ELEM_INVOKE,
        BpelConstants.ELEM_ASSIGN, BpelConstants.ELEM_THROW, BpelConstants.ELEM_EXIT,
        BpelConstants.ELEM_WAIT, BpelConstants.ELEM_SEQUENCE, BpelConstants.ELEM_IF, "switch",
        BpelConstants.ELEM_WHILE, BpelConstants.ELEM_PICK, BpelConstants.ELEM_FLOW,
        BpelConstants.ELEM_SCOPE, BpelConstants.ELEM_COMPENSATE,
        BpelConstants.ELEM_COMPENSATE_SCOPE, BpelConstants.ELEM_RETHROW,
        BpelConstants.ELEM_VALIDATE };

    private static final Set activityNames = new HashSet(
        Arrays.asList(ActivityElementPredicate.BPEL_2_ACTIVITIES));

    static final Predicate INSTANCE = new ActivityElementPredicate();

    private ActivityElementPredicate() {
    }

    public boolean evaluate(Object arg) {
      return evaluate((Node) arg);
    }

    static boolean evaluate(Node node) {
      /*
       * the node is an activity if (1) it is an element, (2) its namespace URI matches the BPEL
       * namespace and (3) the set of activity names contains its local name
       */
      return node.getNodeType() == Node.ELEMENT_NODE
          && BpelConstants.NS_BPEL.equals(node.getNamespaceURI())
          && activityNames.contains(node.getLocalName());
    }
  }
}
