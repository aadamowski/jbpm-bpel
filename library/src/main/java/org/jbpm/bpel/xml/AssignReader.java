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

import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.jbpm.bpel.BpelException;
import org.jbpm.bpel.graph.basic.Assign;
import org.jbpm.bpel.graph.basic.assign.Copy;
import org.jbpm.bpel.graph.basic.assign.From;
import org.jbpm.bpel.graph.basic.assign.FromElement;
import org.jbpm.bpel.graph.basic.assign.FromExpression;
import org.jbpm.bpel.graph.basic.assign.FromPartnerLink;
import org.jbpm.bpel.graph.basic.assign.FromProperty;
import org.jbpm.bpel.graph.basic.assign.FromText;
import org.jbpm.bpel.graph.basic.assign.FromVariable;
import org.jbpm.bpel.graph.basic.assign.To;
import org.jbpm.bpel.graph.basic.assign.ToExpression;
import org.jbpm.bpel.graph.basic.assign.ToPartnerLink;
import org.jbpm.bpel.graph.basic.assign.ToProperty;
import org.jbpm.bpel.graph.basic.assign.ToVariable;
import org.jbpm.bpel.graph.basic.assign.FromPartnerLink.Reference;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.def.CompositeActivity;
import org.jbpm.bpel.integration.def.PartnerLinkDefinition;
import org.jbpm.bpel.sublang.def.VariableQuery;
import org.jbpm.bpel.variable.def.MessageType;
import org.jbpm.bpel.variable.def.VariableDefinition;
import org.jbpm.bpel.variable.def.VariableType;
import org.jbpm.bpel.wsdl.Property;
import org.jbpm.bpel.wsdl.PartnerLinkType.Role;
import org.jbpm.bpel.xml.util.DatatypeUtil;
import org.jbpm.bpel.xml.util.XmlUtil;

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/09/12 23:20:20 $
 */
public class AssignReader extends ActivityReader {

  public Activity read(Element activityElem, CompositeActivity parent) {
    Assign assign = new Assign();
    readStandardProperties(activityElem, assign, parent);
    readAssign(activityElem, assign);
    return assign;
  }

  public void readAssign(Element assignElem, Assign assign) {
    validateNonInitial(assignElem, assign);

    // iterate copy elements
    CompositeActivity parent = assign.getCompositeActivity();
    for (Iterator i = XmlUtil.getElements(assignElem, BpelConstants.NS_BPEL,
        BpelConstants.ELEM_COPY); i.hasNext();) {
      Element copyElem = (Element) i.next();

      // from-spec
      Element fromElem = XmlUtil.getElement(copyElem, BpelConstants.NS_BPEL,
          BpelConstants.ELEM_FROM);
      From from = readFrom(fromElem, parent);

      // to-spec
      Element toElem = XmlUtil.getElement(copyElem, BpelConstants.NS_BPEL, BpelConstants.ELEM_TO);
      To to = readTo(toElem, parent);

      // copy
      Copy copy = new Copy();
      copy.setFrom(from);
      copy.setTo(to);

      assign.addOperation(copy);
    }
  }

  protected From readFrom(Element fromElem, CompositeActivity parent) {
    From from;
    if (fromElem.hasAttribute(BpelConstants.ATTR_PROPERTY))
      from = readFromProperty(fromElem, parent);
    else if (fromElem.hasAttribute(BpelConstants.ATTR_VARIABLE))
      from = readFromVariable(fromElem, parent);
    else if (fromElem.hasAttribute(BpelConstants.ATTR_PARTNER_LINK))
      from = readFromPartnerLink(fromElem, parent);
    else {
      Element literalElem = XmlUtil.getElement(fromElem, BpelConstants.NS_BPEL,
          BpelConstants.ELEM_LITERAL);
      if (literalElem != null)
        from = readFromLiteral(literalElem);
      else
        from = readFromExpression(fromElem, parent);
    }
    return from;
  }

  protected To readTo(Element toElem, CompositeActivity parent) {
    To to;
    if (toElem.hasAttribute(BpelConstants.ATTR_PROPERTY))
      to = readToProperty(toElem, parent);
    else if (toElem.hasAttribute(BpelConstants.ATTR_VARIABLE))
      to = readToVariable(toElem, parent);
    else if (toElem.hasAttribute(BpelConstants.ATTR_PARTNER_LINK))
      to = readToPartnerLink(toElem, parent);
    else
      to = readToExpression(toElem, parent);
    return to;
  }

  protected From readFromVariable(Element fromElem, CompositeActivity parent) {
    FromVariable from = new FromVariable();
    // variable
    VariableDefinition variable = readVariable(fromElem, parent);
    from.setVariable(variable);
    // part
    if (fromElem.hasAttribute(BpelConstants.ATTR_PART))
      from.setPart(readPart(fromElem, variable));
    // query
    Element queryElem = XmlUtil.getElement(fromElem, BpelConstants.NS_BPEL,
        BpelConstants.ELEM_QUERY);
    if (queryElem != null)
      from.setQuery(readQuery(queryElem, parent));
    return from;
  }

  protected From readFromProperty(Element fromElem, CompositeActivity parent) {
    FromProperty from = new FromProperty();
    from.setVariable(readVariable(fromElem, parent));
    from.setProperty(readProperty(fromElem, parent));
    return from;
  }

