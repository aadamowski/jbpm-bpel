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

import java.util.Set;

import org.jaxen.JaxenException;
import org.jaxen.XPathSyntaxException;
import org.jaxen.expr.Expr;
import org.jaxen.expr.LocationPath;
import org.jaxen.expr.PathExpr;

import org.jbpm.bpel.BpelException;
import org.jbpm.bpel.sublang.def.VariableQuery;
import org.jbpm.bpel.sublang.def.Expression;
import org.jbpm.bpel.sublang.def.JoinCondition;
import org.jbpm.bpel.sublang.def.PropertyQuery;
import org.jbpm.bpel.sublang.exe.VariableQueryEvaluator;
import org.jbpm.bpel.sublang.exe.EvaluatorFactory;
import org.jbpm.bpel.sublang.exe.ExpressionEvaluator;
import org.jbpm.bpel.sublang.exe.PropertyQueryEvaluator;

/**
 * A factory for expressions and queries written in XPath 1.0.
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/09/12 23:20:16 $
 */
public class XPathEvaluatorFactory extends EvaluatorFactory {

  private static final XPathEvaluatorFactory instance = new XPathEvaluatorFactory();

  public static EvaluatorFactory getInstance() {
    return instance;
  }

  public ExpressionEvaluator createEvaluator(Expression expression) {
    // parse text and create evaluator corresponding to expression class
    String text = expression.getText();
    XPathExpressionEvaluator evaluator;
    try {
      evaluator = expression instanceof JoinCondition ? createJoinConditionEvaluator(text)
          : createExpressionEvaluator(text);
    }
    catch (JaxenException e) {
      throw new BpelException("could not create evaluator for expression: " + expression, e);
    }

    // set namespace declarations
    Set namespaces = expression.getNamespaces();
    if (namespaces != null)
      evaluator.setNamespaceContext(new SetNamespaceContext(namespaces));

    return evaluator;
  }

  public PropertyQueryEvaluator createEvaluator(PropertyQuery query) {
    // parse text and create evaluator
    XPathPropertyQueryEvaluator evaluator;
    try {
      evaluator = createPropertyQueryEvaluator(query.getText());
    }
    catch (JaxenException e) {
      throw new BpelException("could not create evaluator for query: " + query, e);
    }

    // set namespace declarations
    Set namespaces = query.getNamespaces();
    if (namespaces != null)
      evaluator.setNamespaceContext(new SetNamespaceContext(namespaces));

    return evaluator;
  }

  public VariableQueryEvaluator createEvaluator(VariableQuery query) {
    // parse text and create evaluator
    XPathVariableQueryEvaluator evaluator;
    try {
      evaluator = createVariableQueryEvaluator(query.getText());
    }
    catch (JaxenException e) {
      throw new BpelException("could not create evaluator for query: " + query, e);
    }

    // set namespace declarations
    Set namespaces = query.getNamespaces();
    if (namespaces != null)
      evaluator.setNamespaceContext(new SetNamespaceContext(namespaces));

    return evaluator;
  }

  static XPathExpressionEvaluator createExpressionEvaluator(String text) throws JaxenException {
    XPathExpressionEvaluator expression = new XPathExpressionEvaluator(text);

    // expressions are general xpath expressions, excluding location paths
    if (!new ExprValidator().validate(expression.getRootExpr()))
      throw new XPathSyntaxException(text, 0, "illegal access to root/context node");

    return expression;
  }

  static XPathJoinConditionEvaluator createJoinConditionEvaluator(String text)
      throws JaxenException {
    XPathJoinConditionEvaluator joinCondition = new XPathJoinConditionEvaluator(text);

    // expressions are general xpath expressions, excluding location paths
    if (!new ExprValidator().validate(joinCondition.getRootExpr()))
      throw new XPathSyntaxException(text, 0, "illegal access to root/context node");

    return joinCondition;
  }

  static XPathVariableQueryEvaluator createVariableQueryEvaluator(String text)
      throws JaxenException {
    /*
     * BPEL 2.0 S8.2.6: the restrictions on an expression do not apply to a query because it has a
     * defined context node; any legal XPath expression can be used
     */
    return new XPathVariableQueryEvaluator(text);
  }

  static XPathPropertyQueryEvaluator createPropertyQueryEvaluator(String text)
      throws JaxenException {
    /*
     * BPEL 2.0 S8.2.6: the restrictions on an expression do not apply to a query because it has a
     * defined context node; any legal XPath expression can be used
     */
    return new XPathPropertyQueryEvaluator(text);
  }

  private static class ExprValidator extends ExprVisitor {

    private boolean valid;

    public boolean validate(Expr expr) {
      valid = true;
      visit(expr);
      return valid;
    }

    public void visit(PathExpr expr) {
      visit(expr.getFilterExpr());
      /*
       * location paths relative to filter expressions are valid, a.o.t. absolute or
       * context-relative paths
       */
      // visit(expr.getLocationPath());
    }

    public void visit(LocationPath locationPath) {
      valid = false;
    }
  }
}