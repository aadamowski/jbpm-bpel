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
package org.jbpm.bpel.tools;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/11/06 22:06:26 $
 */
public class DeploymentException extends Exception {

  private static final long serialVersionUID = 1L;

  public DeploymentException(String message) {
    super(message);
  }

  public DeploymentException(String message, Throwable cause) {
    super(message, cause);
  }
}
