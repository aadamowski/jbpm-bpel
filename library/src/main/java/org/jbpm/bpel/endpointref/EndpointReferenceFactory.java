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
package org.jbpm.bpel.endpointref;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import org.jbpm.JbpmConfiguration;
import org.jbpm.bpel.xml.util.XmlUtil;
import org.jbpm.util.ClassLoaderUtil;

/**
 * Manufactures the endpoint reference instance appropriate for a given value
 * element name and interpretation scheme combination.
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/05/02 23:06:00 $
 */
public abstract class EndpointReferenceFactory {

  public static final String RESOURCE_ENDPOINT_FACTORIES = "resource.endpoint.reference.factories";

  private static final Log log = LogFactory.getLog(EndpointReferenceFactory.class);
  private static final List endpointFactories = readEndpointFactories();

  protected EndpointReferenceFactory() {
  }

  public abstract EndpointReference createEndpointReference();

  public abstract boolean acceptsReference(QName endpointRefName,
      String refScheme);

  public static EndpointReferenceFactory getInstance(QName endpointRefName,
      String refScheme) {
    for (int i = 0, n = endpointFactories.size(); i < n; i++) {
      EndpointReferenceFactory factory = (EndpointReferenceFactory) endpointFactories.get(i);
      if (factory.acceptsReference(endpointRefName, refScheme))
        return factory;
    }
    return null;
  }

  private static List readEndpointFactories() {
    // get endpoint factories resource name
    String resource = JbpmConfiguration.Configs.getString(RESOURCE_ENDPOINT_FACTORIES);

    // parse endpoint factories document
    Element factoriesElem;
    try {
      // parse xml document
      factoriesElem = XmlUtil.parseResource(resource);
    }
    catch (SAXException e) {
      log.error(
          "endpoint factories document contains invalid xml: " + resource, e);
      return Collections.EMPTY_LIST;
    }
    catch (IOException e) {
      log.error("could not read endpoint factories document: " + resource, e);
      return Collections.EMPTY_LIST;
    }

    // walk through endpointFactory elements
    ArrayList endpointFactories = new ArrayList();
    Iterator factoryElemIt = XmlUtil.getElements(factoriesElem, null,
        "endpointFactory");
    while (factoryElemIt.hasNext()) {
      Element factoryElem = (Element) factoryElemIt.next();

      // load endpoint factory class
      String factoryClassName = factoryElem.getAttribute("class");
      Class factoryClass = ClassLoaderUtil.loadClass(factoryClassName);

      // validate endpoint factory class
      if (!EndpointReferenceFactory.class.isAssignableFrom(factoryClass)) {
        log.warn("not an endpoint factory: " + factoryClassName);
        continue;
      }

      try {
        // instantiate endpoint factory
        EndpointReferenceFactory factory = (EndpointReferenceFactory) factoryClass.newInstance();

        // register instance
        endpointFactories.add(factory);
        log.debug("registered endpoint factory: " + factoryClassName);
      }
      catch (InstantiationException e) {
        log.warn(
            "endpoint factory class not instantiable: " + factoryClassName, e);
      }
      catch (IllegalAccessException e) {
        log.warn("endpoint factory class or constructor not public: "
            + factoryClassName, e);
      }
    }
    return endpointFactories;
  }
}
