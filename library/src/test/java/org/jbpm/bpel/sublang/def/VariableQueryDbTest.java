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

import org.jbpm.bpel.graph.basic.Assign;
import org.jbpm.bpel.graph.basic.assign.Copy;
import org.jbpm.bpel.graph.basic.assign.FromVariable;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/09/12 23:20:17 $
 */
public class VariableQueryDbTest extends SnippetDbTestCase {

  protected Snippet createSnippet(BpelProcessDefinition processDefinition) {
    VariableQuery query = new VariableQuery();

    FromVariable from = new FromVariable();
    from.setQuery(query);

    Copy copy = new Copy();
    copy.setFrom(from);

    Assign assign = new Assign();
    assign.addOperation(copy);

    processDefinition.getGlobalScope().setActivity(assign);

    return query;
  }

  protected Snippet getSnippet(BpelProcessDefinition processDefinition) {
    Assign assign = (Assign) session.load(Assign.class, new Long(processDefinition.getGlobalScope()
        .getActivity()
        .getId()));
    Copy copy = (Copy) assign.getOperations().get(0);
    FromVariable from = (FromVariable) session.load(FromVariable.class,
        session.getIdentifier(copy.getFrom()));
    return from.getQuery();
  }
}