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

import java.util.Iterator;

import org.jaxen.Context;
import org.jaxen.ContextSupport;
import org.jaxen.FunctionContext;
import org.jaxen.JaxenException;
import org.jaxen.SimpleVariableContext;

import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.LinkDefinition;
import org.jbpm.bpel.sublang.def.Snippet;
import org.jbpm.graph.exe.Token;

/**
 * Evaluator for expressions in join conditions. Only link status can be used within join conditions
 * and only join conditions can make use of link status.
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/07/22 05:57:25 $
 */
class XPathJoinConditionEvaluator extends XPathExpressionEvaluator {

  /**
   * The jBPM configuration object name for the resource describing the function library available
   * to join conditions.
   */
  public static final String FUNCTIONS_CONFIG_NAME = "resource.join.condition.functions";

  private static final long serialVersionUID = 1L;

  private static final FunctionContext functionLibrary = readFunctionLibrary(FUNCTIONS_CONFIG_NAME);

  XPathJoinConditionEvaluator(String text) throws JaxenException {
    super(text);
  }

  protected FunctionContext createFunctionContext() {
    return functionLibrary;
  }

  /**
   * Gets a context for expression evaluation. The context for a join condition is:
   * <ul>
   * <li>the variable bindings are the links targeting the current node of the given token</li>
   * <li>the function library is read from resource comes from {@link #createFunctionContext()}</li>
   * <li>the namespace declarations come from {@link Snippet#getNamespaces()}</li>
   * </ul>
   * @param node an instance of {@link Token}
   */
  protected Context getContext(Object node) {
    Token token = (Token) node;
    Activity activity = (Activity) token.getNode();
    SimpleVariableContext variableContext = new SimpleVariableContext();

    // set status of target links in the variable context
    for (Iterator i = activity.getTargets().iterator(); i.hasNext();) {
      LinkDefinition target = (LinkDefinition) i.next();
      variableContext.setVariableValue(target.getName(), target.getInstance(token).getStatus());
    }

    ContextSupport support = new ContextSupport(getNamespaceContext(), getFunctionContext(),
        variableContext, getNavigator());
    return new Context(support);
  }
}
