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

import junit.framework.TestCase;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import org.jbpm.bpel.xml.util.XmlUtil;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2008/02/05 05:37:34 $
 */
public class WscompileToolTest extends TestCase {

  private WscompileTool tool = new WscompileTool();

  protected void setUp() throws Exception {
    tool.setWsdlFile(FileUtil.toFile(getClass(), "service.wsdl"));
  }

  public void testGenerateJavaMappingConfiguration() throws SAXException, IOException {
    Document configurationDoc = tool.generateConfiguration();

    // <configuration>
    Element configuration = configurationDoc.getDocumentElement();
    assertEquals(WscompileTool.NS_WSCOMPILE, configuration.getNamespaceURI());
    assertEquals(WscompileTool.ELEM_CONFIGURATION, configuration.getLocalName());

    // <wsdl>
    Element wsdl = XmlUtil.getElement(configuration, WscompileTool.NS_WSCOMPILE,
        WscompileTool.ELEM_WSDL);
    File wsdlFile = tool.getWsdlFile();
    assertEquals(wsdlFile.getAbsolutePath(), wsdl.getAttribute(WscompileTool.ATTR_LOCATION));
    assertEquals(tool.getPackageName(), wsdl.getAttribute(WscompileTool.ATTR_PACKAGE_NAME));
  }

  public void testGenerateJavaMapping() {
    tool.generateJavaMapping();

    // jax-rpc mapping file
    File mappingFile = tool.getJaxrpcMappingFile();
    assertTrue(mappingFile.exists());

    // generated classes
    File classesDirectory = tool.getClassesDirectory();
    String[] packageSegments = tool.getPackageName().split("\\.");
    for (int i = 0; i < packageSegments.length; i++) {
      classesDirectory = new File(classesDirectory, packageSegments[i]);
      assertTrue(classesDirectory.exists());
    }
    assertEquals(13, classesDirectory.list().length);

    // comment out if you want to examine the generated files
    tool.clean();
  }
}
