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
package org.jbpm.bpel.graph.def;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.JbpmConfiguration;
import org.jbpm.bpel.graph.scope.Scope;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.module.def.ModuleDefinition;
import org.jbpm.util.ClassLoaderUtil;

/**
 * BPEL process definitions model the behavior of a participant in a business interaction.
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/10/17 22:02:52 $
 */
public class BpelProcessDefinition extends ProcessDefinition {

  private String targetNamespace;
  private String queryLanguage;
  private String expressionLanguage;
  private String location;
  private Set namespaces = new HashSet();

  private static final Log log = LogFactory.getLog(BpelProcessDefinition.class);
  private static List moduleClasses = readModuleClasses();

  private static final long serialVersionUID = 1L;

  public BpelProcessDefinition() {
  }

  public BpelProcessDefinition(String name, String targetNamespace) {
    super(name);
    this.targetNamespace = targetNamespace;
  }

  public Node addNode(Node node) {
    if (!(node instanceof Scope))
      throw new IllegalArgumentException("not a scope: " + node);

    if (nodes != null && !nodes.isEmpty())
      removeNode((Node) nodes.get(0));

    return super.addNode(node);
  }

  public ModuleDefinition getDefinition(Class clazz) {
    checkDefinitions();
    return super.getDefinition(clazz);
  }

  public Map getDefinitions() {
    checkDefinitions();
    return super.getDefinitions();
  }

  private void checkDefinitions() {
    if (definitions != null)
      return;

    // walk through registered module classes
    for (int i = 0, n = moduleClasses.size(); i < n; i++) {
      Class moduleClass = (Class) moduleClasses.get(i);
      try {
        ModuleDefinition module = (ModuleDefinition) moduleClass.newInstance();
        addDefinition(module);
      }
      catch (InstantiationException e) {
        log.warn("cannot instantiate module class: " + moduleClass, e);
      }
      catch (IllegalAccessException e) {
        log.warn("module class or constructor not public: " + moduleClass, e);
      }
    }
  }

  public Scope getGlobalScope() {
    Scope globalScope;

    if (nodes == null || nodes.isEmpty()) {
      globalScope = new Scope();
      globalScope.setName("global");
      globalScope.setImplicit(true);
      globalScope.setSuppressJoinFailure(Boolean.FALSE);

      addNode(globalScope);
    }
    else
      globalScope = (Scope) nodes.get(0);

    return globalScope;
  }

  public ImportDefinition getImportDefinition() {
    return (ImportDefinition) getDefinition(ImportDefinition.class);
  }

  public String getTargetNamespace() {
    return targetNamespace;
  }

  public void setTargetNamespace(String targetNamespace) {
    this.targetNamespace = targetNamespace;
  }

  public String getQueryLanguage() {
    return queryLanguage;
  }

  public void setQueryLanguage(String queryLanguage) {
    this.queryLanguage = queryLanguage;
  }

  public String getExpressionLanguage() {
    return expressionLanguage;
  }

  public void setExpressionLanguage(String expressionLanguage) {
    this.expressionLanguage = expressionLanguage;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public Set getNamespaces() {
    return namespaces;
  }

  public Namespace getNamespace(String prefix, String uri) {
    for (Iterator i = namespaces.iterator(); i.hasNext();) {
      Namespace namespace = (Namespace) i.next();
      if (prefix.equals(namespace.getPrefix()) && uri.equals(namespace.getURI()))
        return namespace;
    }
    return null;
  }

  public Namespace addNamespace(Namespace namespace) {
    if (!namespaces.add(namespace)) {
      // namespace already exists, get the original one
      namespace = getNamespace(namespace.getPrefix(), namespace.getURI());
    }
    return namespace;
  }

  public Namespace addNamespace(String prefix, String URI) {
    Namespace namespace = getNamespace(prefix, URI);
    if (namespace == null) {
      namespace = new Namespace(prefix, URI);
      namespaces.add(namespace);
    }
    return namespace;
  }

  public Set addNamespaces(Set namespaces) {
    HashSet internNamespaces = new HashSet();
    for (Iterator i = namespaces.iterator(); i.hasNext();) {
      Namespace namespace = (Namespace) i.next();
      Namespace internNamespace = addNamespace(namespace);
      internNamespaces.add(internNamespace);
    }
    return internNamespaces;
  }

  public Set addNamespaces(Map namespaceMap) {
    HashSet internNamespaces = new HashSet();
    Iterator namespaceEntryIt = namespaceMap.entrySet().iterator();
    while (namespaceEntryIt.hasNext()) {
      Entry namespaceEntry = (Entry) namespaceEntryIt.next();
      Namespace internNamespace = addNamespace((String) namespaceEntry.getKey(),
          (String) namespaceEntry.getValue());
      internNamespaces.add(internNamespace);
    }
    return internNamespaces;
  }

  private static List readModuleClasses() {
    // get configured modules resource
    String resource = JbpmConfiguration.Configs.getString("resource.bpel.modules");

    // parse modules document
    Properties modulesProperties;
    try {
      modulesProperties = loadProperties(resource);
    }
    catch (IOException e) {
      log.warn("could not read bpel modules document: " + resource);
      return Collections.EMPTY_LIST;
    }

    // walk through property names
    ArrayList moduleClasses = new ArrayList();
    for (Iterator i = modulesProperties.keySet().iterator(); i.hasNext();) {
      String moduleName = (String) i.next();

      // load module class
      Class moduleClass = ClassLoaderUtil.loadClass(moduleName);

      // validate module class
      if (!ModuleDefinition.class.isAssignableFrom(moduleClass)) {
        log.warn("not a module definition: " + moduleClass);
        continue;
      }

      // register module class
      moduleClasses.add(moduleClass);
      log.debug("registered module class: " + moduleClass.getName());
    }
    return moduleClasses;
  }

  private static Properties loadProperties(String resource) throws IOException {
    InputStream resourceStream = ClassLoaderUtil.getStream(resource);
    if (resourceStream == null)
      throw new FileNotFoundException(resource);
    try {
      Properties properties = new Properties();
      properties.load(resourceStream);
      return properties;
    }
    finally {
      resourceStream.close();
    }
  }
}
