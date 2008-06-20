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
package org.jbpm.bpel.xml.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/**
 * @author Juan Cantu
 * @version $Revision$ $Date: 2007/11/25 13:03:14 $
 */
class LocalEntityResolver implements EntityResolver {

  private Map entityRegistry = createEntityRegistry();

  private static final Log log = LogFactory.getLog(LocalEntityResolver.class);

  public static final EntityResolver INSTANCE = new LocalEntityResolver();

  private LocalEntityResolver() {
  }

  /**
   * Gets the local resource that corresponds to the given identifiers by looking up the local
   * entity registry.
   * @param publicId the public ID of the entity
   * @param systemId the system ID of the entity
   * @return the input source for the entity, or <code>null</code> if the given identifiers do not
   * match any entity
   * @throws IOException if the local resource cannot be found
   */
  public InputSource resolveEntity(String publicId, String systemId) throws IOException {
    String entityResource = getResourceForEntity(publicId, systemId);
    if (entityResource == null)
      return null;

    URL entityURL = getClass().getResource(entityResource);
    if (entityURL == null)
      throw new FileNotFoundException(entityResource);

    return new InputSource(entityURL.toExternalForm());
  }

  /**
   * Gets the local resource that corresponds to the identified entity by looking up the entity
   * registry. The public ID is used first. If not found, the entity is searched with the system ID.
   * @param publicId the public ID of the entity, can be <code>null</code>
   * @param systemId the system ID of the entity, can be <code>null</code>
   * @return the local filename
   */
  private String getResourceForEntity(String publicId, String systemId) {
    if (publicId != null) {
      String resource = (String) entityRegistry.get(publicId);
      if (resource != null) {
        if (log.isTraceEnabled())
          log.trace("resolved: publicId=" + publicId + ", resource=" + resource);
        return resource;
      }
    }

    if (systemId != null) {
      String resource = (String) entityRegistry.get(systemId);
      if (resource != null) {
        if (log.isTraceEnabled())
          log.trace("resolved: systemId=" + systemId + ", resource=" + resource);
        return resource;
      }
    }

    return null;
  }

  private static Map createEntityRegistry() {
    HashMap entityRegistry = new HashMap();

    // xml schema data type definitions
    entityRegistry.put("-//W3C//DTD XMLSCHEMA 200102//EN", "XMLSchema.dtd");
    entityRegistry.put("datatypes", "datatypes.dtd");

    // xml namespace schema
    entityRegistry.put("http://www.w3.org/2001/xml.xsd", "xml.xsd");

    // wsdl schema
    entityRegistry.put("http://schemas.xmlsoap.org/wsdl/", "wsdl.xsd");

    // j2ee schema
    entityRegistry.put("http://www.ibm.com/webservices/xsd/j2ee_jaxrpc_mapping_1_1.xsd",
        "j2ee_jaxrpc_mapping_1_1.xsd");
    entityRegistry.put("http://www.ibm.com/webservices/xsd/j2ee_web_services_client_1_1.xsd",
        "j2ee_web_services_client_1_1.xsd");

    return entityRegistry;
  }
}