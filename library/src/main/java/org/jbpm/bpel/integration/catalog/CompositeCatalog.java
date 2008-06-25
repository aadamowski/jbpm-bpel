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
import java.util.Iterator;
import java.util.List;

import javax.wsdl.Service;
import javax.xml.namespace.QName;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2006/08/21 01:06:10 $
 */
public class CompositeCatalog implements ServiceCatalog {

  private List catalogs;

  public CompositeCatalog() {
    catalogs = Collections.EMPTY_LIST;
  }

  public CompositeCatalog(List catalogs) {
    this.catalogs = catalogs;
  }

  public List getCatalogs() {
    return catalogs;
  }

  public void setCatalogs(List catalogs) {
    this.catalogs = catalogs;
  }

  public List lookupServices(QName portTypeName) {
    List allServices = new ArrayList();
    Iterator catalogIt = catalogs.iterator();
    while (catalogIt.hasNext()) {
      ServiceCatalog catalog = (ServiceCatalog) catalogIt.next();
      allServices.addAll(catalog.lookupServices(portTypeName));
    }
    return allServices;
  }

  public Service lookupService(QName serviceName) {
    Service service = null;
    Iterator catalogIt = catalogs.iterator();
    while (catalogIt.hasNext()) {
      ServiceCatalog catalog = (ServiceCatalog) catalogIt.next();
      service = catalog.lookupService(serviceName);
      if (service == null) break;
    }
    return service;
  }
}
