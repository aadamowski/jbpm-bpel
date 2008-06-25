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
package org.jbpm.bpel.sublang.xpath;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jaxen.BaseXPath;
import org.jaxen.Context;
import org.jaxen.ContextSupport;
import org.jaxen.Function;
import org.jaxen.FunctionContext;
import org.jaxen.JaxenException;
import org.jaxen.SimpleFunctionContext;
import org.jaxen.dom.DocumentNavigator;
import org.jaxen.expr.CommentNodeStep;
import org.jaxen.expr.LocationPath;
import org.jaxen.expr.NameStep;
import org.jaxen.expr.Predicate;
import org.jaxen.expr.ProcessingInstructionNodeStep;
import org.jaxen.expr.Step;
import org.jaxen.expr.TextNodeStep;
import org.jaxen.saxpath.Axis;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import org.jbpm.JbpmConfiguration;
import org.jbpm.bpel.graph.exe.BpelFaultException;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.bpel.xml.util.XmlUtil;
import org.jbpm.util.ClassLoaderUtil;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/09/04 06:42:26 $
 */
abstract class XPathEvaluator extends BaseXPath {

  private static final FunctionContext EMPTY_FUNCTION_CONTEXT = new SimpleFunctionContext();

  private static final Log log = LogFactory.getLog(XPathEvaluator.class);
  private static final long serialVersionUID = 1L;

  XPathEvaluator(String text) throws JaxenException {
    super(text, DocumentNavigator.getInstance());
  }

  protected static List selectOrCreateNodes(LocationPath location, Context context)
      throws JaxenException {
    List contextNodes = context.getNodeSet();
    // empty context nodeset -> empty selection
    if (contextNodes.isEmpty())
      return Collections.EMPTY_LIST;

    ContextSupport support = context.getContextSupport();

    // absolute path?
    if (location.isAbsolute()) {
      // start from the root node
      Object rootNode = support.getNavigator().getDocumentNode(contextNodes.get(0));
      contextNodes = Collections.singletonList(rootNode);
    }

    Iterator stepIter = location.getSteps().iterator();
    while (stepIter.hasNext()) {
      // prepare the context for the current step
      Context stepContext = new Context(support);
      stepContext.setNodeSet(contextNodes);
      // evaluate the step, capture the selected nodes
      Step step = (Step) stepIter.next();
      contextNodes = step.evaluate(stepContext);
      // no node was selected?
      if (contextNodes.isEmpty()) {
        // try to create the missing node
        Node newNode = createNode(step, stepContext);
        // create a new list, since the existing empty list is immutable
        contextNodes = Collections.singletonList(newNode);
      }
    }
    return contextNodes;
  }

  private static Node createNode(Step step, Context context) {
    List nodeset = context.getNodeSet();
    if (nodeset.size() != 1) {
      log.error("cannot create node for context node set of size other than one: " + nodeset);
      throw new BpelFaultException(BpelConstants.FAULT_SELECTION_FAILURE);
    }

    Object contextNode = nodeset.get(0);
    if (!(contextNode instanceof Element)) {
      log.error("cannot create node for non-element context node: " + contextNode);
      throw new BpelFaultException(BpelConstants.FAULT_SELECTION_FAILURE);
    }

    Element contextElem = (Element) contextNode;
    Node newNode;

    switch (step.getAxis()) {
    case Axis.ATTRIBUTE:
      newNode = createAttribute(step, context, contextElem);
      break;
    case Axis.CHILD:
      if (step instanceof NameStep)
        newNode = createElement((NameStep) step, context, contextElem);
      else
        newNode = createNonElementChild(step, contextElem);
      contextElem.appendChild(newNode);
      break;
    default:
      log.error("cannot create node on the specified axis: " + step);
      throw new BpelFaultException(BpelConstants.FAULT_SELECTION_FAILURE);
    }
    return newNode;
  }

