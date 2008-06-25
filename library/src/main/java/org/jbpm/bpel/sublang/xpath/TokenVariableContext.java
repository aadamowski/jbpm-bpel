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

import java.io.Serializable;

import org.jaxen.UnresolvableException;
import org.jaxen.VariableContext;

import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.variable.def.VariableDefinition;
import org.jbpm.bpel.variable.exe.MessageValue;
import org.jbpm.graph.exe.Token;

/**
 * An implementation of {@link VariableContext} that resolves variable bindings
 * from the values in the scope of the enclosed {@link Token token}.
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/05/29 01:09:34 $
 */
public class TokenVariableContext implements VariableContext, Serializable {

  private final Token token;

  private static final long serialVersionUID = 1L;

  /**
   * Creates a variable context that wraps the given token.
   * @param token the enclosed token
   * @throws NullPointerException if <code>token</code> is <code>null</code>
   */
  public TokenVariableContext(Token token) {
    if (token == null)
      throw new NullPointerException("token must not be null");

    this.token = token;
  }

  /**
   * Gets the enclosed token.
   * @return the enclosed token
   */
  public Token getToken() {
    return token;
  }

  /**
   * Finds the definition of the specified variable in the scope of the
   * {@linkplain Activity activity} where the enclosed token is.
   * @param variableName name of the desired variable
   * @return the variable definition or <code>null</code> if there is no match
   */
  public VariableDefinition findVariableDefinition(String variableName) {
    Activity activity = (Activity) token.getNode();
    return activity.getCompositeActivity().findVariable(variableName);
  }

  /**
   * Returns the value of a process variable based on the given local name.
   * @return the value of the resolved variable
   * @throws UnresolvableException in any of the following situations:
   * <ul>
   * <li><code>namespaceURI</code> is not <code>null</code>; process
   * variables are always unqualified</li>
   * <li><code>localName</code> does not match any process variable</li>
   * </ul>
   */
  public Object getVariableValue(String namespaceURI, String prefix, String localName)
      throws UnresolvableException {
    if (namespaceURI != null)
      throw new UnresolvableException("variable not found: " + prefix + ':' + localName);

    Object value;
    // look for a dot in the variable name, indicating a message part access
    int dotIndex = localName.indexOf('.');
    if (dotIndex == -1) {
      // dotless name => variable must be defined as schema type or element
      // find variable definition
      VariableDefinition definition = findVariableDefinition(localName);
      if (definition == null)
        throw new UnresolvableException("variable not found: " + localName);

      // extract variable value
      value = definition.getValue(token);

      // prevent direct access to message variable
      if (value instanceof MessageValue)
        throw new UnresolvableException("illegal access to message variable: " + localName);
    }
    else {
      // dotted name => variable must be defined as messsage
      String messageName = localName.substring(0, dotIndex);
      // find variable definition
      VariableDefinition definition = findVariableDefinition(messageName);
      if (definition == null)
        throw new UnresolvableException("variable not found: " + messageName);

      // extract variable value
      value = definition.getValue(token);

      // prevent access to part of non-message variable
      if (!(value instanceof MessageValue)) {
        throw new UnresolvableException("illegal access to part of non-message variable: "
            + localName);
      }

      // retrieve part value
      MessageValue messageValue = (MessageValue) value;
      String partName = localName.substring(dotIndex + 1);
      value = messageValue.getPart(partName);
    }
    return value;
  }
}
