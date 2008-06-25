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
package org.jbpm.bpel.graph.basic;

import java.io.Serializable;

import org.jbpm.graph.exe.ExecutionContext;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2008/02/04 14:35:47 $
 */
public abstract class AssignOperation implements Serializable {

  long id;

  public abstract void execute(ExecutionContext exeContext);
}
