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

import org.jbpm.bpel.sublang.exe.PropertyQueryEvaluator;
import org.jbpm.bpel.wsdl.PropertyAlias;

/**
 * Used for selection of nodes in {@linkplain PropertyAlias property aliases}.
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/09/12 23:20:15 $
 */
public class PropertyQuery extends Snippet {

  private transient PropertyQueryEvaluator evaluator;

  private static final long serialVersionUID = 1L;

  public PropertyQueryEvaluator getEvaluator() {
    if (evaluator == null)
      parse();

    return evaluator;
  }

  public void parse() {
    evaluator = getEvaluatorFactory().createEvaluator(this);
  }
}
