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

/**
 * Binding of <tt>partnerLink</tt> element.
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/10/13 02:53:24 $
 */
public class PartnerLinkDescriptor {

  private String name;
  private PartnerRoleDescriptor partnerRole;
  private MyRoleDescriptor myRole;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public MyRoleDescriptor getMyRole() {
    return myRole;
  }

  public void setMyRole(MyRoleDescriptor myRole) {
    this.myRole = myRole;
  }

  public PartnerRoleDescriptor getPartnerRole() {
    return partnerRole;
  }

  public void setPartnerRole(PartnerRoleDescriptor partnerRole) {
    this.partnerRole = partnerRole;
  }

  public void accept(DeploymentVisitor visitor) {
    visitor.visit(this);
  }
}
