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
package org.jbpm.bpel;

import org.jbpm.JbpmException;

/**
 * Base of all jBPM BPEL exceptions.
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2006/09/27 03:53:07 $
 */
public class BpelException extends JbpmException {

  private static final long serialVersionUID = 1L;

  public BpelException(String message) {
    super(message);
  }

  public BpelException(String message, Exception e) {
    super(message, e);
  }
}