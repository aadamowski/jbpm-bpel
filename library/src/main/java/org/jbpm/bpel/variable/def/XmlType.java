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

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import org.jbpm.bpel.sublang.def.PropertyQuery;
import org.jbpm.bpel.wsdl.PropertyAlias;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.bpel.xml.util.DatatypeUtil;
import org.jbpm.bpel.xml.util.XmlUtil;

/**
 * Common base for {@linkplain SchemaType XML Schema types} and
 * {@linkplain ElementType XML Schema elements}.
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/09/12 23:20:21 $
 */
public abstract class XmlType extends VariableType {

  protected QName name;

  protected XmlType() {
  }

  public QName getName() {
    return name;
  }

  public void setName(QName name) {
    this.name = name;
  }

  public Object createValue(VariableDefinition definition) {
    // create an element value
    Element value = createElement(definition);
    // mark as uninitialized with jbpm:initialized
    value.setAttributeNS(BpelConstants.NS_VENDOR, "jbpm:"
        + BpelConstants.ATTR_INITIALIZED, "false");
    // some TrAX implementations do not fixup namespaces,
    // declare xsi namespace
    XmlUtil.addNamespaceDeclaration(value, BpelConstants.NS_VENDOR, "jbpm");
    return value;
  }

  protected abstract Element createElement(VariableDefinition definition);

  public boolean isInitialized(Object variableValue) {
    Element elementValue = (Element) variableValue;
    // check for jbpm:initialized
    Attr initializedAttr = elementValue.getAttributeNodeNS(
        BpelConstants.NS_VENDOR, BpelConstants.ATTR_INITIALIZED);
    if (initializedAttr != null) {
      if (DatatypeUtil.parseBoolean(initializedAttr.getValue()) == Boolean.FALSE
          // check for child nodes or other attributes
          && !elementValue.hasChildNodes()
          && !hasAttributesOtherThanInitialized(elementValue)) {
        return false;
      }
      // consider jbpm:initialized is bogus
      elementValue.removeAttributeNode(initializedAttr);
    }
    return true;
  }

  private static boolean hasAttributesOtherThanInitialized(Element elem) {
    NamedNodeMap attributeMap = elem.getAttributes();

    final int attributeCount = attributeMap.getLength();
    assert attributeCount > 0 : attributeCount;

    switch (attributeCount) {
    case 1: // must be jbpm:initialized
      return false;
    case 2: // second attribute might be xmlns:jbpm, which doesn't count
      return !elem.hasAttributeNS(BpelConstants.NS_XMLNS, XmlUtil.getPrefix(
          BpelConstants.NS_VENDOR, elem));
    default:
      return true;
    }
  }

  public void setValue(Object currentValue, Object newValue) {
    XmlUtil.setObjectValue((Element) currentValue, newValue);
  }

  protected Object evaluateProperty(PropertyAlias propertyAlias,
      Object variableValue) {
    PropertyQuery query = propertyAlias.getQuery();
    return query != null ?
    // evaluate the query using the variable value as context node
    query.getEvaluator().evaluate((Element) variableValue)
        // assume this is a simple type or an element with simple content
        : variableValue;
  }

  protected void assignProperty(PropertyAlias propertyAlias,
      Object variableValue, Object propertyValue) {
    PropertyQuery query = propertyAlias.getQuery();
    Element elementValue = (Element) variableValue;
    if (query == null) {
      // assign the variable itself; assume this is a simple type or an element
      // with simple content
      XmlUtil.setObjectValue(elementValue, propertyValue);
    }
    else {
      // assign the query location using the variable value as context node
      query.getEvaluator().assign(elementValue, propertyValue);
    }
  }
}
