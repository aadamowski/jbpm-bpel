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

import org.apache.commons.lang.builder.ToStringBuilder;

import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.exe.Token;

import org.jbpm.bpel.integration.exe.PartnerLinkInstance;
import org.jbpm.bpel.wsdl.PartnerLinkType;
import org.jbpm.bpel.wsdl.PartnerLinkType.Role;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/09/10 23:58:50 $
 */
public class PartnerLinkDefinition implements Serializable {

  private long id;
  private String name;
  private PartnerLinkType partnerLinkType;
  private boolean partnerFirst;

  private static final String VARIABLE_PREFIX = "p:";
  private static final long serialVersionUID = 1L;

  public PartnerLinkDefinition() {
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public PartnerLinkType getPartnerLinkType() {
    return partnerLinkType;
  }

  public void setPartnerLinkType(PartnerLinkType partnerLinkType) {
    this.partnerLinkType = partnerLinkType;
  }

  public Role getPartnerRole() {
    return partnerLinkType != null ? partnerFirst ? partnerLinkType.getFirstRole()
        : partnerLinkType.getSecondRole()
        : null;
  }

  public void setPartnerRole(Role partnerRole) {
    if (partnerLinkType == null) {
      throw new IllegalStateException(
          "partner link type is null, set it before the roles");
    }
    partnerFirst = partnerRole == partnerLinkType.getFirstRole();
  }

  public Role getMyRole() {
    return partnerLinkType != null ? partnerFirst ? partnerLinkType.getSecondRole()
        : partnerLinkType.getFirstRole()
        : null;
  }

  public void setMyRole(Role myRole) {
    if (partnerLinkType == null) {
      throw new IllegalStateException(
          "partner link type is null, set it before the roles");
    }
    partnerFirst = myRole != partnerLinkType.getFirstRole();
  }

  public long getId() {
    return id;
  }

  public String toString() {
    return new ToStringBuilder(this).append("name", name)
        .append("type", partnerLinkType.getQName())
        .toString();
  }

  public PartnerLinkInstance createInstance(Token token) {
    PartnerLinkInstance instance = new PartnerLinkInstance();
    instance.setDefinition(this);
    ContextInstance context = token.getProcessInstance().getContextInstance();
    context.createVariable(VARIABLE_PREFIX + name, instance, token);
    return instance;
  }

  public PartnerLinkInstance getInstance(Token token) {
    ContextInstance context = token.getProcessInstance().getContextInstance();
    return (PartnerLinkInstance) context.getVariable(VARIABLE_PREFIX + name,
        token);
  }
}