  protected From readFromPartnerLink(Element fromElem, CompositeActivity parent) {
    FromPartnerLink from = new FromPartnerLink();

    // partner link
    PartnerLinkDefinition partnerLink = readPartnerLink(fromElem, parent);
    from.setPartnerLink(partnerLink);

    // endpoint reference
    Reference reference = Reference.valueOf(fromElem.getAttribute(BpelConstants.ATTR_ENDPOINT_REFERENCE));
    from.setEndpointReference(reference);

    // BPEL-278 verify the referenced role exists in the partner link
    Role role = reference == Reference.PARTNER_ROLE ? partnerLink.getPartnerRole()
        : partnerLink.getMyRole();
    if (role == null) {
      bpelReader.getProblemHandler().add(
          new ParseProblem("role not declared in partner link", fromElem));
    }

    return from;
  }

  protected From readFromLiteral(Element literalElem) {
    Element anyElem = XmlUtil.getElement(literalElem);
    if (anyElem != null) {
      FromElement from = new FromElement();
      from.setLiteral(anyElem);
      return from;
    }

    // no child element found, take the text content
    FromText from = new FromText();
    from.setLiteral(DatatypeUtil.toString(literalElem));
    return from;
  }

  protected From readFromExpression(Element fromElem, CompositeActivity parent) {
    FromExpression from = new FromExpression();
    from.setExpression(bpelReader.readExpression(fromElem, parent));
    return from;
  }

  protected To readToVariable(Element toElem, CompositeActivity parent) {
    ToVariable to = new ToVariable();
    // variable
    VariableDefinition variable = readVariable(toElem, parent);
    to.setVariable(variable);
    // part
    if (toElem.hasAttribute(BpelConstants.ATTR_PART))
      to.setPart(readPart(toElem, variable));
    // query
    Element queryElem = XmlUtil.getElement(toElem, BpelConstants.NS_BPEL, BpelConstants.ELEM_QUERY);
    if (queryElem != null)
      to.setQuery(readQuery(queryElem, parent));
    return to;
  }

  protected To readToProperty(Element toElem, CompositeActivity parent) {
    ToProperty to = new ToProperty();
    to.setVariable(readVariable(toElem, parent));
    to.setProperty(readProperty(toElem, parent));
    return to;
  }

  protected To readToPartnerLink(Element toElem, CompositeActivity parent) {
    ToPartnerLink to = new ToPartnerLink();
    to.setPartnerLink(readPartnerLink(toElem, parent));
    return to;
  }

  protected To readToExpression(Element toElem, CompositeActivity parent) {
    ToExpression to = new ToExpression();
    to.setExpression(bpelReader.readExpression(toElem, parent));
    return to;
  }

  protected VariableDefinition readVariable(Element contextElem, CompositeActivity parent) {
    String variableName = contextElem.getAttribute(BpelConstants.ATTR_VARIABLE);
    VariableDefinition variable = parent.findVariable(variableName);
    if (variable == null)
      bpelReader.getProblemHandler().add(new ParseProblem("variable not found", contextElem));
    return variable;
  }

  protected String readPart(Element contextElem, VariableDefinition variable) {
    String partName = contextElem.getAttribute(BpelConstants.ATTR_PART);

    if (variable != null) {
      VariableType type = variable.getType();
      if (type.isMessage()) {
        MessageType messageType = (MessageType) type;

        if (!messageType.getMessage().getParts().containsKey(partName))
          bpelReader.getProblemHandler().add(new ParseProblem("part not found", contextElem));
      }
      else {
        bpelReader.getProblemHandler().add(
            new ParseProblem("illegal access to part of non-message variable", contextElem));
      }
    }

    return partName;
  }

  protected VariableQuery readQuery(Element queryElem, CompositeActivity parent) {
    VariableQuery query = new VariableQuery();
    BpelProcessDefinition processDefinition = parent.getBpelProcessDefinition();

    // namespace declarations
    Map namespaces = XmlUtil.findNamespaceDeclarations(queryElem);
    query.setNamespaces(processDefinition.addNamespaces(namespaces));

    // language attribute
    String language = XmlUtil.getAttribute(queryElem, BpelConstants.ATTR_QUERY_LANGUAGE);
    if (language == null)
      language = processDefinition.getQueryLanguage();
    query.setLanguage(language);

    // text content
    query.setText(DatatypeUtil.toString(queryElem));

    // parsing
    try {
      query.parse();
    }
    catch (BpelException e) {
      bpelReader.getProblemHandler().add(new ParseProblem("could not parse query", queryElem, e));
    }
    return query;
  }

  protected Property readProperty(Element contextElem, CompositeActivity parent) {
    QName propertyName = XmlUtil.getQNameValue(contextElem.getAttributeNode(BpelConstants.ATTR_PROPERTY));
    Property property = parent.getBpelProcessDefinition().getImportDefinition().getProperty(
        propertyName);
    if (property == null)
      bpelReader.getProblemHandler().add(new ParseProblem("property not found", contextElem));
    return property;
  }

  protected PartnerLinkDefinition readPartnerLink(Element contextElem, CompositeActivity parent) {
    String partnerLinkName = contextElem.getAttribute(BpelConstants.ATTR_PARTNER_LINK);
    PartnerLinkDefinition partnerLink = parent.findPartnerLink(partnerLinkName);
    if (partnerLink == null)
      bpelReader.getProblemHandler().add(new ParseProblem("partner link not found", contextElem));
    return partnerLink;
  }
}
