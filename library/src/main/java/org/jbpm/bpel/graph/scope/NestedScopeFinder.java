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
package org.jbpm.bpel.graph.scope;

import java.util.ArrayList;
import java.util.Collection;

import org.jbpm.bpel.graph.def.AbstractBpelVisitor;

/**
 * @author Juan Cantu
 * @version $Revision$ $Date: 2007/06/26 18:43:19 $
 */
class NestedScopeFinder extends AbstractBpelVisitor {

  private Collection scopes = new ArrayList();

  public Collection getScopes() {
    return scopes;
  }

  public void visit(Scope scope) {
    scopes.add(scope);
    scope.getActivity().accept(this);
  }
}