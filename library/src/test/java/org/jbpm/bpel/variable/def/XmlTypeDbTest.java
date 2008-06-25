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
package org.jbpm.bpel.variable.def;

import javax.xml.namespace.QName;

import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.persistence.db.AbstractDbTestCase;
import org.jbpm.bpel.wsdl.Property;
import org.jbpm.bpel.wsdl.PropertyAlias;
import org.jbpm.bpel.wsdl.impl.PropertyAliasImpl;
import org.jbpm.bpel.wsdl.impl.PropertyImpl;
import org.jbpm.bpel.xml.BpelConstants;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/10/13 02:53:28 $
 */
public class XmlTypeDbTest extends AbstractDbTestCase {

  private BpelProcessDefinition processDefinition;
  private SchemaType type;

  private static final QName TYPE_NAME = new QName(BpelConstants.NS_EXAMPLES, "st");

  protected void setUp() throws Exception {
    super.setUp();
    // process, create after opening jbpm context
    processDefinition = new BpelProcessDefinition("pd", BpelConstants.NS_EXAMPLES);
    // variable type
    type = processDefinition.getImportDefinition().getSchemaType(TYPE_NAME);
  }

  public void testName() {
    // save objects and load them back
    processDefinition = saveAndReload(processDefinition);
    type = processDefinition.getImportDefinition().getSchemaType(TYPE_NAME);

    // verify retrieved objects
    assertEquals(TYPE_NAME, type.getName());
  }

  public void testPropertyAliases() {
    // prepare persistent objects
    // property
    final QName propertyName = new QName(BpelConstants.NS_EXAMPLES, "p");
    Property property = new PropertyImpl();
    property.setQName(propertyName);
    processDefinition.getImportDefinition().addProperty(property);
    // alias
    PropertyAlias alias = new PropertyAliasImpl();
    alias.setProperty(property);
    alias.setType(TYPE_NAME);
    type.addPropertyAlias(alias);

    // save objects and load them back
    processDefinition = saveAndReload(processDefinition);
    type = processDefinition.getImportDefinition().getSchemaType(TYPE_NAME);

    // verify retrieved objects
    assertEquals(TYPE_NAME, type.getPropertyAlias(propertyName).getType());
  }
}