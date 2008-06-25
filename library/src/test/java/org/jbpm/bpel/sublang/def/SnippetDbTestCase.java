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
package org.jbpm.bpel.sublang.def;

import java.util.HashSet;
import java.util.Set;

import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.def.Namespace;
import org.jbpm.bpel.persistence.db.AbstractDbTestCase;
import org.jbpm.bpel.xml.BpelConstants;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/10/13 02:53:31 $
 */
public abstract class SnippetDbTestCase extends AbstractDbTestCase {

  private BpelProcessDefinition processDefinition;
  private Snippet snippet;

  protected void setUp() throws Exception {
    super.setUp();
    // process, create after opening a jbpm context
    processDefinition = new BpelProcessDefinition("pd", BpelConstants.NS_EXAMPLES);
    snippet = createSnippet(processDefinition);
  }

  protected abstract Snippet createSnippet(BpelProcessDefinition processDefinition);

  public void testText() {
    snippet.setText("$x/y");

    processDefinition = saveAndReload(processDefinition);
    snippet = getSnippet(processDefinition);

    assertEquals("$x/y", snippet.getText());
  }

  public void testLanguage() {
    snippet.setLanguage("cobol");

    processDefinition = saveAndReload(processDefinition);
    snippet = getSnippet(processDefinition);

    assertEquals("cobol", snippet.getLanguage());
  }

  public void testNamespaceDeclarations() {
    Set namespaces = new HashSet();
    namespaces.add(new Namespace("jbpm", "http://www.jbpm.org"));
    namespaces.add(new Namespace("nme", "http://www.nme.com"));
    snippet.setNamespaces(processDefinition.addNamespaces(namespaces));

    processDefinition = saveAndReload(processDefinition);
    snippet = getSnippet(processDefinition);

    namespaces = snippet.getNamespaces();
    assertEquals(2, namespaces.size());
    assertTrue(namespaces.contains(new Namespace("jbpm", "http://www.jbpm.org")));
    assertTrue(namespaces.contains(new Namespace("nme", "http://www.nme.com")));
  }

  protected abstract Snippet getSnippet(BpelProcessDefinition processDefinition);
}
