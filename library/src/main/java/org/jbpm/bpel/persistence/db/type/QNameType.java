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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import javax.xml.namespace.QName;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.type.Type;
import org.hibernate.usertype.CompositeUserType;

/**
 * Mapping between a {@linkplain QName qualified name} and two {@linkplain Types#VARCHAR VARCHAR}
 * columns.
 * @author Juan Cantu
 * @version $Revision$ $Date: 2008/02/01 05:46:43 $
 */
public class QNameType implements CompositeUserType {

  private static final String[] PROPERTY_NAMES = { "localPart", "namespaceURI" };
  private static final Type[] PROPERTY_TYPES = { Hibernate.STRING, Hibernate.STRING };

  public boolean equals(Object x, Object y) throws HibernateException {
    return x == null ? y == null : x.equals(y);
  }

  public int hashCode(Object x) throws HibernateException {
    return x.hashCode();
  }

  public Object deepCopy(Object value) throws HibernateException {
    return value;
  }

  public boolean isMutable() {
    return false;
  }

  public Class returnedClass() {
    return QName.class;
  }

  public Object assemble(Serializable cached, SessionImplementor session, Object owner)
      throws HibernateException {
    return cached;
  }

  public Serializable disassemble(Object value, SessionImplementor session)
      throws HibernateException {
    return (Serializable) value;
  }

  public String[] getPropertyNames() {
    return PROPERTY_NAMES;
  }

  public Type[] getPropertyTypes() {
    return PROPERTY_TYPES;
  }

  public Object getPropertyValue(Object component, int property) throws HibernateException {
    QName qname = (QName) component;
    return property == 0 ? qname.getLocalPart() : qname.getNamespaceURI();
  }

  public void setPropertyValue(Object component, int property, Object value)
      throws HibernateException {
    throw new HibernateException("QName is immutable");
  }

  public Object nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner)
      throws HibernateException, SQLException {
    // BPEL-221: QNameType persists namespace property in LOCALNAME_ column
    String localPart = (String) Hibernate.STRING.nullSafeGet(rs, names[0]);
    String namespace = (String) Hibernate.STRING.nullSafeGet(rs, names[1]);

    return localPart != null ? new QName(namespace, localPart) : null;
  }

  public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session)
      throws HibernateException, SQLException {
    QName qname = (QName) value;

    String localPart;
    String namespace;
    if (value == null) {
      localPart = null;
      namespace = null;
    }
    else {
      localPart = qname.getLocalPart();
      namespace = qname.getNamespaceURI();
    }

    // BPEL-221: QNameType persists namespace property in LOCALNAME_ column
    Hibernate.STRING.nullSafeSet(st, localPart, index);
    Hibernate.STRING.nullSafeSet(st, namespace, index + 1);
  }

  public Object replace(Object original, Object target, SessionImplementor session, Object owner)
      throws HibernateException {
    return original;
  }
}
