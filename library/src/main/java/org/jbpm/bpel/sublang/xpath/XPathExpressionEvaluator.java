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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jaxen.Context;
import org.jaxen.ContextSupport;
import org.jaxen.FunctionContext;
import org.jaxen.JaxenException;
import org.jaxen.expr.Expr;
import org.jaxen.expr.FilterExpr;
import org.jaxen.expr.FunctionCallExpr;
import org.jaxen.expr.LiteralExpr;
import org.jaxen.expr.LocationPath;
import org.jaxen.expr.NumberExpr;
import org.jaxen.expr.Predicate;
import org.jaxen.expr.PredicateSet;
import org.jaxen.expr.VariableReferenceExpr;
import org.w3c.dom.Node;

import org.jbpm.bpel.BpelException;
import org.jbpm.bpel.graph.exe.BpelFaultException;
import org.jbpm.bpel.sublang.exe.ExpressionEvaluator;
import org.jbpm.bpel.variable.def.VariableDefinition;
import org.jbpm.bpel.variable.exe.MessageValue;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.bpel.xml.util.XmlUtil;
import org.jbpm.graph.exe.Token;

/**
 * Evaluator for general expressions. When XPath 1.0 is used as an expression language, there is no
 * context node available.
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/09/12 23:20:16 $
 */
class XPathExpressionEvaluator extends XPathEvaluator implements ExpressionEvaluator {

  /**
   * The jBPM configuration object name for the resource describing the function library available
   * to expressions.
   */
  public static final String FUNCTIONS_CONFIG_NAME = "resource.expression.functions";

  private static final long serialVersionUID = 1L;
  private static final Log log = LogFactory.getLog(XPathExpressionEvaluator.class);

  private static final FunctionContext functionLibrary = readFunctionLibrary(FUNCTIONS_CONFIG_NAME);

  XPathExpressionEvaluator(String text) throws JaxenException {
    super(text);
  }

  /**
   * Evaluates this expression. The context of XPath evaluation is:
   * <ul>
   * <li>the context node, position and size are unspecified</li>
   * <li>the function library, namespace declaration and variable bindings are obtained by a call
   * to {@link #getContext(Object)} with the given <code>contextInfo</code> as argument</li>
   * <ul>
   */
  public Object evaluate(Token contextToken) {
    try {
      List nodeSet = selectNodes(contextToken);
      if (nodeSet != null) {
        switch (nodeSet.size()) {
        case 0:
          break;
        case 1:
          return nodeSet.get(0);
        default:
          return nodeSet;
        }
      }
      return null;
    }
    catch (JaxenException e) {
      log.error("expression evaluation failed", e);
      throw new BpelFaultException(BpelConstants.FAULT_SUB_LANGUAGE_EXECUTION);
    }
  }

  public void assign(Token contextToken, Object value) {
    Expr rootExpr = getRootExpr();
    Context context = getContext(contextToken);
    // direct variable assignment?
    if (rootExpr instanceof VariableReferenceExpr) {
      assignVariable((VariableReferenceExpr) rootExpr, context, value);
    }
    else {
      try {
        new ExprAssigner().assign(rootExpr, context, value);
      }
      catch (JaxenException e) {
        log.error("expression assignment failed", e);
        throw new BpelFaultException(BpelConstants.FAULT_SUB_LANGUAGE_EXECUTION);
      }
    }
  }

  protected FunctionContext createFunctionContext() {
    return functionLibrary;
  }

  /**
   * Gets a context for XPath evaluation. The context for an expression expression is:
   * <ul>
   * <li>the variable bindings are the variables in the scope of the given token</li>
   * <li>the function library is {@link #functionLibrary}</li>
   * <li>the namespace declarations are taken from the snippet</li>
   * </ul>
   * @param token an instance of {@link Token}
   */
  protected Context getContext(Object token) {
    ContextSupport support = getContextSupport();
    support.setVariableContext(new TokenVariableContext((Token) token));

    return new Context(support);
  }

