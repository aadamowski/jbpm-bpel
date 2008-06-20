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
package org.jbpm.bpel.wsdl;

import java.io.Serializable;

import javax.wsdl.PortType;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.xml.namespace.QName;

/**
 * Characterizes the conversational relationship between two services. Defines
 * the {@linkplain Role roles} played by each service in the conversation and
 * specifies the portType provided by each service.
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/01/22 00:24:55 $
 */
public interface PartnerLinkType extends ExtensibilityElement, Serializable {

  /**
   * Gets the name of this partner link type.
   */
  public QName getQName();

  /**
   * Sets the name of this partner link type.
   */
  public void setQName(QName name);

  /**
   * Gets the first (mandatory) role of this partner link type.
   */
  public Role getFirstRole();

  /**
   * Sets the first (mandatory) role of this partner link type.
   */
  public void setFirstRole(Role firstRole);

  /**
   * Gets the second (optional) role of this partner link type.
   */
  public Role getSecondRole();

  /**
   * Sets the second (optional) role of this partner link type.
   */
  public void setSecondRole(Role secondRole);

  /**
   * Creates a role.
   */
  public Role createRole();

  /**
   * The role played by each service in the conversation characterized by the
   * enclosing {@linkplain PartnerLinkType partner link type}.
   */
  public static interface Role extends Serializable {

    /**
     * Gets the name of this role.
     */
    public String getName();

    /**
     * Sets the name of this role.
     */
    public void setName(String name);

    /**
     * Gets the port type of this role.
     */
    public PortType getPortType();

    /**
     * Sets the port type of this role.
     */
    public void setPortType(PortType portType);
  }
}