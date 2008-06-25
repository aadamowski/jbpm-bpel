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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jaxen.Context;
import org.jaxen.ContextSupport;
import org.jaxen.Function;
import org.jaxen.FunctionCallException;
import org.jaxen.JaxenException;
import org.jaxen.Navigator;
import org.jaxen.function.StringFunction;
import org.w3c.dom.Element;

import org.jbpm.bpel.variable.def.VariableDefinition;
import org.jbpm.bpel.variable.exe.MessageValue;

/**
 * The <strong>getVariableData</strong> function extracts arbitrary values from variables.
 * <p>
 * <code><i>any</i> bpws:getVariableData(<i>string</i> variableName,
 * <i>string</i> partName?, <i>string</i> locationPath?)</code>
 * </p>
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/09/12 23:20:16 $
 */
public class GetVariableDataFunction implements Function {

  private static final Log log = LogFactory.getLog(GetVariableDataFunction.class);

  /**
   * Extracts an arbitrary value from a variable.
   * @param context the context where the function is called
   * @param args a list containing the variable name and, optionally, the part and location path to
   * access
   * @return the variable value
   * @throws FunctionCallException if <code>args</code> is empty or has more than three items
   */
  public Object call(Context context, List args) throws FunctionCallException {
    Object value;

    switch (args.size()) {
    case 1:
      value = evaluate(args.get(0), context);
      break;
    case 2:
      value = evaluate(args.get(0), args.get(1), context);
      break;
    case 3:
      value = evaluate(args.get(0), args.get(1), args.get(2), context);
      break;
    default:
      throw new FunctionCallException("getVariableData() requires one to three arguments");
    }
    return value;
  }

  /**
   * Extracts the value of a variable.
   * @param variableArg the variable name
   * @param context the context where the function is called
   * @return the variable value
   * @throws FunctionCallException if <code>variableArg</code> does not match any variable
   */
  public static Object evaluate(Object variableArg, Context context) throws FunctionCallException {
    log.debug("parameters: " + variableArg);

    // find variable definition
    String variableName = StringFunction.evaluate(variableArg, context.getNavigator());
    TokenVariableContext variableContext = (TokenVariableContext) context.getContextSupport()
        .getVariableContext();
    VariableDefinition variable = variableContext.findVariableDefinition(variableName);
    if (variable == null)
      throw new FunctionCallException("variable not found: " + variableName);

    // retrieve variable value
    Object variableValue = variable.getValue(variableContext.getToken());
    if (variableValue instanceof MessageValue)
      throw new FunctionCallException("illegal access to message variable: " + variableName);

    log.debug("return value: " + variableValue);
    return variableValue;
  }

  /**
   * Extracts the value of a variable part.
   * @param variableArg the variable name
   * @param partArg the part name
   * @param context the context at the point in the expression when the function is called
   * @return the part value
   * @throws FunctionCallException if any of the following situations occurs:
   * <ul>
   * <li><code>variableArg</code> does not match any variable</li>
   * <li><code>variableArg</code> references a non-message variable</li>
   * </ul>
   */
  public static Object evaluate(Object variableArg, Object partArg, Context context)
      throws FunctionCallException {
    log.debug("parameters: " + variableArg + ", " + partArg);

    Navigator nav = context.getNavigator();

    // find variable definition
    String variableName = StringFunction.evaluate(variableArg, nav);
    TokenVariableContext variableContext = (TokenVariableContext) context.getContextSupport()
        .getVariableContext();
    VariableDefinition variable = variableContext.findVariableDefinition(variableName);
    if (variable == null)
      throw new FunctionCallException("variable not found: " + variableName);

    // retrieve variable value
    Object variableValue = variable.getValue(variableContext.getToken());
    if (!(variableValue instanceof MessageValue))
      throw new FunctionCallException("illegal access to part of non-message variable: "
          + variableName);

    // extract part value
    String partName = StringFunction.evaluate(partArg, nav);
    Element partValue = ((MessageValue) variableValue).getPart(partName);
    log.debug("return value: " + partValue);
    return partValue;
  }

  /**
   * Extracts the value of a location within a variable part.
   * @param variableArg the variable name
   * @param partArg the part name
   * @param locationArg the location within the part
   * @param context the context at the point in the expression when the function is called
   * @return the location value
   * @throws FunctionCallException if any of the following situations occurs:
   * <ul>
   * <li><code>variableArg</code> does not match any variable</li>
   * <li><code>variableArg</code> references a non-message variable</li>
   * <li><code>locationArg</code> contains an invalid query</li>
   * </ul>
   */
  public static Object evaluate(Object variableArg, Object partArg, Object locationArg,
      Context context) throws FunctionCallException {
    log.debug("parameters: " + variableArg + ", " + partArg + ", " + locationArg);

    Navigator navigator = context.getNavigator();
    ContextSupport support = context.getContextSupport();

    // find variable definition
    TokenVariableContext variableContext = (TokenVariableContext) support.getVariableContext();
    String variableName = StringFunction.evaluate(variableArg, navigator);
    VariableDefinition variable = variableContext.findVariableDefinition(variableName);
    if (variable == null)
      throw new FunctionCallException("variable not found: " + variableName);

    // retrieve variable value
    Object variableValue = variable.getValue(variableContext.getToken());
    if (!(variableValue instanceof MessageValue))
      throw new FunctionCallException("illegal access to part of non-message variable: "
          + variableName);

    // extract part value
    String partName = StringFunction.evaluate(partArg, navigator);
    Element partValue = ((MessageValue) variableValue).getPart(partName);

    // evaluate location string
    String locationString = StringFunction.evaluate(locationArg, navigator);
    try {
      XPathVariableQueryEvaluator evaluator = XPathEvaluatorFactory.createVariableQueryEvaluator(locationString);
      evaluator.setNamespaceContext(support.getNamespaceContext());
      // in 1.1 the context node is 'the root of the document fragment representing the entire part'
      Object queryValue = evaluator.evaluate(partValue.getOwnerDocument(),
          variableContext.getToken());
      log.debug("return value: " + queryValue);
      return queryValue;
    }
    catch (JaxenException e) {
      throw new FunctionCallException("could not parse query", e);
    }
  }
}