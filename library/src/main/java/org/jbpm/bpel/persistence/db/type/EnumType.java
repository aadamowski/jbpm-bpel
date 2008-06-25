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
import java.util.Properties;

import org.apache.commons.lang.enums.Enum;
import org.apache.commons.lang.enums.EnumUtils;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;

/**
 * Mapping between an {@linkplain Enum enumeration} and a {@linkplain Types#VARCHAR VARCHAR} column.
 * @author Juan Cantu
 * @version $Revision$ $Date: 2008/02/01 05:46:43 $
 */
public class EnumType implements UserType, ParameterizedType {

  private Class enumClass;

  /**
   * Enumeration class parameter name.
   */
  public static final String ENUM_CLASS_PARAM = "class";

  private static final int[] SQL_TYPES = { Types.VARCHAR };

  public boolean equals(Object x, Object y) {
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

  public Serializable disassemble(Object value) throws HibernateException {
    return (Serializable) value;
  }

  public Object assemble(Serializable cached, Object owner) throws HibernateException {
    return cached;
  }

  public Object replace(Object original, Object target, Object owner) {
    return target;
  }

  public int[] sqlTypes() {
    return SQL_TYPES;
  }

  public Object nullSafeGet(ResultSet rs, String[] names, Object owner) throws HibernateException,
      SQLException {
    // get enum constant name
    String enumName = (String) Hibernate.STRING.nullSafeGet(rs, names[0]);
    // resolve enum constant
    Enum enumConstant = enumName != null ? EnumUtils.getEnum(enumClass, enumName) : null;
    return enumConstant;
  }

  public void nullSafeSet(PreparedStatement st, Object value, int index) throws HibernateException,
      SQLException {
    String enumName = value != null ? ((Enum) value).getName() : null;
    Hibernate.STRING.nullSafeSet(st, enumName, index);
  }

  public Class returnedClass() {
    return enumClass;
  }

  public void setParameterValues(Properties parameters) throws HibernateException {
    String enumClassName = (String) parameters.get(ENUM_CLASS_PARAM);
    try {
      enumClass = Class.forName(enumClassName);
    }
    catch (ClassNotFoundException e) {
      throw new HibernateException("enum class not found: " + enumClassName, e);
    }
  }
}
