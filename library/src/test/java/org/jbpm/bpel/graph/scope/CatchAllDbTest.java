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
package org.jbpm.bpel.graph.scope;

import org.jbpm.bpel.graph.def.BpelProcessDefinition;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/03/16 00:04:38 $
 */
public class CatchAllDbTest extends AbstractHandlerDbTestCase {

  protected Handler createHandler(BpelProcessDefinition process) {
    Handler handler = new Handler();
    process.getGlobalScope().setCatchAll(handler);
    return handler;
  }

  protected Handler getHandler(BpelProcessDefinition process) {
    return process.getGlobalScope().getCatchAll();
  }
}
