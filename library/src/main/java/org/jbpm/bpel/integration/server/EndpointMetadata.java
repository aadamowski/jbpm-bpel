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
package org.jbpm.bpel.integration.server;

import javax.servlet.ServletContext;
import javax.wsdl.Definition;
import javax.xml.namespace.QName;

import org.jbpm.bpel.integration.soap.FaultFormat;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/04/11 06:37:35 $
 */
public class EndpointMetadata {

  private ServletContext servletContext;

  private Definition wsdlDefinition;

  private QName serviceName;

  private String portName;

  private FaultFormat faultFormat;

  public ServletContext getServletContext() {
    return servletContext;
  }

  public void setServletContext(ServletContext servletContext) {
    this.servletContext = servletContext;
  }

  public Definition getWsdlDefinition() {
    return wsdlDefinition;
  }

  public void setWsdlDefinition(Definition definition) {
    this.wsdlDefinition = definition;
  }

  public QName getServiceName() {
    return serviceName;
  }

  public void setServiceName(QName serviceName) {
    this.serviceName = serviceName;
  }

  public String getPortName() {
    return portName;
  }

  public void setPortName(String portName) {
    this.portName = portName;
  }

  public FaultFormat getFaultFormat() {
    return faultFormat;
  }

  public void setFaultFormat(FaultFormat faultFormat) {
    this.faultFormat = faultFormat;
  }
}
