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
package org.jbpm.bpel.integration.catalog;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.wsdl.Definition;
import javax.wsdl.WSDLException;
import javax.wsdl.xml.WSDLReader;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.xml.sax.InputSource;

import org.jbpm.bytes.ByteArray;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/11/06 02:58:56 $
 */
public class CatalogEntry {

  long id;
  private String baseLocation;
  private ByteArray descriptionBody;

  CatalogEntry() {
  }

  public CatalogEntry(String baseLocation, InputStream descriptionSource) throws IOException {
    this.baseLocation = baseLocation;
    if (descriptionSource != null)
      descriptionBody = new ByteArray(toByteArray(descriptionSource));
  }

  private static byte[] toByteArray(InputStream source) throws IOException {
    ByteArrayOutputStream memorySink = new ByteArrayOutputStream(4 * 1024);
    byte[] buffer = new byte[512];
    try {
      for (int count; (count = source.read(buffer)) != -1;)
        memorySink.write(buffer, 0, count);
    }
    finally {
      source.close();
    }
    return memorySink.toByteArray();
  }

  public String getBaseLocation() {
    return baseLocation;
  }

  public boolean hasDescriptionBody() {
    return descriptionBody != null;
  }

  public Definition readDefinition(WSDLReader reader) throws WSDLException {
    if (descriptionBody != null) {
      try {
        return reader.readWSDL(baseLocation, new InputSource(new ByteArrayInputStream(
            descriptionBody.getBytes())));
      }
      catch (WSDLException e) {
        if (baseLocation == null)
          throw e;
      }
    }

    return reader.readWSDL(baseLocation);
  }

  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    if (baseLocation != null)
      builder.append("baseLocation", baseLocation);
    if (descriptionBody != null)
      builder.append("descriptionBody", true);
    return builder.toString();
  }
}
