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
 * @author Juan Cantu
 * @version $Revision$ $Date: 2007/10/13 02:53:24 $
 */
public interface DeploymentVisitor {

  public void visit(DeploymentDescriptor deploymentDescriptor);

  public void visit(ScopeDescriptor scopeDescriptor);

  public void visit(PartnerLinkDescriptor partnerLinkDescriptor);
}
