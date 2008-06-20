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

import javax.wsdl.Output;

import org.xml.sax.SAXException;

import com.ibm.wsdl.OutputImpl;

import org.jbpm.bpel.graph.basic.Reply;
import org.jbpm.bpel.integration.def.CorrelationSetDefinition;
import org.jbpm.bpel.integration.def.ReplyAction;
import org.jbpm.bpel.variable.def.MessageType;

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/10/13 02:53:24 $
 */
public class ReplyReaderTest extends AbstractReaderTestCase {

  protected void setUp() throws Exception {
    super.setUp();
    initMessageProperties();
  }

  public void testPartnerLink() throws Exception {
    String xml = "<reply partnerLink='aPartner' operation='o' variable='iv'/>";
    Reply reply = (Reply) readActivity(xml);
    assertEquals(partnerLink, ((ReplyAction) reply.getAction()).getPartnerLink());
  }

  public void testPortType() {
    String xml = "<reply partnerLink='aPartner' portType='tns:mpt' operation='o' variable='iv'"
        + " xmlns:tns='http://manufacturing.org/wsdl/purchase'/>";
    try {
      readActivity(xml);
    }
    catch (Exception e) {
      fail(e.toString());
    }
  }

  public void testPortTypeDefault() {
    String xml = "<reply partnerLink='aPartner' operation='o' variable='iv'/>";
    try {
      readActivity(xml);
    }
    catch (Exception e) {
      fail(e.toString());
    }
  }

  public void testPortTypeNotFound() throws SAXException {
    String xml = "<reply partnerLink='aPartner' portType='invalidPT' operation='o' variable='iv'/>";
    ProblemCollector collector = new ProblemCollector();
    reader.setProblemHandler(collector);
    readActivity(xml);
    assertFalse(
        "invoke parse must fail when portType doesn't match partnerRole's portType",
        collector.getProblems().isEmpty());
  }

  public void testOperation() throws Exception {
    String xml = "<reply partnerLink='aPartner' operation='o' variable='iv'/>";
    Reply reply = (Reply) readActivity(xml);
    assertEquals("o", ((ReplyAction) reply.getAction()).getOperation()
        .getName());
  }

  public void testVariable() throws Exception {
    MessageType typeInfo = (MessageType) messageVariable.getType();
    Output output = new OutputImpl();
    output.setMessage(typeInfo.getMessage());
    operation.setOutput(output);

    String xml = "<reply partnerLink='aPartner' operation='o' variable='iv'/>";
    Reply reply = (Reply) readActivity(xml);
    assertEquals(messageVariable,
        ((ReplyAction) reply.getAction()).getVariable());
  }

  public void testCorrelations() throws Exception {
    CorrelationSetDefinition set = new CorrelationSetDefinition();
    set.setName("corr");
    set.addProperty(p1);

    scope.addCorrelationSet(set);

    String correlationXml = "<reply partnerLink='aPartner' operation='o' variable='iv'>"
        + "	<correlations>"
        + "     <correlation set='corr'/> "
        + "	</correlations>"
        + "</reply>";
    Reply reply = (Reply) readActivity(correlationXml);

    assertNotNull(((ReplyAction) reply.getAction()).getCorrelations());
  }
}
