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
package org.jbpm.bpel.integration.def;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.builder.ToStringBuilder;

import org.jbpm.bpel.integration.exe.CorrelationSetInstance;
import org.jbpm.bpel.wsdl.Property;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.exe.Token;

/**
 * Each correlation set is a named group of properties used to identify an
 * application-level conversation within a process instance.
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/03/22 13:32:55 $
 */
public class CorrelationSetDefinition implements Serializable {

  long id;
  private String name;
  private Set properties;

  private static final String VARIABLE_PREFIX = "c:";
  private static final long serialVersionUID = 1L;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void addProperty(Property property) {
    if (properties == null)
      properties = new HashSet();

    properties.add(property);
  }

  public Set getProperties() {
    return properties;
  }

  public String toString() {
    return new ToStringBuilder(this).append("name", name)
        .append("id", id)
        .toString();
  }

  public CorrelationSetInstance getInstance(Token token) {
    ContextInstance context = token.getProcessInstance().getContextInstance();
    return (CorrelationSetInstance) context.getVariable(VARIABLE_PREFIX + name,
        token);
  }

  public CorrelationSetInstance createInstance(Token token) {
    CorrelationSetInstance instance = new CorrelationSetInstance();
    instance.setDefinition(this);
    ContextInstance context = token.getProcessInstance().getContextInstance();
    context.createVariable(VARIABLE_PREFIX + name, instance, token);
    return instance;
  }
}