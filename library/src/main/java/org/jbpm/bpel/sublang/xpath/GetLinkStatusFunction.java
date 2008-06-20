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
import org.jaxen.Function;
import org.jaxen.FunctionCallException;
import org.jaxen.UnresolvableException;
import org.jaxen.VariableContext;
import org.jaxen.function.StringFunction;

/**
 * The <strong>getLinkStatus</strong> function indicates the status of a link.
 * <p>
 * <code><i>boolean</i> bpws:getLinkStatus(<i>string</i> linkName)</code>
 * </p>
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/07/26 00:16:08 $
 */
public class GetLinkStatusFunction implements Function {

  private static final Log log = LogFactory.getLog(GetLinkStatusFunction.class);

  /**
   * Indicates the status of a link.
   * @param context the context where the function is called
   * @param args a list containing the link name
   * @return the link status
   * @throws FunctionCallException if <code>args</code> does not have exactly one item
   */
  public Object call(Context context, List args) throws FunctionCallException {
    if (args.size() != 1)
      throw new FunctionCallException("getLinkStatus() requires one argument");

    return evaluate(args.get(0), context);
  }

  /**
   * Indicates the status of a link.
   * @param linkArg the link name
   * @param context the context where the function is called
   * @return the link status
   * @throws FunctionCallException if <code>linkArg</code> does not match any link
   */
  public static Boolean evaluate(Object linkArg, Context context) throws FunctionCallException {
    log.debug("parameters: " + linkArg);

    String linkName = StringFunction.evaluate(linkArg, context.getNavigator());
    VariableContext variableContext = context.getContextSupport().getVariableContext();
    try {
      Boolean linkValue = (Boolean) variableContext.getVariableValue(null, null, linkName);
      log.debug("return value: " + linkValue);
      return linkValue;
    }
    catch (UnresolvableException e) {
      throw new FunctionCallException("variable not found: " + linkName);
    }
  }
}