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
package org.jbpm.bpel.tools;

import java.io.File;

import org.jbpm.bpel.xml.ProblemHandler;

/**
 * Contract for tools that generate the Java representation of the services described in a WSDL
 * document.
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2008/02/05 05:37:34 $
 */
public interface JavaMappingTool {

  /**
   * Sets the input WSDL file.
   * @param wsdlFile the input WSDL file
   */
  public void setWsdlFile(File wsdlFile);

  /**
   * Specifies the package of the generated classes.
   * @param packageName the package of the generated classes
   */
  public void setPackageName(String packageName);

  /**
   * Specifies where to write the JAX-RPC mapping file.
   * @param jaxrpcMappingFile the JAX-RPC mapping file
   */
  public void setJaxrpcMappingFile(File jaxrpcMappingFile);

  /**
   * Specifies where to place generated classes.
   * @param classesDirectory the generated classes directory
   */
  public void setClassesDirectory(File classesDirectory);

  /**
   * Sets the {@linkplain ProblemHandler problem handler} to be used for reporting errors present in
   * the WSDL document to be read, or detected while generating the Java representation.
   * @param problemHandler the problem handler to be used for reporting errors
   */
  public void setProblemHandler(ProblemHandler problemHandler);

  /**
   * Generates the Java representation of the services described in the WSDL file.
   */
  public void generateJavaMapping();
}
