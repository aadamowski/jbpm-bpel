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
package org.jbpm.bpel.xml;

import java.io.StringReader;

import junit.framework.TestCase;

import org.xml.sax.InputSource;

import org.jbpm.bpel.graph.def.Import;
import org.jbpm.bpel.par.DefinitionDescriptor;
import org.jbpm.bpel.xml.util.XmlUtil;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/10/13 02:53:24 $
 */
public class DefinitionDescriptorReaderTest extends TestCase {

  private DefinitionDescriptorReader reader = DefinitionDescriptorReader.getInstance();

  public void testReadUri() {
    String locationUri = getClass().getResource("bpelDefinitionSample.xml").toString();
    DefinitionDescriptor definitionDescriptor = new DefinitionDescriptor();
    reader.read(definitionDescriptor, new InputSource(locationUri));

    assertEquals(0, reader.getProblemHandler().getProblemCount());
  }

  public void testDefinitionLocation() throws Exception {
    // read descriptor
    String text = "<bpelDefinition location='hello.bpel' xmlns='"
        + BpelConstants.NS_DEFINITION_DESCRIPTOR
        + "'/>";
    DefinitionDescriptor definitionDescriptor = new DefinitionDescriptor();
    reader.read(definitionDescriptor, new InputSource(new StringReader(text)));
    // assertions
    assertEquals("hello.bpel", definitionDescriptor.getLocation());
  }

  public void testDefinitionImports() throws Exception {
    // read descriptor
    String text = "<bpelDefinition location='bogus.bpel' xmlns='"
        + BpelConstants.NS_DEFINITION_DESCRIPTOR
        + "'>"
        + "  <imports>"
        + "    <wsdl location='bogus.wsdl' />"
        + "    <schema location='bogus.xsd' />"
        + "  </imports>"
        + "</bpelDefinition>";
    DefinitionDescriptor definitionDescriptor = new DefinitionDescriptor();
    reader.read(definitionDescriptor, new InputSource(new StringReader(text)));
    // assertions
    assertEquals(2, definitionDescriptor.getImports().size());
  }

  public void testImportNamespace() throws Exception {
    // read descriptor
    String text = "<wsdl namespace='" + BpelConstants.NS_EXAMPLES + "'/>";
    Import imp = reader.readImport(XmlUtil.parseText(text));
    // assertions
    assertEquals(BpelConstants.NS_EXAMPLES, imp.getNamespace());
  }

  public void testImportLocation() throws Exception {
    // read descriptor
    String text = "<wsdl location='hello.wsdl'/>";
    Import imp = reader.readImport(XmlUtil.parseText(text));
    // assertions
    assertEquals("hello.wsdl", imp.getLocation());
  }

  public void testImportTypeWsdl() throws Exception {
    // read descriptor
    String text = "<wsdl />";
    Import imp = reader.readImport(XmlUtil.parseText(text));
    // assertions
    assertEquals(Import.Type.WSDL, imp.getType());
  }

  public void testImportTypeXmlSchema() throws Exception {
    // read descriptor
    String text = "<schema />";
    Import imp = reader.readImport(XmlUtil.parseText(text));
    // assertions
    assertEquals(Import.Type.XML_SCHEMA, imp.getType());
  }
}
