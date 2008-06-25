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
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jaxen.Context;
import org.jaxen.ContextSupport;
import org.jaxen.FunctionContext;
import org.jaxen.JaxenException;
import org.jaxen.expr.Expr;
import org.jaxen.expr.LocationPath;
import org.w3c.dom.Node;

import org.jbpm.bpel.graph.exe.BpelFaultException;
import org.jbpm.bpel.sublang.exe.VariableQueryEvaluator;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.bpel.xml.util.XmlUtil;
import org.jbpm.graph.exe.Token;

/**
 * Default evaluator for variable queries.
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/09/12 23:20:16 $
 */
class XPathVariableQueryEvaluator extends XPathEvaluator implements VariableQueryEvaluator {

  /**
   * The jBPM configuration object name for the resource describing the function library available
   * to variable queries.
   */
  public static final String FUNCTIONS_CONFIG_NAME = "resource.variable.query.functions";

  private static final long serialVersionUID = 1L;

  private static final Log log = LogFactory.getLog(XPathVariableQueryEvaluator.class);

  private static final FunctionContext functionLibrary = readFunctionLibrary(FUNCTIONS_CONFIG_NAME);

  XPathVariableQueryEvaluator(String text) throws JaxenException {
    super(text);
  }

  public Object evaluate(Node contextNode, Token token) {
    Context context = getContext(contextNode, token);
    try {
      List nodeSet = selectNodes(context);
      return narrowToSingleNode(nodeSet);
    }
    catch (JaxenException e) {
      log.error("query evaluation failed", e);
      throw new BpelFaultException(BpelConstants.FAULT_SUB_LANGUAGE_EXECUTION);
    }
  }

  public void assign(Node contextNode, Token token, Object value) {
    Expr expr = getRootExpr();
    Context context = getContext(contextNode, token);
    try {
      List nodeSet = expr instanceof LocationPath ? selectOrCreateNodes((LocationPath) expr,
          context) : selectNodes(context);
      XmlUtil.setObjectValue(narrowToSingleNode(nodeSet), value);
    }
    catch (JaxenException e) {
      log.error("query assignment failed", e);
      throw new BpelFaultException(BpelConstants.FAULT_SUB_LANGUAGE_EXECUTION);
    }
  }

  protected FunctionContext createFunctionContext() {
    return functionLibrary;
  }

  private Context getContext(Node contextNode, Token token) {
    ContextSupport support = getContextSupport();
    support.setVariableContext(new TokenVariableContext(token));

    Context context = new Context(support);
    context.setNodeSet(Collections.singletonList(contextNode));
    return context;
  }
}
