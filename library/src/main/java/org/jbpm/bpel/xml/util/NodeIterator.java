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
package org.jbpm.bpel.xml.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.w3c.dom.Node;

/**
 * An iterator over the children of a DOM node.
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2006/08/21 01:05:59 $
 */
public class NodeIterator implements Iterator {

  private Node currentNode;
  private Node lastReturned;

  /**
   * Creates an iterator over the children of the given node.
   * @param parentNode the node to iterate
   */
  public NodeIterator(Node parentNode) {
    currentNode = parentNode.getFirstChild();
  }

  public boolean hasNext() {
    return currentNode != null;
  }

  public Object next() {
    if (currentNode == null) throw new NoSuchElementException();
    lastReturned = currentNode;
    currentNode = lastReturned.getNextSibling();
    return lastReturned;
  }

  public void remove() {
    if (lastReturned == null) throw new IllegalStateException();
    Node parentNode = lastReturned.getParentNode();
    if (parentNode != null) parentNode.removeChild(lastReturned);
    lastReturned = null;
  }
}