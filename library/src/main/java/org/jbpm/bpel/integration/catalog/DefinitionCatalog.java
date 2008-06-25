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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.xml.namespace.QName;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/10/13 02:53:29 $
 */
public class DefinitionCatalog implements ServiceCatalog {

  private Map servicesByName = new HashMap();
  private Map servicesByPortType = new HashMap();

  public void addService(Service service) {
    synchronized (servicesByName) {
      servicesByName.put(service.getQName(), service);
    }

    for (Iterator p = service.getPorts().values().iterator(); p.hasNext();) {
      Port port = (Port) p.next();
      QName portTypeName = port.getBinding().getPortType().getQName();

      List services;
      synchronized (servicesByPortType) {
        services = (List) servicesByPortType.get(portTypeName);
        if (services == null) {
          services = new ArrayList();
          servicesByPortType.put(portTypeName, services);
        }
      }
      services.add(service);
    }
  }

  public void addDefinition(Definition definition) {
    for (Iterator s = definition.getServices().values().iterator(); s.hasNext();) {
      Service service = (Service) s.next();
      addService(service);
    }
  }

  public List lookupServices(QName portTypeName) {
    List services = (List) servicesByPortType.get(portTypeName);
    return services != null ? services : Collections.EMPTY_LIST;
  }

  public Service lookupService(QName serviceName) {
    return (Service) servicesByName.get(serviceName);
  }
}
