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
package org.jbpm.bpel.graph.basic;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.ibm.wsdl.util.xml.DOMUtils;

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
import org.jbpm.bpel.graph.def.AbstractActivityDbTestCase;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.integration.def.PartnerLinkDefinition;
import org.jbpm.bpel.sublang.def.Expression;
import org.jbpm.bpel.sublang.def.VariableQuery;
import org.jbpm.bpel.variable.def.VariableDefinition;
import org.jbpm.bpel.wsdl.Property;
import org.jbpm.bpel.wsdl.impl.PropertyImpl;
import org.jbpm.bpel.xml.util.DatatypeUtil;
import org.jbpm.bpel.xml.util.XmlUtil;

/**
 * @author Juan Cantu
 * @version $Revision$ $Date: 2007/09/12 23:20:20 $
 */
public class AssignDbTest extends AbstractActivityDbTestCase {

  public void testFromVariable() {
    // prepare persistent objects
    // variable
    VariableDefinition variable = new VariableDefinition();
    variable.setName("RFQ");
    processDefinition.getGlobalScope().addVariable(variable);
    // query
    VariableQuery query = new VariableQuery();
    query.setText("/description");
    // from
    FromVariable from = new FromVariable();
    from.setVariable(variable);
    from.setPart("item");
    from.setQuery(query);
    // copy
    Copy copy = new Copy();
    copy.setFrom(from);
    // assign
    Assign assign = createAssign();
    assign.addOperation(copy);
    putAssign(processDefinition, assign);

    // save objects and load them back
    processDefinition = saveAndReload(processDefinition);
    assign = getAssign(processDefinition);
    copy = (Copy) assign.getOperations().get(0);
    from = (FromVariable) session.load(FromVariable.class, session.getIdentifier(copy.getFrom()));

    // verify retrieved objects
    // variable
    assertEquals(variable.getName(), from.getVariable().getName());
    // part
    assertEquals("item", from.getPart());
    // query
    assertEquals("/description", from.getQuery().getText());
  }

  public void testFromProperty() throws Exception {
    // prepare persistent objects
    // variable
    VariableDefinition variable = new VariableDefinition();
    variable.setName("RFQ");
    processDefinition.getGlobalScope().addVariable(variable);
    // property
    Property property = new PropertyImpl();
    property.setQName(new QName("providerId"));
    processDefinition.getImportDefinition().addProperty(property);
    // from
    FromProperty from = new FromProperty();
    from.setVariable(variable);
    from.setProperty(property);
    // copy
    Copy copy = new Copy();
    copy.setFrom(from);
    // assign
    Assign assign = createAssign();
    assign.addOperation(copy);
    putAssign(processDefinition, assign);

    // save objects and load them back
    processDefinition = saveAndReload(processDefinition);
    assign = getAssign(processDefinition);
    copy = (Copy) assign.getOperations().get(0);
    from = (FromProperty) session.load(FromProperty.class, session.getIdentifier(copy.getFrom()));

    // verify retrieved objects
    // variable
    assertEquals(variable.getName(), from.getVariable().getName());
    // property
    assertEquals(property.getQName(), from.getProperty().getQName());
  }

  public void testFromPartnerLink() throws Exception {
    // prepare persistent objects
    // partner link
    PartnerLinkDefinition partnerLink = new PartnerLinkDefinition();
    partnerLink.setName("flightScheduleService");
    processDefinition.getGlobalScope().addPartnerLink(partnerLink);
    // from
    FromPartnerLink from = new FromPartnerLink();
    from.setPartnerLink(partnerLink);
    from.setEndpointReference(Reference.PARTNER_ROLE);
    // copy
    Copy copy = new Copy();
    copy.setFrom(from);
    // assign
    Assign assign = createAssign();
    assign.addOperation(copy);
    putAssign(processDefinition, assign);

    // save objects and load them back
    processDefinition = saveAndReload(processDefinition);
    assign = getAssign(processDefinition);
    copy = (Copy) assign.getOperations().get(0);
    from = (FromPartnerLink) session.load(FromPartnerLink.class,
        session.getIdentifier(copy.getFrom()));

    // verify retrieved objects
    assertEquals("flightScheduleService", from.getPartnerLink().getName());
    assertEquals(Reference.PARTNER_ROLE, from.getEndpointReference());
  }

