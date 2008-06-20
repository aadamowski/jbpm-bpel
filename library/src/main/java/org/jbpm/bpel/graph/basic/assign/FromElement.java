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
package org.jbpm.bpel.graph.basic.assign;

import org.w3c.dom.Element;

import org.jbpm.graph.exe.Token;

/**
 * <code>&lt;from&gt;</code> variant that allows an element literal to be given as the source
 * value to assign to a destination.
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/07/22 05:57:25 $
 */
public class FromElement extends From {

  private Element literal;

  private static final long serialVersionUID = 1L;

  public Object extract(Token token) {
    return literal;
  }

  /**
   * Gets the element literal extracted as source value.
   * @return an element value
   */
  public Element getLiteral() {
    return literal;
  }

  /**
   * Sets the element literal to extract as source value.
   * @param literal an element value
   */
  public void setLiteral(Element literal) {
    this.literal = literal;
  }
}
