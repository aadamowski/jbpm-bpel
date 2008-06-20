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
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.security.CodeSource;

import javax.xml.rpc.Service;
import javax.xml.soap.SOAPMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sun.xml.rpc.tools.wscompile.CompileTool;

import org.jbpm.bpel.xml.ProblemCounter;
import org.jbpm.bpel.xml.ProblemHandler;
import org.jbpm.bpel.xml.util.XmlUtil;
import org.jbpm.jpdl.xml.Problem;

/**
 * Adapter for the <code>wscompile</code> tool, part of the Java Web Services Development Pack.
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2008/02/05 05:37:34 $
 */
public class WscompileTool implements JavaMappingTool {

  private String packageName = DEFAULT_PACKAGE_NAME;
  private File wsdlFile = DEFAULT_WSDL_FILE;

  private File classesDirectory = DEFAULT_CLASSES_DIRECTORY;
  private File jaxrpcMappingFile = DEFAULT_JAXRPC_MAPPING_FILE;

  private ProblemHandler problemHandler = new ProblemCounter();

  static final String NS_WSCOMPILE = "http://java.sun.com/xml/ns/jax-rpc/ri/config";
  static final String ELEM_CONFIGURATION = "configuration";
  static final String ELEM_WSDL = "wsdl";
  static final String ATTR_LOCATION = "location";
  static final String ATTR_PACKAGE_NAME = "packageName";

  static final String DEFAULT_PACKAGE_NAME = "org.tempuri";
  static final File DEFAULT_WSDL_FILE = new File(WsdlServiceTool.DEFAULT_WSDL_DIRECTORY,
      WsdlServiceTool.DEFAULT_SERVICE_FILE_NAME);

  static final File DEFAULT_CLASSES_DIRECTORY = new File("classes/");
  static final File DEFAULT_JAXRPC_MAPPING_FILE = new File("jaxrpc-mapping.xml");

  private static final Log log = LogFactory.getLog(WscompileTool.class);

  /**
   * Returns the input WSDL file.
   */
  public File getWsdlFile() {
    return wsdlFile;
  }

  public void setWsdlFile(File wsdlFile) {
    if (wsdlFile == null)
      throw new IllegalArgumentException("wsdl file cannot be null");

    this.wsdlFile = wsdlFile;
  }

  /**
   * Returns the package of the generated classes.
   */
  public String getPackageName() {
    return packageName;
  }

  public void setPackageName(String packageName) {
    if (packageName == null)
      throw new IllegalArgumentException("package name cannot be null");

    this.packageName = packageName;
  }

  /**
   * Returns where to place generated classes.
   */
  public File getClassesDirectory() {
    return classesDirectory;
  }

  public void setClassesDirectory(File classesDirectory) {
    if (classesDirectory == null)
      throw new IllegalArgumentException("classes directory cannot be null");

    this.classesDirectory = classesDirectory;
  }

  /**
   * Returns where to write the JAX-RPC mapping file.
   */
  public File getJaxrpcMappingFile() {
    return jaxrpcMappingFile;
  }

  public void setJaxrpcMappingFile(File jaxrpcMappingFile) {
    if (jaxrpcMappingFile == null)
      throw new IllegalArgumentException("jax-rpc mapping file cannot be null");

    this.jaxrpcMappingFile = jaxrpcMappingFile;
  }

  /**
   * Returns the problem handler to be used for reporting errors.
   */
  public ProblemHandler getProblemHandler() {
    return problemHandler;
  }

  public void setProblemHandler(ProblemHandler problemHandler) {
    if (problemHandler == null)
      throw new IllegalArgumentException("problem handler cannot be null");

    this.problemHandler = problemHandler;
  }

  public void generateJavaMapping() {
    Document configurationDoc = generateConfiguration();
    try {
      generateJavaMappingImpl(configurationDoc);
    }
    catch (IOException e) {
      problemHandler.add(new Problem(Problem.LEVEL_ERROR, "could not write configuration file", e));
    }
  }

  private void generateJavaMappingImpl(Document configurationDoc) throws IOException {
    File configurationFile = File.createTempFile("wscompile", ".xml");
    try {
      XmlUtil.writeFile(configurationDoc, configurationFile);
      log.debug("wrote configuration: " + configurationFile.getName());

      callWscompile(configurationFile);
    }
    finally {
      configurationFile.delete();
    }
  }