  public void testFromExpression() {
    // prepare persistent objects
    // expression
    Expression expression = new Expression();
    expression.setText("$rock");
    // from
    FromExpression from = new FromExpression();
    from.setExpression(expression);
    // copy
    Copy copy = new Copy();
    copy.setFrom(from);
    // assign
    Assign assign = createAssign();
    assign.addOperation(copy);
    putAssign(processDefinition, assign);

    // save objects and load them back
    processDefinition = saveAndReload(processDefinition);
    assign = getAssign(processDefinition);
    copy = (Copy) assign.getOperations().get(0);
    from = (FromExpression) session.load(FromExpression.class,
        session.getIdentifier(copy.getFrom()));

    // verify retrieved objects
    assertEquals("$rock", from.getExpression().getText());
  }

  public void testFromLiteral_element() throws Exception {
    // prepare persistent objects
    // literal
    Element literal = XmlUtil.parseText("\n<types:operationFault xmlns:types='urn:samples:atm:types' xmlns=''>"
        + "\n  <code>100</code>"
        + "\n  <description>not enough funds</description>"
        + "\n</types:operationFault>");
    // from
    FromElement from = new FromElement();
    from.setLiteral(literal);
    // copy
    Copy copy = new Copy();
    copy.setFrom(from);
    // assign
    Assign assign = createAssign();
    assign.addOperation(copy);
    putAssign(processDefinition, assign);

    // save objects and load them back
    processDefinition = saveAndReload(processDefinition);
    assign = getAssign(processDefinition);
    copy = (Copy) assign.getOperations().get(0);
    from = (FromElement) session.load(FromElement.class, session.getIdentifier(copy.getFrom()));

    // verify retrieved objects
    literal = from.getLiteral();
    assertEquals("urn:samples:atm:types", literal.getNamespaceURI());
    assertEquals("operationFault", literal.getLocalName());

    Element codeElem = XmlUtil.getElement(literal);
    assertNull(codeElem.getNamespaceURI());
    assertEquals("code", codeElem.getLocalName());
    assertEquals("100", DatatypeUtil.toString(codeElem));

    Element descElem = DOMUtils.getNextSiblingElement(codeElem);
    assertNull(descElem.getNamespaceURI());
    assertEquals("description", descElem.getLocalName());
    assertEquals("not enough funds", DatatypeUtil.toString(descElem));
  }

  public void testFromLiteral_text() {
    // prepare persistent objects
    // literal
    String literal = "<<hello, friends & foes>>";
    // from
    FromText from = new FromText();
    from.setLiteral(literal);
    // copy
    Copy copy = new Copy();
    copy.setFrom(from);
    // assign
    Assign assign = createAssign();
    assign.addOperation(copy);
    putAssign(processDefinition, assign);

    // save objects and load them back
    processDefinition = saveAndReload(processDefinition);
    assign = getAssign(processDefinition);
    copy = (Copy) assign.getOperations().get(0);
    from = (FromText) session.load(FromText.class, session.getIdentifier(copy.getFrom()));

    // verify retrieved objects
    assertEquals(literal, from.getLiteral());
  }

