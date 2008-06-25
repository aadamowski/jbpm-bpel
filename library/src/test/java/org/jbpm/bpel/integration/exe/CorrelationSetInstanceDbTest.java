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
package org.jbpm.bpel.integration.exe;

import java.util.Collections;

import javax.xml.namespace.QName;

import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.integration.def.CorrelationSetDefinition;
import org.jbpm.bpel.persistence.db.AbstractDbTestCase;
import org.jbpm.bpel.wsdl.Property;
import org.jbpm.bpel.wsdl.impl.PropertyImpl;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/08/08 11:10:31 $
 */
public class CorrelationSetInstanceDbTest extends AbstractDbTestCase {

  public void testDefinition() {
    // correlation set definition
    CorrelationSetDefinition correlationDefinition = new CorrelationSetDefinition();
    correlationDefinition.setName("cset");

    // process definition
    BpelProcessDefinition processDefinition = new BpelProcessDefinition(
        "definition", BpelConstants.NS_EXAMPLES);
    processDefinition.getGlobalScope().addCorrelationSet(correlationDefinition);

    graphSession.saveProcessDefinition(processDefinition);

    // process instance
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    Token token = processInstance.getRootToken();

    // correlation set instance
    CorrelationSetInstance correlationInstance = correlationDefinition.createInstance(token);

    processInstance = saveAndReload(processInstance);
    processDefinition = getProcessDefinition(processInstance);
    correlationDefinition = processDefinition.getGlobalScope()
        .getCorrelationSet("cset");
    correlationInstance = correlationDefinition.getInstance(processInstance.getRootToken());

    assertEquals(correlationDefinition, correlationInstance.getDefinition());
  }

  public void testProperties() throws Exception {
    // read property
    QName Q_ARTIST_PROP = new QName(BpelConstants.NS_EXAMPLES,
        "tns:artistProperty");
    Property property = new PropertyImpl();
    property.setQName(Q_ARTIST_PROP);

    // correlation set definition
    CorrelationSetDefinition correlationDefinition = new CorrelationSetDefinition();
    correlationDefinition.setName("cset");
    correlationDefinition.addProperty(property);

    // process definition
    BpelProcessDefinition processDefinition = new BpelProcessDefinition(
        "definition", BpelConstants.NS_EXAMPLES);
    processDefinition.getImportDefinition().addProperty(property);
    processDefinition.getGlobalScope().addCorrelationSet(correlationDefinition);

    graphSession.saveProcessDefinition(processDefinition);

    // process instance
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    Token token = processInstance.getRootToken();

    // correlation set instance
    CorrelationSetInstance correlationInstance = correlationDefinition.createInstance(token);
    correlationInstance.initialize(Collections.singletonMap(Q_ARTIST_PROP,
        "Clash, The"));

    processInstance = saveAndReload(processInstance);
    processDefinition = getProcessDefinition(processInstance);
    correlationDefinition = processDefinition.getGlobalScope()
        .getCorrelationSet("cset");
    correlationInstance = correlationDefinition.getInstance(processInstance.getRootToken());

    assertEquals("Clash, The", correlationInstance.getProperty(Q_ARTIST_PROP));
  }

  private BpelProcessDefinition getProcessDefinition(
      ProcessInstance processInstance) {
    long processDefinitionId = processInstance.getProcessDefinition().getId();
    return (BpelProcessDefinition) session.load(BpelProcessDefinition.class,
        new Long(processDefinitionId));
  }
}