  private static Attr createAttribute(Step step, Context context, Element contextElem) {
    if (!step.getPredicates().isEmpty()) {
      log.error("cannot create attribute for step with predicates: " + step);
      throw new BpelFaultException(BpelConstants.FAULT_SELECTION_FAILURE);
    }

    if (!(step instanceof NameStep)) {
      log.error("cannot create attribute for non-name node test: " + step);
      throw new BpelFaultException(BpelConstants.FAULT_SELECTION_FAILURE);
    }

    NameStep nameStep = (NameStep) step;
    String localName = nameStep.getLocalName();

    if ("*".equals(localName)) {
      log.error("cannot create attribute for any-name node test: " + nameStep);
      throw new BpelFaultException(BpelConstants.FAULT_SELECTION_FAILURE);
    }

    // BPEL-191: preserve source prefix
    String prefix = nameStep.getPrefix();
    String namespaceURI = context.translateNamespacePrefixToUri(prefix);

    Attr attribute = contextElem.getOwnerDocument().createAttributeNS(namespaceURI,
        namespaceURI != null ? prefix + ':' + localName : localName);
    contextElem.setAttributeNodeNS(attribute);

    return attribute;
  }

  private static Element createElement(NameStep step, Context context, Element contextElem) {
    String localName = step.getLocalName();
    if ("*".equals(localName)) {
      log.error("cannot create node for any-name node tests: " + step);
      throw new BpelFaultException(BpelConstants.FAULT_SELECTION_FAILURE);
    }

    // BPEL-191: preserve source prefix
    String prefix = step.getPrefix();
    String namespaceURI = context.translateNamespacePrefixToUri(prefix);

    List predicates = step.getPredicates();
    switch (predicates.size()) {
    case 0:
      break;
    case 1: {
      // prepare the context for the predicate
      List contextNodes = getElements(contextElem, namespaceURI, localName);
      Context predicateContext = new Context(context.getContextSupport());
      predicateContext.setNodeSet(contextNodes);

      try {
        // evaluate the predicate, capture the result
        Predicate predicate = (Predicate) predicates.get(0);
        Object result = predicate.evaluate(predicateContext);

        if (!(result instanceof Number)) {
          log.error("cannot create element for step with non-numeric predicate: " + step);
          throw new BpelFaultException(BpelConstants.FAULT_SELECTION_FAILURE);
        }

        if (((Number) result).intValue() != contextNodes.size() + 1) {
          log.error("cannot create element for step with numeric predicate "
              + "beyond the next position: "
              + step);
          throw new BpelFaultException(BpelConstants.FAULT_SELECTION_FAILURE);
        }
      }
      catch (JaxenException e) {
        log.error("predicate evaluation failed", e);
        throw new BpelFaultException(BpelConstants.FAULT_SUB_LANGUAGE_EXECUTION);
      }
      break;
    }
    default:
      log.error("cannot create element for step with multiple predicates: " + step);
      throw new BpelFaultException(BpelConstants.FAULT_SELECTION_FAILURE);
    }

    String qualifiedName = namespaceURI != null ? prefix + ':' + localName : localName;
    return contextElem.getOwnerDocument().createElementNS(namespaceURI, qualifiedName);
  }

  private static Node createNonElementChild(Step step, Element contextElem) {
    if (!step.getPredicates().isEmpty()) {
      log.error("cannot create node for step with predicates: " + step);
      throw new BpelFaultException(BpelConstants.FAULT_SELECTION_FAILURE);
    }

    Document contextDoc = contextElem.getOwnerDocument();

    if (step instanceof TextNodeStep)
      return contextDoc.createTextNode("");

    if (step instanceof ProcessingInstructionNodeStep) {
      ProcessingInstructionNodeStep processingStep = (ProcessingInstructionNodeStep) step;
      return contextDoc.createProcessingInstruction(processingStep.getName(), "");
    }

    if (step instanceof CommentNodeStep)
      return contextDoc.createComment("");

    log.error("cannot create node for any-node tests on the child axis: " + step);
    throw new BpelFaultException(BpelConstants.FAULT_SELECTION_FAILURE);
  }

