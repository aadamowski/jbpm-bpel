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
package org.jbpm.bpel.wsdl.impl;

import javax.wsdl.PortType;

import org.jbpm.bpel.wsdl.PartnerLinkType;
import org.jbpm.bpel.wsdl.xml.WsdlConstants;

/**
 * A partner link type characterizes the conversational relationship between two
 * services. It does so by defining the "roles" played by each of the services
 * in the conversation and specifying the portType provided by each service to
 * receive messages within the context of the conversation.
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2006/08/21 01:05:59 $
 */
public class PartnerLinkTypeImpl extends NamedExtension implements
    PartnerLinkType {

  private static final long serialVersionUID = 1L;

  /**
   * The role played by each of the services in the conversation.
   */
  public static class RoleImpl implements Role {

    long id;
    private String name;
    private PortType portType;

    private static final long serialVersionUID = 1L;

    /**
     * {@inheritDoc}
     */
    public String getName() {
      return name;
    }

    /**
     * {@inheritDoc}
     */
    public void setName(String name) {
      this.name = name;
    }

    /**
     * {@inheritDoc}
     */
    public PortType getPortType() {
      return portType;
    }

    /**
     * {@inheritDoc}
     */
    public void setPortType(PortType portType) {
      this.portType = portType;
    }
  }

  private Role firstRole;
  private Role secondRole;

  /**
   * Constructs a partner link type and sets the element type.
   */
  public PartnerLinkTypeImpl() {
    setElementType(WsdlConstants.Q_PARTNER_LINK_TYPE);
  }

  /**
   * {@inheritDoc}
   */
  public Role getFirstRole() {
    return firstRole;
  }

  /**
   * {@inheritDoc}
   */
  public void setFirstRole(Role firstRole) {
    this.firstRole = firstRole;
  }

  /**
   * {@inheritDoc}
   */
  public Role getSecondRole() {
    return secondRole;
  }

  /**
   * {@inheritDoc}
   */
  public void setSecondRole(Role secondRole) {
    this.secondRole = secondRole;
  }

  /**
   * {@inheritDoc}
   */
  public Role createRole() {
    return new RoleImpl();
  }

  public Role getRole(String roleName) {
    Role role = null;
    if (firstRole != null && firstRole.getName().equals(roleName))
      role = firstRole;
    else if (secondRole != null && secondRole.getName().equals(roleName))
      role = secondRole;
    return role;
  }
}
