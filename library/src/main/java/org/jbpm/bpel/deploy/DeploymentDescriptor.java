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

import java.net.URL;

import org.xml.sax.InputSource;

import org.jbpm.JbpmContext;
import org.jbpm.bpel.BpelException;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.integration.catalog.ServiceCatalog;
import org.jbpm.bpel.persistence.db.BpelGraphSession;
import org.jbpm.bpel.xml.DeploymentDescriptorReader;
import org.jbpm.bpel.xml.ProblemCollector;
import org.jbpm.jpdl.xml.Problem;

/**
 * Binding of <tt>bpelApplication</tt> element.
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/11/02 16:46:17 $
 */
public class DeploymentDescriptor extends ScopeDescriptor {

  private String targetNamespace;
  private Integer version;
  private ServiceCatalog serviceCatalog;

  public static final String FILE_NAME = "bpel-deployment.xml";

  public String getTargetNamespace() {
    return targetNamespace;
  }

  public void setTargetNamespace(String targetNamespace) {
    this.targetNamespace = targetNamespace;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public ServiceCatalog getServiceCatalog() {
    return serviceCatalog;
  }

  public void setServiceCatalog(ServiceCatalog serviceCatalog) {
    this.serviceCatalog = serviceCatalog;
  }

  public BpelProcessDefinition findProcessDefinition(JbpmContext jbpmContext) {
    // this descriptor references a particular process definition
    String name = getName();
    String targetNamespace = getTargetNamespace();
    Integer version = getVersion();

    // use the given context to find the definition
    BpelGraphSession graphSession = BpelGraphSession.getContextInstance(jbpmContext);
    BpelProcessDefinition processDefinition;
    // check for a version indicator
    if (version != null) {
      // find a specific version of the process
      processDefinition = graphSession.findProcessDefinition(name, targetNamespace,
          version.intValue());
    }
    else {
      // just retrieve the latest version
      processDefinition = graphSession.findLatestProcessDefinition(name, targetNamespace);
    }

    // if no such process exist, halt
    if (processDefinition == null) {
      throw new BpelException("process not found: name="
          + name
          + ", targetNamespace="
          + targetNamespace
          + ", version="
          + version);
    }

    return processDefinition;
  }

  public void accept(DeploymentVisitor visitor) {
    visitor.visit(this);
  }

  public static DeploymentDescriptor readDeploymentDescriptor(JbpmContext jbpmContext) {
    String descriptorResource = "/" + FILE_NAME;

    // locate the descriptor resource using the context class loader
    URL descriptorUrl = Thread.currentThread().getContextClassLoader().getResource(
        descriptorResource);

    // was the descriptor found?
    if (descriptorUrl == null) {
      // fall back to the loader of this class
      descriptorUrl = DeploymentDescriptor.class.getResource(descriptorResource);

      // if the descriptor is really not there, halt
      if (descriptorUrl == null)
        throw new BpelException("could not find deployment descriptor: " + descriptorResource);
    }

    // get shared reader
    DeploymentDescriptorReader reader = new DeploymentDescriptorReader();

    // prepare custom error handling
    ProblemCollector problemHandler = new ProblemCollector(descriptorResource);
    reader.setProblemHandler(problemHandler);

    // parse content
    DeploymentDescriptor descriptor = new DeploymentDescriptor();
    reader.read(descriptor, new InputSource(descriptorUrl.toExternalForm()));

    // if the descriptor has errors, halt
    if (Problem.containsProblemsOfLevel(problemHandler.getProblems(), Problem.LEVEL_ERROR))
      throw new BpelException("could not read deployment descriptor: " + descriptorUrl);

    return descriptor;
  }
}