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
package org.jbpm.bpel.graph.exe;

import javax.xml.namespace.QName;

import org.jbpm.bpel.BpelException;

/**
 * @author Juan Cantu
 * @version $Revision$ $Date: 2006/10/29 06:13:41 $
 */
public class BpelFaultException extends BpelException {

  private FaultInstance faultInstance;

  private static final long serialVersionUID = 1L;

  public BpelFaultException(FaultInstance faultInstance) {
    super(faultInstance.toString());
    this.faultInstance = faultInstance;
  }

  public BpelFaultException(QName name) {
    this(new FaultInstance(name));
  }

  public FaultInstance getFaultInstance() {
    return faultInstance;
  }
}