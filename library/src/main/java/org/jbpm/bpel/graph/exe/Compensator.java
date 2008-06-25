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

/**
 * Contract that compensation requestors must adhere for receiving scope
 * compensation notifications from the jBPM BPEL scope state machine.
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/03/16 00:04:38 $
 */
public interface Compensator {

  public void scopeCompensated(ScopeInstance scope);
}
