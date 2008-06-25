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
package org.jbpm.bpel.sublang.def;

import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.LinkDefinition;

/**
 * Join conditions are boolean expressions used to specify requirements about
 * the status of {@linkplain LinkDefinition links} targeting the enclosing
 * {@linkplain Activity#getJoinCondition() activity}.
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/01/22 00:24:55 $
 */
public class JoinCondition extends Expression {

  private static final long serialVersionUID = 1L;
}
