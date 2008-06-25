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
package org.jbpm.bpel.tools.ant;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipInputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.tools.WsdlServiceTool;
import org.jbpm.bpel.xml.ProblemCollector;
import org.jbpm.jpdl.par.ProcessArchive;
import org.jbpm.jpdl.xml.Problem;

/**
 * Task adapter for the {@link WsdlServiceTool wsdlservice} tool.
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2008/02/05 10:29:36 $
 */
public class WsdlServiceTask extends Task {

  private File processArchive;

  private File wsdlDir;
  private String serviceFileName;
  private String bindingFilesPrefix;
  private String bindingFilesSuffix;

  private boolean generateDescriptor = true;
  private File descriptorFile;

  public void execute() throws BuildException {
    // read process archive
    ProcessArchive processArchive;
    try {
      processArchive = readProcessArchive();
    }
    catch (IOException e) {
      throw new BuildException("could not read process archive: " + this.processArchive, e);
    }

    // parse process definition
    BpelProcessDefinition processDefinition = (BpelProcessDefinition) processArchive.parseProcessDefinition();

    // halt on parser errors
    if (Problem.containsProblemsOfLevel(processArchive.getProblems(), Problem.LEVEL_ERROR))
      throw new BuildException("process definition is invalid");

    WsdlServiceTool tool = new WsdlServiceTool();

    // wsdl directory
    if (wsdlDir != null)
      tool.setWsdlDirectory(wsdlDir);

    // binding file prefix
    if (bindingFilesPrefix != null)
      tool.setBindingFilesPrefix(bindingFilesPrefix);

    // binding file suffix
    if (bindingFilesSuffix != null)
      tool.setBindingFilesSuffix(bindingFilesSuffix);

    // service file name
    if (serviceFileName != null)
      tool.setServiceFileName(serviceFileName);

    // descriptor file name
    if (!generateDescriptor)
      tool.setDeploymentDescriptorFile(null);
    else if (descriptorFile != null)
      tool.setDeploymentDescriptorFile(descriptorFile);

    // problem handler
    ProblemCollector collector = new ProblemCollector();
    tool.setProblemHandler(collector);

    // run tool
    tool.generateWsdlService(processDefinition);

    // halt on generation errors
    if (Problem.containsProblemsOfLevel(collector.getProblems(), Problem.LEVEL_ERROR))
      throw new BuildException(collector.getProblemCount() + " problems found");
  }

  private ProcessArchive readProcessArchive() throws IOException {
    InputStream processStream = new FileInputStream(processArchive);
    try {
      return new ProcessArchive(new ZipInputStream(processStream));
    }
    finally {
      processStream.close();
    }
  }

  public void setProcessArchive(File processArchive) {
    this.processArchive = processArchive;
  }

  public void setWsdlDir(File outputdir) {
    this.wsdlDir = outputdir;
  }

  public void setServiceFileName(String serviceFile) {
    this.serviceFileName = serviceFile;
  }

  public void setBindingFilesPrefix(String bindingFile) {
    this.bindingFilesPrefix = bindingFile;
  }

  public void setBindingFilesSuffix(String bindingfilessuffix) {
    this.bindingFilesSuffix = bindingfilessuffix;
  }

  public void setGenerateDescriptor(boolean deploymentEnabled) {
    this.generateDescriptor = deploymentEnabled;
  }

  public void setDescriptorFile(File deploymentFile) {
    this.descriptorFile = deploymentFile;
  }
}