  public void testToVariable() {
    // prepare persistent objects
    // variable
    VariableDefinition variable = new VariableDefinition();
    variable.setName("RFQ");
    processDefinition.getGlobalScope().addVariable(variable);
    // query
    VariableQuery query = new VariableQuery();
    query.setText("/description");
    // to
    ToVariable to = new ToVariable();
    to.setVariable(variable);
    to.setPart("item");
    to.setQuery(query);
    // copy
    Copy copy = new Copy();
    copy.setTo(to);
    // assign
    Assign assign = createAssign();
    assign.addOperation(copy);
    putAssign(processDefinition, assign);

    // save objects and load them back
    processDefinition = saveAndReload(processDefinition);
    assign = getAssign(processDefinition);
    copy = (Copy) assign.getOperations().get(0);
    to = (ToVariable) session.load(ToVariable.class, session.getIdentifier(copy.getTo()));

    // verify retrieved objects
    // variable
    assertEquals(variable.getName(), to.getVariable().getName());
    // part
    assertEquals("item", to.getPart());
    // query
    assertEquals("/description", to.getQuery().getText());
  }

  public void testToProperty() throws Exception {
    // prepare persistent objects
    // variable
    VariableDefinition variable = new VariableDefinition();
    variable.setName("RFQ");
    processDefinition.getGlobalScope().addVariable(variable);
    // property
    Property property = new PropertyImpl();
    property.setQName(new QName("providerId"));
    processDefinition.getImportDefinition().addProperty(property);
    // to
    ToProperty to = new ToProperty();
    to.setVariable(variable);
    to.setProperty(property);
    // copy
    Copy copy = new Copy();
    copy.setTo(to);
    // assign
    Assign assign = createAssign();
    assign.addOperation(copy);
    putAssign(processDefinition, assign);

    // save objects and load them back
    processDefinition = saveAndReload(processDefinition);
    assign = getAssign(processDefinition);
    copy = (Copy) assign.getOperations().get(0);
    to = (ToProperty) session.load(ToProperty.class, session.getIdentifier(copy.getTo()));

    // verify retrieved objects
    // variable
    assertEquals(variable.getName(), to.getVariable().getName());
    // property
    assertEquals(property.getQName(), to.getProperty().getQName());
  }

  public void testToPartnerLink() throws Exception {
    // prepare persistent objects
    // partner link
    PartnerLinkDefinition partnerLink = new PartnerLinkDefinition();
    partnerLink.setName("flightScheduleService");
    processDefinition.getGlobalScope().addPartnerLink(partnerLink);
    // to
    ToPartnerLink to = new ToPartnerLink();
    to.setPartnerLink(partnerLink);
    // copy
    Copy copy = new Copy();
    copy.setTo(to);
    // assign
    Assign assign = createAssign();
    assign.addOperation(copy);
    putAssign(processDefinition, assign);

    // save objects and load them back
    processDefinition = saveAndReload(processDefinition);
    assign = getAssign(processDefinition);
    copy = (Copy) assign.getOperations().get(0);
    to = (ToPartnerLink) session.load(ToPartnerLink.class, session.getIdentifier(copy.getTo()));

    // verify retrieved objects
    assertEquals("flightScheduleService", to.getPartnerLink().getName());
  }

  public void testToExpression() {
    // prepare persistent objects
    // query
    Expression expression = new Expression();
    expression.setText("$rock");
    // to
    ToExpression to = new ToExpression();
    to.setExpression(expression);
    // copy
    Copy copy = new Copy();
    copy.setTo(to);
    // assign
    Assign assign = createAssign();
    assign.addOperation(copy);
    putAssign(processDefinition, assign);

    // save objects and load them back
    processDefinition = saveAndReload(processDefinition);
    assign = getAssign(processDefinition);
    copy = (Copy) assign.getOperations().get(0);
    to = (ToExpression) session.load(ToExpression.class, session.getIdentifier(copy.getTo()));

    // verify retrieved objects
    assertEquals("$rock", to.getExpression().getText());
  }

  protected Activity createActivity() {
    return createAssign();
  }

  private Assign createAssign() {
    return new Assign("assign");
  }

  private void putAssign(BpelProcessDefinition processDefinition, Assign assign) {
    processDefinition.getGlobalScope().setActivity(assign);
  }

  private Assign getAssign(BpelProcessDefinition processDefinition) {
    return (Assign) session.load(Assign.class, new Long(processDefinition.getGlobalScope()
        .getActivity()
        .getId()));
  }
}
