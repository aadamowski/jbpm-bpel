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
package org.jbpm.bpel.wsdl.impl;

import javax.xml.namespace.QName;

import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.persistence.db.AbstractDbTestCase;
import org.jbpm.bpel.wsdl.Property;
import org.jbpm.bpel.xml.BpelConstants;

/**
 * @author Juan Cantu
 * @version $Revision$ $Date: 2007/11/02 16:46:20 $
 */
public class PropertyImplDbTest extends AbstractDbTestCase {

  private BpelProcessDefinition processDefinition;
  private Property property;

  private static final QName PROPERTY_NAME = new QName("p");

  protected void setUp() throws Exception {
    super.setUp();
    // property
    property = new PropertyImpl();
    property.setQName(PROPERTY_NAME);
    // process definition
    processDefinition = new BpelProcessDefinition("pd", BpelConstants.NS_EXAMPLES);
    processDefinition.getImportDefinition().addProperty(property);
  }

  public void testQName() {
    processDefinition = saveAndReload(processDefinition);
    property = processDefinition.getImportDefinition().getProperty(PROPERTY_NAME);

    assertEquals(PROPERTY_NAME, property.getQName());
  }

  public void testType() {
    QName type = new QName("t");
    property.setType(type);

    processDefinition = saveAndReload(processDefinition);
    property = processDefinition.getImportDefinition().getProperty(PROPERTY_NAME);

    assertEquals(type, property.getType());
  }
}
