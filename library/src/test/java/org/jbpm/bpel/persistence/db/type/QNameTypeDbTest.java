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
package org.jbpm.bpel.persistence.db.type;

import java.io.Serializable;
import java.util.List;

import javax.xml.namespace.QName;

import org.jbpm.bpel.persistence.db.AbstractDbTestCase;
import org.jbpm.bpel.wsdl.Property;
import org.jbpm.bpel.wsdl.impl.PropertyImpl;
import org.jbpm.bpel.xml.BpelConstants;

/**
 * @author Juan Cantu
 * @version $Revision$ $Date: 2007/07/02 23:55:09 $
 */
public class QNameTypeDbTest extends AbstractDbTestCase {

  Property property = new PropertyImpl();

  static final QName PROPERTY_NAME = new QName(BpelConstants.NS_EXAMPLES, "p");

  protected void setUp() throws Exception {
    super.setUp();
    // property
    property.setQName(PROPERTY_NAME);
  }

  public void testUnqualifiedName() {
    QName qname = new QName("someLocalName");
    property.setType(qname);

    property = saveAndReload(property);

    assertEquals(qname, property.getType());
  }

  public void testNamespaceQualifiedName() {
    QName qname = new QName("aNamespace", "someLocalName");
    property.setType(qname);

    property = saveAndReload(property);

    assertEquals(qname, property.getType());
  }

  public void testNullName() {
    property.setType(null);

    property = saveAndReload(property);

    assertNull(property.getType());
  }

  public void testLocalPartDereference() {
    QName propertyType = new QName("brandFuel");
    property.setType(propertyType);

    session.save(property);
    List result = session.createQuery(
        "from org.jbpm.bpel.wsdl.impl.PropertyImpl where type.localPart = 'brandFuel'").list();

    for (int i = 0, n = result.size(); i < n; i++) {
      property = (Property) result.get(i);
      assertEquals(PROPERTY_NAME, property.getQName());
      assertEquals(propertyType, property.getType());
    }
  }

  public void testNamespaceURIDereference() {
    QName propertyType = new QName("urn:jbpm:bpel:db", "uniqueStuff");
    property.setType(propertyType);

    session.save(property);
    List result = session.createQuery(
        "from org.jbpm.bpel.wsdl.impl.PropertyImpl where type.namespaceURI = 'urn:jbpm:bpel:db'")
        .list();

    for (int i = 0, n = result.size(); i < n; i++) {
      property = (Property) result.get(i);
      assertEquals(PROPERTY_NAME, property.getQName());
      assertEquals(propertyType, property.getType());
    }
  }

  protected Property saveAndReload(Property property) {
    Serializable id = session.save(property);
    newTransaction();
    return (Property) session.load(PropertyImpl.class, id);
  }
}