  /**
   * Generates the configuration file read by the <code>wscompile</code> tool. Said file contains
   * information that describes the web service.
   * @return the configuration file read by <code>wscompile</code>, as a DOM tree
   */
  protected Document generateConfiguration() {
    Document configurationDoc = XmlUtil.createDocument();

    // configuration
    Element configuration = configurationDoc.createElementNS(NS_WSCOMPILE, ELEM_CONFIGURATION);
    XmlUtil.addNamespaceDeclaration(configuration, NS_WSCOMPILE);

    // wsdl
    Element wsdl = configurationDoc.createElementNS(NS_WSCOMPILE, ELEM_WSDL);
    wsdl.setAttribute(ATTR_LOCATION, getWsdlFile().getAbsolutePath());
    wsdl.setAttribute(ATTR_PACKAGE_NAME, packageName);

    // assemble document
    configurationDoc.appendChild(configuration);
    configuration.appendChild(wsdl);

    return configurationDoc;
  }

  /**
   * Runs the <code>wscompile</code> tool, which obtains information that describes the web
   * service from the given configuration file.
   * @param configurationFile the configuration file read by <code>wscompile</code>
   */
  protected void callWscompile(File configurationFile) {
    String[] args = new String[10];
    int i = 0;

    // read a WSDL file, generate the service endpoint interface
    args[i++] = "-import";

    // do not generate RPC structures
    args[i++] = "-f:norpcstructures";

    // enable WSI-Basic Profile features
    args[i++] = "-f:wsi";

    // specify where to find input class files
    args[i++] = "-cp";
    args[i++] = formatClasspath();

    // specify where to place generated output files
    args[i++] = "-d";
    args[i++] = classesDirectory.getAbsolutePath();
    // ensure classes directory exists
    classesDirectory.mkdirs();

    // generate a J2EE mapping.xml file
    args[i++] = "-mapping";
    args[i++] = jaxrpcMappingFile.getAbsolutePath();

    // the tool reads a configuration file, which specifies a WSDL file
    args[i++] = configurationFile.getAbsolutePath();

    LogOutputStream out = new LogOutputStream(log);
    try {
      CompileTool tool = new CompileTool(out, "wscompile");
      if (tool.run(args))
        log.debug("wrote jax-rpc mapping file: " + jaxrpcMappingFile.getName());
      else
        problemHandler.add(new Problem(Problem.LEVEL_ERROR, "java mapping generation failed"));
    }
    finally {
      out.close();
    }
  }

  private static String formatClasspath() {
    // include the jax-rpc and saaj classes in the classpath
    String jaxrpcLocation = getLocation(Service.class);
    String saajLocation = getLocation(SOAPMessage.class);
    String classpath = jaxrpcLocation != null ? (saajLocation != null ? jaxrpcLocation
        + File.pathSeparator
        + saajLocation : jaxrpcLocation) : (saajLocation != null ? saajLocation : "");
    log.debug("using classpath: " + classpath);
    return classpath;
  }

  /**
   * Gets the directory, JAR file or remote URL from which the given class was loaded.
   * @param c the class to be located
   * @return the location from which the class was loaded, or <code>null</code> if the origin of
   * the class is unknown
   */
  private static String getLocation(Class c) {
    CodeSource codeSource = c.getProtectionDomain().getCodeSource();
    if (codeSource == null) {
      /*
       * The code source of a domain may be null. This is the case for classes included in the Java
       * platform. For example, the javax.xml.soap package is part of Java SE 6. Not much more can
       * be done here, so we just tell the caller the location is unknown.
       * 
       * Thanks to Bernd Ruecker of Camunda GmbH for catching this one.
       */
      return null;
    }
    URL url = codeSource.getLocation();

    if ("jar".equals(url.getProtocol())) {
      try {
        JarURLConnection urlConnection = (JarURLConnection) url.openConnection();
        url = urlConnection.getJarFileURL();
      }
      catch (IOException e) {
        log.debug("could not open connection to " + url, e);
      }
    }

    if (!"file".equals(url.getProtocol()))
      return url.toString();

    String fileName = url.getFile();
    try {
      fileName = URLDecoder.decode(fileName, "UTF-8");
    }
    catch (UnsupportedEncodingException e) {
      log.debug("UTF-8 not supported", e);
    }
    return new File(fileName).getAbsolutePath();
  }

  /**
   * Cleans the JAX-RPC {@linkplain #getJaxrpcMappingFile() mapping file} and the
   * {@linkplain #getClassesDirectory() classes directory} referenced by this tool.
   */
  public void clean() {
    // recursively delete classes directory
    if (FileUtil.clean(classesDirectory))
      log.info("deleted: " + classesDirectory);
    // delete mapping file
    if (jaxrpcMappingFile.delete())
      log.info("deleted: " + jaxrpcMappingFile);
  }
}
