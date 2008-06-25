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
package org.jbpm.bpel.deploy;

import org.apache.commons.lang.enums.Enum;

import org.jbpm.bpel.endpointref.EndpointReference;

/**
 * Binding of <tt>partnerRole</tt> element.
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/10/13 02:53:24 $
 */
public class PartnerRoleDescriptor {

  private InitiateMode initiateMode;
  private EndpointReference endpointReference;

  public InitiateMode getInitiateMode() {
    return initiateMode;
  }

  public void setInitiateMode(InitiateMode initiateMode) {
    this.initiateMode = initiateMode;
  }

  public EndpointReference getEndpointReference() {
    return endpointReference;
  }

  public void setEndpointReference(EndpointReference endpointReference) {
    this.endpointReference = endpointReference;
  }

  public static final class InitiateMode extends Enum {

    public static final InitiateMode STATIC = new InitiateMode("static");
    public static final InitiateMode PUSH = new InitiateMode("push");
    public static final InitiateMode PULL = new InitiateMode("pull");

    private static final long serialVersionUID = 1L;

    /**
     * Enumeration constructor.
     * @param name the desired textual representation.
     */
    private InitiateMode(String name) {
      super(name);
    }

    /**
     * Gets an enumeration object by name.
     * @param name a string that identifies one element
     * @return the appropiate enumeration object, or <code>null</code> if the
     *         object does not exist
     */
    public static InitiateMode valueOf(String name) {
      return name != null ? (InitiateMode) getEnum(InitiateMode.class, name)
          : InitiateMode.STATIC;
    }
  }
}
