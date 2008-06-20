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
package org.jbpm.bpel.graph.exe;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.jbpm.bpel.graph.scope.Scope;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.exe.Token;

/**
 * @author Juan Cantu
 * @version $Revision$ $Date: 2007/07/02 23:55:06 $
 */
class ScopeInstanceIterator implements Iterator {

  private List tokensToTraverse = new ArrayList();
  private ScopeInstance currentScopeInstance;

  public ScopeInstanceIterator(Token parent) {
    Map children = parent.getChildren();
    if (children != null && !children.isEmpty()) {
      tokensToTraverse.addAll(children.values());
      currentScopeInstance = nextScopeInstance();
    }
  }

  public boolean hasNext() {
    return currentScopeInstance != null;
  }

  public Object next() {
    if (!hasNext())
      throw new NoSuchElementException();

    Object lastReturned = currentScopeInstance;
    currentScopeInstance = nextScopeInstance();
    return lastReturned;
  }

  public void remove() {
    throw new UnsupportedOperationException();
  }

  private ScopeInstance nextScopeInstance() {
    while (!tokensToTraverse.isEmpty()) {
      ScopeInstance scopeInstance = getScopeInstance(nextToken());
      if (scopeInstance != null)
        return scopeInstance;
    }
    return null;
  }

  private Token nextToken() {
    if (tokensToTraverse.isEmpty())
      throw new NoSuchElementException();

    Token nextToken = (Token) tokensToTraverse.remove(tokensToTraverse.size() - 1);
    // add children of the next token to the traverse collection
    Map children = nextToken.getChildren();
    // children are added if they don't have an instance themselves.
    // this causes the iterator to retrieve only instances of the immediate
    // nested level
    if (getScopeInstance(nextToken) == null && children != null)
      tokensToTraverse.addAll(children.values());

    return nextToken;
  }

  private static ScopeInstance getScopeInstance(Token token) {
    ContextInstance contextInstance = (ContextInstance) token.getProcessInstance()
        .getInstance(ContextInstance.class);
    return (ScopeInstance) contextInstance.getLocalVariable(
        Scope.VARIABLE_NAME, token);
  }
}
