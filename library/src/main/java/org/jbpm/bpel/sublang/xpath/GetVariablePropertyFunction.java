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

import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jaxen.Context;
import org.jaxen.ContextSupport;
import org.jaxen.Function;
import org.jaxen.FunctionCallException;
import org.jaxen.NamespaceContext;
import org.jaxen.Navigator;
import org.jaxen.function.StringFunction;

import org.jbpm.bpel.variable.def.VariableDefinition;

/**
 * The <strong>getVariableProperty</strong> function extracts global property values from
 * variables.
 * <p>
 * <code><i>any</i> bpws:getVariableProperty(<i>string</i> variableName,
 * <i>string</i> propertyName)</code>
 * </p>
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/07/26 00:16:08 $
 */
public class GetVariablePropertyFunction implements Function {

  private static final Log log = LogFactory.getLog(GetVariablePropertyFunction.class);

  /**
   * Extracts the value of a global property from a variable.
   * @param context the context at the point in the expression when the function is called
   * @param args a list containing the variable name and the property qualified name
   * @return the property vlaue
   * @throws FunctionCallException if <code>args</code> does not have exactly two items
   */
  public Object call(Context context, List args) throws FunctionCallException {
    if (args.size() != 2)
      throw new FunctionCallException("getVariableProperty() requires two arguments");

    return evaluate(args.get(0), args.get(1), context);
  }

  /**
   * Extracts the value of a global property from a variable.
   * @param variableArg the variable name
   * @param propertyArg the property qualified name
   * @param context the context at the point in the expression when the function is called
   * @return the property value
   * @throws FunctionCallException if <code>variableArg</code> does not match any variable
   */
  public static Object evaluate(Object variableArg, Object propertyArg, Context context)
      throws FunctionCallException {
    log.debug("parameters: " + variableArg + ", " + propertyArg);

    // convert arguments to strings
    Navigator nav = context.getNavigator();
    ContextSupport sup = context.getContextSupport();

    // find variable definition
    String variableName = StringFunction.evaluate(variableArg, nav);
    TokenVariableContext variableContext = (TokenVariableContext) sup.getVariableContext();
    VariableDefinition variableDefinition = variableContext.findVariableDefinition(variableName);
    if (variableDefinition == null)
      throw new FunctionCallException("variable not found: " + variableName);

    // resolve property name
    String propertyName = StringFunction.evaluate(propertyArg, nav);
    NamespaceContext nsContext = sup.getNamespaceContext();
    QName propertyQName = toQName(propertyName, nsContext);

    // extract property value
    Object propertyValue = variableDefinition.getPropertyValue(propertyQName,
        variableContext.getToken());
    log.debug("return value: " + propertyValue);
    return propertyValue;
  }

  private static QName toQName(String prefixedName, NamespaceContext namespaceContext) {
    int colonIndex = prefixedName.indexOf(':');
    if (colonIndex == -1)
      return new QName(prefixedName);

    String localName = prefixedName.substring(colonIndex + 1);
    String prefix = prefixedName.substring(0, colonIndex);
    String namespaceURI = namespaceContext.translateNamespacePrefixToUri(prefix);
    return new QName(namespaceURI, localName, prefix);
  }
}