  private static List getElements(Element parent, String namespaceURI, String localName) {
    ArrayList elements = new ArrayList();
    for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (XmlUtil.nodeNameEquals(child, namespaceURI, localName))
        elements.add(child);
    }
    return elements;
  }

  protected static Node narrowToSingleNode(List nodeset) {
    log.debug("narrowing to single node: " + nodeset);
    if (nodeset == null || nodeset.size() != 1) {
      log.error("selection of size other than one: " + nodeset);
      throw new BpelFaultException(BpelConstants.FAULT_SELECTION_FAILURE);
    }
    Object singleResult = nodeset.get(0);
    if (!(singleResult instanceof Node)) {
      log.error("selection is not a node: " + singleResult);
      throw new BpelFaultException(BpelConstants.FAULT_SELECTION_FAILURE);
    }
    return (Node) singleResult;
  }

  protected static FunctionContext readFunctionLibrary(String configName) {
    // get functions resource name
    String resource = JbpmConfiguration.Configs.getString(configName);

    // parse functions document
    Element functionsElem;
    try {
      functionsElem = XmlUtil.parseResource(resource);
    }
    catch (SAXException e) {
      log.error("functions document contains invalid xml: " + resource, e);
      return EMPTY_FUNCTION_CONTEXT;
    }
    catch (IOException e) {
      log.error("could not read functions document: " + resource, e);
      return EMPTY_FUNCTION_CONTEXT;
    }

    // load function context class
    String functionContextClassName = functionsElem.getAttribute("class");
    Class functionContextClass = ClassLoaderUtil.loadClass(functionContextClassName);

    // instantiate function context
    FunctionContext functionContext;
    try {
      functionContext = (FunctionContext) functionContextClass.newInstance();
    }
    catch (InstantiationException e) {
      log.warn("function context class not instantiable: " + functionContextClassName, e);
      return EMPTY_FUNCTION_CONTEXT;
    }
    catch (IllegalAccessException e) {
      log.warn("function context class or constructor not public: " + functionContextClassName, e);
      return EMPTY_FUNCTION_CONTEXT;
    }

    // walk through function elements
    Iterator functionElemIt = XmlUtil.getElements(functionsElem, null, "function");
    if (functionElemIt.hasNext()) {
      /*
       * since the FunctionContext interface does not mandate methods to register new functions, we
       * cannot add functions to an arbitrary implementation, unless it is a subclass of the
       * SimpleFunctionContext included with Jaxen
       */
      if (!SimpleFunctionContext.class.isAssignableFrom(functionContextClass)) {
        log.warn("unknown function context implementation, cannot add functions to it: "
            + functionContextClassName);
        return functionContext;
      }
      SimpleFunctionContext simpleContext = (SimpleFunctionContext) functionContext;

      while (functionElemIt.hasNext()) {
        Element functionElem = (Element) functionElemIt.next();

        // name - XPath function names are QNames
        QName functionName = XmlUtil.getQNameValue(functionElem.getAttributeNode("name"));

        // load function class
        String functionClassName = functionElem.getAttribute("class");
        Class functionClass = ClassLoaderUtil.loadClass(functionClassName);

        // validate function class
        if (!Function.class.isAssignableFrom(functionClass)) {
          log.warn("not a function: " + functionClassName);
          continue;
        }

        try {
          // instantiate function
          Function function = (Function) functionClass.newInstance();
          // register function
          simpleContext.registerFunction(functionName.getNamespaceURI(),
              functionName.getLocalPart(), function);
          log.debug("registered function: name=" + functionName + ", class=" + functionClassName);
        }
        catch (InstantiationException e) {
          log.warn("function class not instantiable: " + functionClassName, e);
        }
        catch (IllegalAccessException e) {
          log.warn("function class or constructor not public: " + functionClassName, e);
        }
      }
    }
    return functionContext;
  }
}