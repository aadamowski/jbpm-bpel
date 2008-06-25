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

import java.util.Set;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.jbpm.bpel.graph.basic.Assign;
import org.jbpm.bpel.graph.basic.assign.Copy;
import org.jbpm.bpel.graph.basic.assign.FromElement;
import org.jbpm.bpel.graph.basic.assign.FromExpression;
import org.jbpm.bpel.graph.basic.assign.FromPartnerLink;
import org.jbpm.bpel.graph.basic.assign.FromProperty;
import org.jbpm.bpel.graph.basic.assign.FromText;
import org.jbpm.bpel.graph.basic.assign.FromVariable;
import org.jbpm.bpel.graph.basic.assign.ToExpression;
import org.jbpm.bpel.graph.basic.assign.ToPartnerLink;
import org.jbpm.bpel.graph.basic.assign.ToProperty;
import org.jbpm.bpel.graph.basic.assign.ToVariable;
import org.jbpm.bpel.graph.basic.assign.FromPartnerLink.Reference;
import org.jbpm.bpel.graph.def.Namespace;
import org.jbpm.bpel.integration.def.PartnerLinkDefinition;
import org.jbpm.bpel.sublang.def.Expression;
import org.jbpm.bpel.sublang.def.VariableQuery;

/**
 * @author Juan Cantú
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/09/12 23:20:21 $
 */
public class AssignReaderTest extends AbstractReaderTestCase {

  public void testFromExpression() throws Exception {
    String xml = "<assign xmlns:a='urn:a'>"
        + "<copy xmlns:c='urn:c'>"
        + " <from xmlns:f='urn:f'>$v/p/q</from>"
        + " <to>$v/p</to>"
        + "</copy>"
        + "</assign>";
    Assign assign = (Assign) readActivity(xml);
    Copy copy = (Copy) assign.getOperations().get(0);
    Expression expr = ((FromExpression) copy.getFrom()).getExpression();
    assertEquals("$v/p/q", expr.getText());
    Set namespaces = expr.getNamespaces();
    assertTrue(namespaces.contains(new Namespace("a", "urn:a")));
    assertTrue(namespaces.contains(new Namespace("c", "urn:c")));
    assertTrue(namespaces.contains(new Namespace("f", "urn:f")));
  }

  public void testFromVariable() throws Exception {
    String xml = "<assign>"
        + "<copy>"
        + " <from variable='iv' />"
        + " <to>$v/p</to>"
        + "</copy>"
        + "</assign>";
    initMessageProperties();
    Assign assign = (Assign) readActivity(xml);
    Copy copy = (Copy) assign.getOperations().get(0);
    FromVariable from = (FromVariable) copy.getFrom();
    assertSame(messageVariable, from.getVariable());
  }

  public void testFromVariablePart() throws Exception {
    String xml = "<assign>"
        + "<copy>"
        + " <from variable='iv' part='p' />"
        + " <to>$v/p</to>"
        + "</copy>"
        + "</assign>";
    initMessageProperties();
    Assign assign = (Assign) readActivity(xml);
    Copy copy = (Copy) assign.getOperations().get(0);
    FromVariable from = (FromVariable) copy.getFrom();
    assertSame(messageVariable, from.getVariable());
    assertEquals("p", from.getPart());
  }

  public void testFromVariableQuery() throws Exception {
    String xml = "<assign xmlns:a='urn:a'>"
        + "<copy xmlns:c='urn:c'>"
        + " <from xmlns:f='urn:f' variable='iv' part='p'>"
        + "  <query>q</query>"
        + " </from>"
        + " <to>$v/p</to>"
        + "</copy>"
        + "</assign>";
    initMessageProperties();
    Assign assign = (Assign) readActivity(xml);
    Copy copy = (Copy) assign.getOperations().get(0);
    FromVariable from = (FromVariable) copy.getFrom();
    // variable
    assertSame(messageVariable, from.getVariable());
    // part
    assertEquals("p", from.getPart());
    // query
    VariableQuery query = from.getQuery();
    assertEquals("q", query.getText());
    Set namespaces = query.getNamespaces();
    assertTrue(namespaces.contains(new Namespace("a", "urn:a")));
    assertTrue(namespaces.contains(new Namespace("c", "urn:c")));
    assertTrue(namespaces.contains(new Namespace("f", "urn:f")));
  }

