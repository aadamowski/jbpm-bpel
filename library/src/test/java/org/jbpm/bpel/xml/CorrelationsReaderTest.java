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

import java.util.Map;

import javax.wsdl.Message;

import org.w3c.dom.Element;

import org.jbpm.bpel.integration.def.Correlation;
import org.jbpm.bpel.integration.def.CorrelationSetDefinition;
import org.jbpm.bpel.integration.def.Correlations;
import org.jbpm.bpel.integration.def.Correlation.Initiate;
import org.jbpm.bpel.variable.def.MessageType;
import org.jbpm.bpel.wsdl.PropertyAlias;
import org.jbpm.bpel.wsdl.impl.PropertyAliasImpl;

/**
 * @author Juan Cantu
 * @version $Revision$ $Date: 2006/09/27 03:53:01 $
 */
public class CorrelationsReaderTest extends AbstractReaderTestCase {

  CorrelationSetDefinition initiatedSet;
  CorrelationSetDefinition followedSet;
  CorrelationSetDefinition defaultSet;

  public void setUp() throws Exception {
    super.setUp();
    initMessageProperties();
    // correlation sets
    initiatedSet = new CorrelationSetDefinition();
    initiatedSet.setName("initiated");
    initiatedSet.addProperty(p1);
    followedSet = new CorrelationSetDefinition();
    followedSet.setName("followed");
    followedSet.addProperty(p2);
    defaultSet = new CorrelationSetDefinition();
    defaultSet.setName("default");
    defaultSet.addProperty(p3);
    // property aliases
    MessageType messageType = (MessageType) messageVariable.getType();
    Message message = messageType.getMessage();
    PropertyAlias alias = new PropertyAliasImpl();
    alias.setMessage(message);
    alias.setProperty(p1);
    messageType.addPropertyAlias(alias);
    alias = new PropertyAliasImpl();
    alias.setMessage(message);
    alias.setProperty(p2);
    messageType.addPropertyAlias(alias);
    alias = new PropertyAliasImpl();
    alias.setMessage(message);
    alias.setProperty(p3);
    messageType.addPropertyAlias(alias);
    // declare sets in scope
    scope.addCorrelationSet(initiatedSet);
    scope.addCorrelationSet(followedSet);
    scope.addCorrelationSet(defaultSet);
  }

  public void testCorrelationsCount() throws Exception {
    String xml = "<correlations>"
        + "  <correlation set='initiated' initiate='yes'/> "
        + "  <correlation set='followed' initiate='no'/> "
        + "</correlations>";
    Correlations correlations = readCorrelations(xml);
    Map correlationMap = correlations.getCorrelations();
    assertEquals(2, correlationMap.size());
  }

  public void testInitiated() throws Exception {
    String xml = "<correlations>"
        + "  <correlation set='initiated' initiate='yes'/> "
        + "</correlations>";
    Correlations correlations = readCorrelations(xml);
    Map correlationMap = correlations.getCorrelations();
    // initiated correlation
    Correlation correlation = (Correlation) correlationMap.get("initiated");
    assertEquals(Initiate.YES, correlation.getInitiate());
  }

  public void testFollowed() throws Exception {
    String xml = "<correlations>"
        + "  <correlation set='followed' initiate='no'/> "
        + "</correlations>";
    Correlations correlations = readCorrelations(xml);
    Map correlationMap = correlations.getCorrelations();
    // followed correlation
    Correlation correlation = (Correlation) correlationMap.get("followed");
    assertEquals(Initiate.NO, correlation.getInitiate());
  }

  public void testDefault() throws Exception {
    String xml = "<correlations>"
        + " <correlation set='default'/> "
        + "</correlations>";
    Correlations correlations = readCorrelations(xml);
    Map correlationMap = correlations.getCorrelations();
    // followed correlation (default)
    Correlation correlation = (Correlation) correlationMap.get("default");
    assertEquals(Initiate.NO, correlation.getInitiate());
  }

  private Correlations readCorrelations(String xml) throws Exception {
    Element element = parseAsBpelElement(xml);
    return reader.readCorrelations(element, scope, messageVariable);
  }
}
