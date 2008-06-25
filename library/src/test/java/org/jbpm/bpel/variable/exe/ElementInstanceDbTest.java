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
package org.jbpm.bpel.variable.exe;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.persistence.db.AbstractDbTestCase;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.bpel.xml.util.XmlUtil;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.exe.ProcessInstance;

/**
 * @author Juan Cantu
 * @version $Revision$ $Date: 2007/10/13 02:53:26 $
 */
public class ElementInstanceDbTest extends AbstractDbTestCase {

  public void testValue() throws SAXException {
    BpelProcessDefinition processDefinition = new BpelProcessDefinition("pd",
        BpelConstants.NS_EXAMPLES);
    graphSession.saveProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    ContextInstance contextInstance = processInstance.getContextInstance();
    contextInstance.createVariable("item", XmlUtil.parseText("<record artist='happy mondays'/>"));

    processInstance = saveAndReload(processInstance);

    Element item = (Element) processInstance.getContextInstance().getVariable("item");
    assertNull(item.getNamespaceURI());
    assertEquals("record", item.getLocalName());
    assertEquals("happy mondays", item.getAttribute("artist"));
  }
}