  public void testFromVariableProperty() throws Exception {
    String xml = "<assign xmlns:tns='http://manufacturing.org/wsdl/purchase'>"
        + "<copy>"
        + " <from variable='iv' property='tns:p1'/>"
        + " <to>$v/p</to>"
        + "</copy>"
        + "</assign>";
    initMessageProperties();
    Assign assign = (Assign) readActivity(xml);
    Copy copy = (Copy) assign.getOperations().get(0);
    FromProperty from = (FromProperty) copy.getFrom();
    assertEquals("iv", from.getVariable().getName());
    assertEquals(new QName(NS_TNS, "p1"), from.getProperty().getQName());
  }

  public void testFromLiteral_element() throws Exception {
    String xml = "<assign>"
        + "<copy>"
        + " <from>\n"
        + "  <literal>"
        + "   <order name='o' xmlns='http://manufacturing.org/xsd/purchase'/>\n"
        + "  </literal>"
        + " </from>"
        + " <to>$v/p</to>"
        + "</copy>"
        + "</assign>";
    Assign assign = (Assign) readActivity(xml);
    Copy copy = (Copy) assign.getOperations().get(0);
    FromElement from = (FromElement) copy.getFrom();
    Element literal = from.getLiteral();
    assertEquals("http://manufacturing.org/xsd/purchase", literal.getNamespaceURI());
    assertEquals("order", literal.getLocalName());
    assertEquals("o", literal.getAttribute("name"));
  }

  public void testFromLiteral_text() throws Exception {
    String xml = "<assign>"
        + "<copy>"
        + " <from>\n"
        + "  <literal>\n"
        + "   plain text\n"
        + "  </literal>\n"
        + " </from>"
        + " <to>$v/p</to>"
        + "</copy>"
        + "</assign>";
    Assign assign = (Assign) readActivity(xml);
    Copy copy = (Copy) assign.getOperations().get(0);
    FromText from = (FromText) copy.getFrom();
    assertEquals("\n   plain text\n  ", from.getLiteral());
  }

  public void testFromLiteral_cdata() throws Exception {
    String xml = "<assign>"
        + "<copy>"
        + " <from>\n"
        + "  <literal>\n"
        + "   <![CDATA[<<hello, friends & foes>>]]>\n"
        + "  </literal>\n"
        + " </from>"
        + " <to>$v/p</to>"
        + "</copy>"
        + "</assign>";
    Assign assign = (Assign) readActivity(xml);
    Copy copy = (Copy) assign.getOperations().get(0);
    FromText from = (FromText) copy.getFrom();
    assertEquals("\n   <<hello, friends & foes>>\n  ", from.getLiteral());
  }

  public void testFromLiteral_noChildren() throws Exception {
    String xml = "<assign>"
        + "<copy>"
        + " <from><literal/></from>"
        + " <to>$v/p</to>"
        + "</copy>"
        + "</assign>";
    Assign assign = (Assign) readActivity(xml);
    Copy copy = (Copy) assign.getOperations().get(0);
    FromText from = (FromText) copy.getFrom();
    assertEquals("", from.getLiteral());
  }

  public void testFromPartnerLink() throws Exception {
    PartnerLinkDefinition pl = new PartnerLinkDefinition();
    pl.setName("pl");
    scope.addPartnerLink(pl);
    String xml = "<assign>"
        + "<copy>"
        + " <from partnerLink='pl' endpointReference='myRole'/>"
        + " <to>$v/p</to>"
        + "</copy>"
        + "</assign>";
    Assign assign = (Assign) readActivity(xml);
    Copy copy = (Copy) assign.getOperations().get(0);
    FromPartnerLink from = (FromPartnerLink) copy.getFrom();
    assertEquals("pl", from.getPartnerLink().getName());
    assertEquals(Reference.MY_ROLE, from.getEndpointReference());
  }

