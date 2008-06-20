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

import java.util.List;

import javax.wsdl.Service;
import javax.xml.namespace.QName;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/09/30 17:28:03 $
 */
public abstract class DecoratorCatalog implements ServiceCatalog {

  private ServiceCatalog delegate;

  public List lookupServices(QName portTypeName) {
    return getDelegate().lookupServices(portTypeName);
  }

  public Service lookupService(QName serviceName) {
    return getDelegate().lookupService(serviceName);
  }

  protected synchronized ServiceCatalog getDelegate() {
    if (delegate == null)
      delegate = createDelegate();

    return delegate;
  }

  protected abstract ServiceCatalog createDelegate();
}
