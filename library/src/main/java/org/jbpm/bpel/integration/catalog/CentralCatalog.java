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
import java.util.Iterator;
import java.util.List;

import javax.wsdl.Service;
import javax.wsdl.WSDLException;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.bpel.persistence.db.IntegrationSession;
import org.jbpm.bpel.wsdl.xml.WsdlUtil;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/11/06 02:58:56 $
 */
public class CentralCatalog extends DecoratorCatalog {

  private JbpmConfiguration jbpmConfiguration; // injected object, see jbpm.cfg.xml
  private List newEntries = new ArrayList();

  private static final Log log = LogFactory.getLog(CentralCatalog.class);

  public Service lookupService(QName serviceName) {
    ServiceCatalog delegate = getDelegate();
    Service service = delegate.lookupService(serviceName);

    if (service == null && loadNewEntries())
      service = delegate.lookupService(serviceName);

    return service;
  }

  public List lookupServices(QName portTypeName) {
    ServiceCatalog delegate = getDelegate();
    List services = super.lookupServices(portTypeName);

    if (services.isEmpty() && loadNewEntries())
      services = delegate.lookupServices(portTypeName);

    return services;
  }

  private boolean loadNewEntries() {
    synchronized (newEntries) {
      if (newEntries.isEmpty())
        return false;

      DefinitionCatalog delegate = (DefinitionCatalog) getDelegate();
      WSDLReader reader = WsdlUtil.getFactory().newWSDLReader();

      for (int i = 0, n = newEntries.size(); i < n; i++) {
        CatalogEntry entry = (CatalogEntry) newEntries.get(i);
        try {
          delegate.addDefinition(entry.readDefinition(reader));
        }
        catch (WSDLException e) {
          log.debug("skipping entry: " + entry, e);
        }
      }
      newEntries.clear();
    }
    return true;
  }

  public void addEntry(CatalogEntry entry) {
    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
    try {
      IntegrationSession integrationSession = IntegrationSession.getContextInstance(jbpmContext);
      integrationSession.saveCatalogEntry(entry);
    }
    finally {
      jbpmContext.close();
    }

    synchronized (newEntries) {
      newEntries.add(entry);
    }
  }

  protected ServiceCatalog createDelegate() {
    DefinitionCatalog delegate = new DefinitionCatalog();
    WSDLReader reader = WsdlUtil.getFactory().newWSDLReader();

    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
    try {
      IntegrationSession integrationSession = IntegrationSession.getContextInstance(jbpmContext);
      for (Iterator i = integrationSession.findCatalogEntries().iterator(); i.hasNext();) {
        CatalogEntry entry = (CatalogEntry) i.next();
        try {
          delegate.addDefinition(entry.readDefinition(reader));
        }
        catch (WSDLException e) {
          log.debug("skipping entry: " + entry, e);
        }
      }
    }
    finally {
      jbpmContext.close();
    }
    return delegate;
  }

  public static CentralCatalog getConfigurationInstance() {
    return (CentralCatalog) JbpmConfiguration.Configs.getObject("jbpm.bpel.central.catalog");
  }
}
