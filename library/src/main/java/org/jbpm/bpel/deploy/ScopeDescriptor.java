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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Binding of <tt>scope</tt> element.
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/10/13 02:53:24 $
 */
public class ScopeDescriptor {

  private String name;
  private Map partnerLinks = new HashMap();
  private List scopes = new ArrayList();

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Map getPartnerLinks() {
    return partnerLinks;
  }

  public void addPartnerLink(PartnerLinkDescriptor partnerLink) {
    partnerLinks.put(partnerLink.getName(), partnerLink);
  }

  public List getScopes() {
    return scopes;
  }

  public void addScope(ScopeDescriptor scope) {
    scopes.add(scope);
  }

  public void accept(DeploymentVisitor visitor) {
    visitor.visit(this);
  }
}