  public void testToQuery() throws Exception {
    String xml = "<assign>"
        + "<copy>"
        + " <from>$v/p</from>"
        + " <to>$v/p/q</to>"
        + "</copy>"
        + "</assign>";
    Assign assign = (Assign) readActivity(xml);
    Copy copy = (Copy) assign.getOperations().get(0);
    assertEquals("$v/p/q", ((ToExpression) copy.getTo()).getExpression().getText());
  }

  public void testToVariable() throws Exception {
    String xml = "<assign>"
        + "<copy>"
        + " <from>$v/p</from>"
        + " <to variable='iv'/>"
        + "</copy>"
        + "</assign>";
    initMessageProperties();
    Assign assign = (Assign) readActivity(xml);
    Copy copy = (Copy) assign.getOperations().get(0);
    ToVariable to = (ToVariable) copy.getTo();
    assertSame(messageVariable, to.getVariable());
  }

  public void testToVariablePart() throws Exception {
    String xml = "<assign>"
        + "<copy>"
        + " <from>$v/p</from>"
        + " <to variable='iv' part='p' />"
        + "</copy>"
        + "</assign>";
    initMessageProperties();
    Assign assign = (Assign) readActivity(xml);
    Copy copy = (Copy) assign.getOperations().get(0);
    ToVariable to = (ToVariable) copy.getTo();
    assertSame(messageVariable, to.getVariable());
    assertEquals("p", to.getPart());
  }

  public void testToVariableQuery() throws Exception {
    String xml = "<assign xmlns:a='urn:a'>"
        + "<copy xmlns:c='urn:c'>"
        + " <from>$v/p</from>"
        + " <to xmlns:f='urn:f' variable='iv' part='p'>"
        + "  <query>q</query>"
        + " </to>"
        + "</copy>"
        + "</assign>";
    initMessageProperties();
    Assign assign = (Assign) readActivity(xml);
    Copy copy = (Copy) assign.getOperations().get(0);
    ToVariable to = (ToVariable) copy.getTo();
    // variable
    assertSame(messageVariable, to.getVariable());
    // part
    assertEquals("p", to.getPart());
    // query
    VariableQuery query = to.getQuery();
    assertEquals("q", query.getText());
    Set namespaces = query.getNamespaces();
    assertTrue(namespaces.contains(new Namespace("a", "urn:a")));
    assertTrue(namespaces.contains(new Namespace("c", "urn:c")));
    assertTrue(namespaces.contains(new Namespace("f", "urn:f")));
  }

  public void testToVariableProperty() throws Exception {
    initMessageProperties();
    String xml = "<assign xmlns:tns='http://manufacturing.org/wsdl/purchase'>"
        + "<copy>"
        + " <from>$v/p</from>"
        + " <to variable='iv' property='tns:p2'/>"
        + "</copy>"
        + "</assign>";
    Assign assign = (Assign) readActivity(xml);
    Copy copy = (Copy) assign.getOperations().get(0);
    ToProperty to = (ToProperty) copy.getTo();
    assertEquals("iv", to.getVariable().getName());
    assertEquals(new QName(NS_TNS, "p2"), to.getProperty().getQName());
  }

  public void testToPartnerLink() throws Exception {
    PartnerLinkDefinition pl = new PartnerLinkDefinition();
    pl.setName("pl");
    scope.addPartnerLink(pl);
    String xml = "<assign>"
        + "<copy>"
        + " <from>$v/p</from>"
        + " <to partnerLink='pl'/>"
        + "</copy>"
        + "</assign>";
    Assign assign = (Assign) readActivity(xml);
    Copy copy = (Copy) assign.getOperations().get(0);
    ToPartnerLink to = (ToPartnerLink) copy.getTo();
    assertEquals("pl", to.getPartnerLink().getName());
  }
}