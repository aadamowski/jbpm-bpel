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
package org.jbpm.bpel.sublang.xpath;

import java.util.Iterator;
import java.util.Set;

import org.jaxen.NamespaceContext;

import org.jbpm.bpel.graph.def.Namespace;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2006/10/29 06:13:41 $
 */
class SetNamespaceContext implements NamespaceContext {

  private final Set namespaces;

  public SetNamespaceContext(Set namespaces) {
    this.namespaces = namespaces;
  }

  public String translateNamespacePrefixToUri(String prefix) {
    Iterator namespaceIt = namespaces.iterator();
    while (namespaceIt.hasNext()) {
      Namespace namespace = (Namespace) namespaceIt.next();
      if (prefix.equals(namespace.getPrefix()))
        return namespace.getURI();
    }
    return null;
  }
}
