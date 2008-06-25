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

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.lang.builder.ToStringBuilder;

import org.jbpm.bpel.BpelException;
import org.jbpm.bpel.graph.def.Namespace;
import org.jbpm.bpel.sublang.exe.EvaluatorFactory;
import org.jbpm.bpel.xml.BpelConstants;

/**
 * Common base for {@linkplain Expression expressions} and {@linkplain PropertyQuery queries}.
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/09/12 23:20:15 $
 */
public abstract class Snippet implements Serializable {

  long id;
  private String text;
  private String language;
  private Set namespaces;

  private static final long serialVersionUID = 1L;

  protected Snippet() {
  }

  /**
   * Gets the lexical representation of this expression.
   * @return the text form
   */
  public String getText() {
    return text;
  }

  /**
   * Sets the lexical representation of this expression.
   * @param text the text form
   */
  public void setText(String text) {
    this.text = text;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public Set getNamespaces() {
    return namespaces;
  }

  public void setNamespaces(Set namespaces) {
    this.namespaces = namespaces;
  }

  public void setNamespaces(Map namespaceMap) {
    HashSet namespaces = new HashSet();
    Iterator namespaceEntryIt = namespaceMap.entrySet().iterator();
    while (namespaceEntryIt.hasNext()) {
      Entry namespaceEntry = (Entry) namespaceEntryIt.next();
      namespaces.add(new Namespace((String) namespaceEntry.getKey(),
          (String) namespaceEntry.getValue()));
    }
    setNamespaces(namespaces);
  }

  protected EvaluatorFactory getEvaluatorFactory() {
    // get language specific to this script
    String language = getLanguage();
    // no specific language, use XPath 1.0
    if (language == null)
      language = BpelConstants.URN_XPATH_1_0;

    // get factory associated to language
    EvaluatorFactory factory = EvaluatorFactory.getInstance(language);
    if (factory == null)
      throw new BpelException("unsupported language: " + language);
    return factory;
  }

  public String toString() {
    return new ToStringBuilder(this).append("text", text).toString();
  }
}