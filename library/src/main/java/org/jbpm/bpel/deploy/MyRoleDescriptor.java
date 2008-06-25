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
package org.jbpm.bpel.deploy;

import javax.xml.namespace.QName;

/**
 * Binding of <tt>myRole</tt> element.
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/10/13 02:53:24 $
 */
public class MyRoleDescriptor {

  private String handle;
  private QName service;
  private String port;

  public String getHandle() {
    return handle;
  }

  public void setHandle(String handle) {
    this.handle = handle;
  }

  public QName getService() {
    return service;
  }

  public void setService(QName service) {
    this.service = service;
  }

  public String getPort() {
    return port;
  }

  public void setPort(String port) {
    this.port = port;
  }
}
