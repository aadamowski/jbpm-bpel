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
package org.jbpm.bpel.graph.def;

import org.jbpm.bpel.graph.basic.Assign;
import org.jbpm.bpel.graph.basic.Empty;
import org.jbpm.bpel.graph.basic.Exit;
import org.jbpm.bpel.graph.basic.Invoke;
import org.jbpm.bpel.graph.basic.Receive;
import org.jbpm.bpel.graph.basic.Reply;
import org.jbpm.bpel.graph.basic.Rethrow;
import org.jbpm.bpel.graph.basic.Throw;
import org.jbpm.bpel.graph.basic.Validate;
import org.jbpm.bpel.graph.basic.Wait;
import org.jbpm.bpel.graph.scope.Compensate;
import org.jbpm.bpel.graph.scope.CompensateScope;
import org.jbpm.bpel.graph.scope.Scope;
import org.jbpm.bpel.graph.struct.Flow;
import org.jbpm.bpel.graph.struct.If;
import org.jbpm.bpel.graph.struct.Pick;
import org.jbpm.bpel.graph.struct.RepeatUntil;
import org.jbpm.bpel.graph.struct.Sequence;
import org.jbpm.bpel.graph.struct.While;

/**
 * @author Juan Cantu
 * @version $Revision$ $Date: 2008/01/30 08:15:33 $
 */
public interface BpelVisitor {

  public void visit(BpelProcessDefinition process);

  public void visit(Empty empty);

  public void visit(Receive receive);

  public void visit(Reply reply);

  public void visit(Invoke invoke);

  public void visit(Assign assign);

  public void visit(Throw _throw);

  public void visit(Exit exit);

  public void visit(Wait wait);

  public void visit(Sequence sequence);

  public void visit(If _if);

  public void visit(While _while);

  public void visit(RepeatUntil repeatUntil);

  public void visit(Pick pick);

  public void visit(Flow flow);

  public void visit(Scope scope);

  public void visit(Compensate compensate);

  public void visit(CompensateScope compensateScope);

  public void visit(Rethrow rethrow);

  public void visit(Validate validate);
}