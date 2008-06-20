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
package org.jbpm.bpel.variable.exe;

import org.w3c.dom.Element;

import org.jbpm.context.exe.JbpmTypeMatcher;

/**
 * Criteria for deciding whether a variable value is a
 * {@linkplain Element DOM element}.
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/01/22 00:24:55 $
 */
public class ElementMatcher implements JbpmTypeMatcher {

  private static final long serialVersionUID = 1L;

  public boolean matches(Object value) {
    return value instanceof Element;
  }
}
