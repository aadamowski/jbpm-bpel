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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import org.jbpm.JbpmContext;
import org.jbpm.bpel.xml.util.XmlUtil;

/**
 * Mapping between a DOM {@linkplain Element element} and a {@linkplain Types#VARBINARY VARBINARY}
 * column.
 * @author Juan Cantu
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2008/02/01 05:46:43 $
 */
public class ElementType implements UserType {

  private static final int[] SQL_TYPES = { Types.VARBINARY };
  private static final Log log = LogFactory.getLog(ElementType.class);

  public boolean equals(Object x, Object y) {
    return x == null ? y == null : x.equals(y);
  }

  public int hashCode(Object x) {
    return x.hashCode();
  }

  public Object deepCopy(Object value) {
    return value != null ? ((Element) value).cloneNode(true) : null;
  }

  public boolean isMutable() {
    return true;
  }

  public Serializable disassemble(Object value) throws HibernateException {
    return (Serializable) deepCopy(value);
  }

  public Object assemble(Serializable cached, Object owner) throws HibernateException {
    return deepCopy(cached);
  }

  public Object replace(Object original, Object target, Object owner) {
    return deepCopy(original);
  }

  public int[] sqlTypes() {
    return SQL_TYPES;
  }

  public Object nullSafeGet(ResultSet rs, String[] names, Object owner) throws HibernateException,
      SQLException {
    // retrieve stream from database
    String columnName = names[0];
    InputStream xmlStream = rs.getBinaryStream(columnName);

    // if SQL value is NULL, element is null as well
    if (xmlStream == null)
      return null;

    // introduce inflater, if requested
    Number deflateLevel = getXmlDeflateLevel();
    if (deflateLevel != null)
      xmlStream = new InflaterInputStream(xmlStream);

    try {
      // parse XML text
      Element element = XmlUtil.getDocumentBuilder().parse(xmlStream).getDocumentElement();
      xmlStream.close();

      if (log.isTraceEnabled())
        log.trace("returning '" + XmlUtil.toTraceString(element) + "' as column: " + columnName);
      return element;
    }
    catch (SAXException e) {
      throw new HibernateException("could not parse column: " + columnName, e);
    }
    catch (IOException e) {
      throw new HibernateException("could not read column: " + columnName, e);
    }
  }

  public void nullSafeSet(PreparedStatement st, Object value, int index) throws HibernateException,
      SQLException {
    // easy way out: null value
    if (value == null) {
      st.setNull(index, SQL_TYPES[0]);
      if (log.isTraceEnabled())
        log.trace("binding null to parameter: " + index);
    }
    else {
      Element element = (Element) value;
      try {
        // create identity transformer
        Transformer idTransformer = XmlUtil.getTransformerFactory().newTransformer();

        // allocate memory result stream
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

        // deflate if requested
        Number deflateLevel = getXmlDeflateLevel();
        if (deflateLevel != null) {
          // introduce deflater stream
          Deflater deflater = new Deflater(deflateLevel.intValue());
          OutputStream deflaterStream = new DeflaterOutputStream(byteStream, deflater);
          // write element to stream
          idTransformer.transform(new DOMSource(element), new StreamResult(deflaterStream));
          // release resources
          try {
            deflaterStream.close();
          }
          catch (IOException e) {
            // should not happen
            throw new AssertionError(e);
          }
          deflater.end();
        }
        else {
          // write element to stream
          idTransformer.transform(new DOMSource(element), new StreamResult(byteStream));
          // noop
          // byteStream.close();
        }

        // extract contents of result stream
        st.setBytes(index, byteStream.toByteArray());
        if (log.isTraceEnabled())
          log.trace("binding '" + byteStream + "' to parameter: " + index);
      }
      catch (TransformerException e) {
        throw new HibernateException("could not transform to xml stream: " + element, e);
      }
    }
  }

  private static Number getXmlDeflateLevel() {
    JbpmContext jbpmContext = JbpmContext.getCurrentJbpmContext();
    if (jbpmContext != null) {
      Object deflateLevel = jbpmContext.getObjectFactory().createObject(
          "jbpm.bpel.xml.deflate.level");
      if (deflateLevel instanceof Number)
        return (Number) deflateLevel;
      else if (deflateLevel != null)
        log.warn("xml deflate level is not a number: " + deflateLevel);
    }
    return null;
  }

  public Class returnedClass() {
    return Element.class;
  }
}