  private static void assignVariable(VariableReferenceExpr rootExpr, Context context, Object value) {
    String variableName = rootExpr.getVariableName();
    TokenVariableContext variableContext = (TokenVariableContext) context.getContextSupport()
        .getVariableContext();
    // look for a dot in the variable name, indicating a message part access
    int dotIndex = variableName.indexOf('.');
    if (dotIndex == -1) {
      // dotless name => variable must be defined as schema type or element
      // find variable definition
      VariableDefinition variableDefinition = variableContext.findVariableDefinition(variableName);
      if (variableDefinition == null)
        throw new BpelException("variable not found: " + variableName);

      // extract variable value
      Object valueForAssign = variableDefinition.getValueForAssign(variableContext.getToken());

      // prevent direct access to a message variable
      if (valueForAssign instanceof MessageValue)
        throw new BpelException("illegal access to message variable: " + variableName);

      // assign variable value
      variableDefinition.getType().setValue(valueForAssign, value);
    }
    else {
      // dotted name => variable must be defined as messsage
      String messageName = variableName.substring(0, dotIndex);

      // find variable definition
      VariableDefinition variableDefinition = variableContext.findVariableDefinition(messageName);
      if (variableDefinition == null)
        throw new BpelException("variable not found: " + messageName);

      // extract variable value
      Object valueForAssign = variableDefinition.getValueForAssign(variableContext.getToken());

      // prevent access to part of non-message variable
      if (!(valueForAssign instanceof MessageValue))
        throw new BpelException("illegal access to part of non-message variable: " + variableName);

      // assign part value
      MessageValue messageValue = (MessageValue) valueForAssign;
      String partName = variableName.substring(dotIndex + 1);
      messageValue.setPart(partName, value);
    }
  }

  private static class ExprAssigner extends ExprVisitor {

    private Context context;
    private Object result;
    private JaxenException jaxenException;

    public void assign(Expr pathExpr, Context context, Object value) throws JaxenException {
      this.context = context;
      visit(pathExpr);

      if (jaxenException != null)
        throw jaxenException;

      Node node = result instanceof List ? narrowToSingleNode((List) result) : (Node) result;
      XmlUtil.setObjectValue(node, value);
    }

    public void visit(FilterExpr filterExpr) {
      visit(filterExpr.getExpr());
      // if result is not a list, then there is nothing to filter
      if (result instanceof List) {
        try {
          result = evaluatePredicates(filterExpr.getPredicateSet(), (List) result,
              context.getContextSupport());
        }
        catch (JaxenException e) {
          jaxenException = e;
        }
      }
    }

    public void visit(LocationPath locationPath) {
      context.setNodeSet(result instanceof List ? (List) result : Collections.singletonList(result));
      try {
        result = selectOrCreateNodes(locationPath, context);
      }
      catch (JaxenException e) {
        jaxenException = e;
      }
    }

    public void visit(VariableReferenceExpr varExpr) {
      String variableName = varExpr.getVariableName();
      TokenVariableContext variableContext = (TokenVariableContext) context.getContextSupport()
          .getVariableContext();
      // look for a dot in the variable name, indicating a message part access
      int dotIndex = variableName.indexOf('.');
      if (dotIndex == -1) {
        // dotless name => variable must be defined as schema type or element
        // find variable definition
        VariableDefinition variableDefinition = variableContext.findVariableDefinition(variableName);
        if (variableDefinition == null)
          throw new BpelException("variable not found: " + variableName);

        // variable value for assign
        result = variableDefinition.getValueForAssign(variableContext.getToken());
      }
      else {
        // dotted name => variable must be defined as messsage
        String messageName = variableName.substring(0, dotIndex);

        // find variable definition
        VariableDefinition definition = variableContext.findVariableDefinition(messageName);
        if (definition == null)
          throw new BpelException("variable not found: " + messageName);

        // prevent access to a non-existent part
        if (!definition.getType().isMessage())
          throw new BpelException("illegal access to part of non-message variable: " + variableName);

        // part value for assign
        MessageValue messageValue = (MessageValue) definition.getValueForAssign(variableContext.getToken());
        String partName = variableName.substring(dotIndex + 1);
        result = messageValue.getPartForAssign(partName);
      }
    }

    public void visit(LiteralExpr literalExpr) {
      try {
        result = literalExpr.evaluate(context);
      }
      catch (JaxenException e) {
        jaxenException = e;
      }
    }

    public void visit(NumberExpr numberExpr) {
      try {
        result = numberExpr.evaluate(context);
      }
      catch (JaxenException e) {
        jaxenException = e;
      }
    }

    public void visit(FunctionCallExpr callExpr) {
      try {
        result = callExpr.evaluate(context);
      }
      catch (JaxenException e) {
        jaxenException = e;
      }
    }

    private List evaluatePredicates(PredicateSet predicateSet, List nodes, ContextSupport support)
        throws JaxenException {
      List predicates = predicateSet.getPredicates();
      if (!predicates.isEmpty()) {
        for (Iterator i = predicateSet.getPredicates().iterator(); i.hasNext();) {
          Predicate predicate = (Predicate) i.next();
          nodes = predicateSet.applyPredicate(predicate, nodes, support);
        }
      }
      return nodes;
    }
  }
}
