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
package org.jbpm.bpel.xml;

import org.w3c.dom.Element;

import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.CompositeActivity;
import org.jbpm.bpel.graph.scope.CompensateScope;
import org.jbpm.bpel.graph.scope.Scope;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/11/29 10:16:30 $
 */
public class CompensateScopeReader extends ActivityReader {

  public Activity read(Element activityElem, CompositeActivity parent) {
    CompensateScope compensateScope = new CompensateScope();
    readStandardProperties(activityElem, compensateScope, parent);
    readCompensateScope(activityElem, compensateScope);
    return compensateScope;
  }

  public void readCompensateScope(Element compensateElem, CompensateScope compensateScope) {
    validateNonInitial(compensateElem, compensateScope);

    // target
    String targetName = compensateElem.getAttribute(BpelConstants.ATTR_TARGET);
    Scope target = compensateScope.getScope().findNestedScope(targetName);
    if (target != null)
      compensateScope.setTarget(target);
    else {
      bpelReader.getProblemHandler()
          .add(new ParseProblem("target scope not found", compensateElem));
    }
  }
}
