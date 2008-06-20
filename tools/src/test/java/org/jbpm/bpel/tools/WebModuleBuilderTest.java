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

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.jpdl.par.ProcessArchive;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/10/17 22:02:51 $
 */
public class WebModuleBuilderTest extends TestCase {

  private static WebModuleBuilder moduleBuilder;
  private static ZipFile moduleZip;

  public void testGeneratedFiles() throws IOException {
    String wsdlPath = WebModuleBuilder.DIR_WEB_INF
        + WebModuleBuilder.SEPARATOR
        + WebModuleBuilder.DIR_WSDL
        + WebModuleBuilder.SEPARATOR;

    // wsdl service file
    String serviceEntry = wsdlPath + moduleBuilder.getWsdlServiceFileName();
    assertNotNull(moduleZip.getEntry(serviceEntry));

    // wsdl binding files
    String bindingEntryPrefix = wsdlPath + moduleBuilder.getWsdlBindingFilesPrefix();
    int bindingCount = 0;
    for (Enumeration e = moduleZip.entries(); e.hasMoreElements();) {
      ZipEntry entry = (ZipEntry) e.nextElement();
      if (entry.getName().startsWith(bindingEntryPrefix))
        bindingCount++;
    }
    assertEquals(1, bindingCount);

    // xml schema files
    int schemaCount = 0;
    for (Enumeration e = moduleZip.entries(); e.hasMoreElements();) {
      ZipEntry entry = (ZipEntry) e.nextElement();
      String entryName = entry.getName();
      if (entryName.startsWith(wsdlPath) && entryName.endsWith(".xsd"))
        schemaCount++;
    }
    assertEquals(1, schemaCount);

    // deployment descriptor file
    String deploymentDescriptorEntry = WebModuleBuilder.DIR_WEB_INF
        + WebModuleBuilder.SEPARATOR
        + WebModuleBuilder.DIR_CLASSES
        + WebModuleBuilder.SEPARATOR
        + moduleBuilder.getDeploymentDescriptorFile().getName();
    assertNotNull(moduleZip.getEntry(deploymentDescriptorEntry));

    // jax-rpc mapping file
    String jaxrpcMappingEntry = WebModuleBuilder.DIR_WEB_INF
        + WebModuleBuilder.SEPARATOR
        + moduleBuilder.getJaxrpcMappingFile().getName();
    assertNotNull(moduleZip.getEntry(jaxrpcMappingEntry));

    // classes
    String classesPath = WebModuleBuilder.DIR_WEB_INF
        + WebModuleBuilder.SEPARATOR
        + WebModuleBuilder.DIR_CLASSES
        + WebModuleBuilder.SEPARATOR
        + moduleBuilder.generateJavaMappingPackage().replace('.', WebModuleBuilder.SEPARATOR);
    int classCount = 0;
    for (Enumeration entries = moduleZip.entries(); entries.hasMoreElements();) {
      ZipEntry entry = (ZipEntry) entries.nextElement();
      String entryName = entry.getName();
      if (entryName.startsWith(classesPath) && entryName.endsWith(".class"))
        classCount++;
    }
    assertEquals(12, classCount);

    // web services deployment descriptor
    String webservicesEntry = WebModuleBuilder.DIR_WEB_INF
        + WebModuleBuilder.SEPARATOR
        + moduleBuilder.getWebServicesDescriptorFile().getName();
    assertNotNull(moduleZip.getEntry(webservicesEntry));
  }

  public void testUserProvidedFiles() throws IOException {
    // web app deployment descriptor
    String webEntry = WebModuleBuilder.DIR_WEB_INF
        + WebModuleBuilder.SEPARATOR
        + moduleBuilder.getWebAppDescriptorFile().getName();
    assertNotNull(moduleZip.getEntry(webEntry));
  }

  public static Test suite() {
    return new Setup();
  }

  private static class Setup extends TestSetup {

    Setup() {
      super(new TestSuite(WebModuleBuilderTest.class));
    }

    protected void setUp() throws Exception {
      // build process archive
      InputStream processStream = WebModuleBuilderTest.class.getResourceAsStream("process.zip");
      try {
        // load process archive
        ProcessArchive processArchive = new ProcessArchive(new ZipInputStream(processStream));
        // read process definition
        BpelProcessDefinition processDefinition = (BpelProcessDefinition) processArchive.parseProcessDefinition();
        // call web module generator
        moduleBuilder = new WebModuleBuilder();
        moduleBuilder.buildModule(processDefinition);
        assertEquals(0, moduleBuilder.getProblemHandler().getProblemCount());
        // read module file
        moduleZip = new ZipFile(moduleBuilder.getModuleFile());
      }
      finally {
        processStream.close();
      }
    }

    protected void tearDown() throws Exception {
      moduleZip.close();
      // comment if you want to see the generated file
      moduleBuilder.getModuleFile().delete();
    }
  }
}
