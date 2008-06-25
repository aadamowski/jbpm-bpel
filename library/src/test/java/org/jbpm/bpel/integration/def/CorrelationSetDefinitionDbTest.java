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
package org.jbpm.bpel.integration.def;

import javax.xml.namespace.QName;

import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.persistence.db.AbstractDbTestCase;
import org.jbpm.bpel.wsdl.Property;
import org.jbpm.bpel.wsdl.impl.PropertyImpl;
import org.jbpm.bpel.xml.BpelConstants;

/**
 * @author Juan Cantu
 * @version $Revision$ $Date: 2007/10/13 02:53:23 $
 */
public class CorrelationSetDefinitionDbTest extends AbstractDbTestCase {

  BpelProcessDefinition processDefinition;
  CorrelationSetDefinition csDefinition;

  protected void setUp() throws Exception {
    super.setUp();
    // correlation set
    csDefinition = new CorrelationSetDefinition();
    csDefinition.setName("cs");
    // process
    processDefinition = new BpelProcessDefinition("pd", BpelConstants.NS_EXAMPLES);
    processDefinition.getGlobalScope().addCorrelationSet(csDefinition);
  }

  public void testName() {
    processDefinition = saveAndReload(processDefinition);
    assertEquals("cs", getCorrelationSetDefinition().getName());
  }

  public void testProperties() {
    Property property = new PropertyImpl();
    property.setQName(new QName("aQName"));
    property.setType(new QName("aType"));

    csDefinition.addProperty(property);

    processDefinition.getImportDefinition().addProperty(property);

    processDefinition = saveAndReload(processDefinition);

    assertNotNull(getCorrelationSetDefinition().getProperties());
  }

  private CorrelationSetDefinition getCorrelationSetDefinition() {
    return processDefinition.getGlobalScope().getCorrelationSet("cs");
  }
}
