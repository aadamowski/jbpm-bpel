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
package org.jbpm.bpel.wsdl.impl;

import javax.xml.namespace.QName;

/**
 * Represents extensibility elements having a qualified name.
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2006/08/21 01:05:59 $
 */
public class NamedExtension extends AbstractExtension {

  private QName name;

  private static final long serialVersionUID = 1L;

  /**
   * Gets the name of this extension.
   * @return the name in place
   */
  public QName getQName() {
    return name;
  }

  /**
   * Sets the name of this extension.
   * @param name the new name
   */
  public void setQName(QName name) {
    this.name = name;
  }

  /**
   * Tests this named extension for equality with another object. Two named
   * extensions are considered equal if the element type and name are equal.
   * This method uses {@link QName#equals(Object)} to check equality of the
   * element type and name.
   * @param obj the object to test for equality with this named extension. If it
   *          is not a {@link NamedExtension} or is <code>null</code>, then
   *          this method returns <code>false</code>
   * @return <code>true</code> if the given object is equal to this named
   *         extension; <code>false</code> otherwise
   */
  public boolean equals(Object obj) {
    boolean equals;
    if (obj instanceof NamedExtension) {
      NamedExtension extension = (NamedExtension) obj;
      equals = getQName().equals(extension.getQName())
          && getElementType().equals(extension.getElementType());
    }
    else {
      equals = false;
    }
    return equals;
  }

  /**
   * Returns a hash code for this named extension. The hash code is calculated
   * using both the element type and the name of this extension.
   * @return a hash code for this named extension
   */
  public int hashCode() {
    int hashCode = getElementType().hashCode();
    QName name = getQName();
    if (name != null) {
      hashCode ^= name.hashCode();
    }
    return hashCode;
  }

  public String toString() {
    return getElementType() + "(" + getQName() + ")";
  }
}
