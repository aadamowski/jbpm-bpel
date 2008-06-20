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
package org.jbpm.bpel.integration.def;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jbpm.bpel.integration.def.Correlation.Initiate;
import org.jbpm.bpel.integration.exe.CorrelationSetInstance;
import org.jbpm.bpel.variable.exe.MessageValue;
import org.jbpm.graph.exe.Token;

/**
 * Groups the correlations ocurring in the message being sent or received in an
 * activity.
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/03/22 13:32:55 $
 */
public class Correlations implements Serializable {

  private static final long serialVersionUID = 1L;

  long id;
  private Map correlations;

  public Correlations() {
  }

  public void addCorrelation(Correlation correlation) {
    if (correlations == null) {
      correlations = new HashMap();
    }
    correlations.put(correlation.getSet().getName(), correlation);
  }

  public Correlation getCorrelation(String setName) {
    return (Correlation) correlations.get(setName);
  }

  public Map getCorrelations() {
    return correlations;
  }

  public void setCorrelations(Map correlations) {
    this.correlations = correlations;
  }

  public Map getReceptionProperties(Token token) {
    Map receptionProperties = new HashMap();
    Iterator correlationIter = correlations.values().iterator();
    while (correlationIter.hasNext()) {
      Correlation correlation = (Correlation) correlationIter.next();
      Initiate initiate = correlation.getInitiate();
      if (initiate != Initiate.YES) {
        CorrelationSetInstance setInstance = correlation.getSet().getInstance(
            token);
        if (initiate == Initiate.NO || setInstance.isInitialized()) {
          receptionProperties.putAll(setInstance.getProperties());
        }
      }
    }
    return receptionProperties;
  }

  public void ensureConstraint(MessageValue messageValue, Token token) {
    Iterator correlationIter = correlations.values().iterator();
    while (correlationIter.hasNext()) {
      Correlation correlation = (Correlation) correlationIter.next();
      CorrelationSetInstance setInstance = correlation.getSet().getInstance(
          token);
      Initiate initiate = correlation.getInitiate();
      if (initiate == Initiate.YES) {
        setInstance.initialize(messageValue);
      }
      else if (initiate == Initiate.NO) {
        setInstance.validateConstraint(messageValue);
      }
      else if (initiate == Initiate.JOIN) {
        if (setInstance.isInitialized())
          setInstance.validateConstraint(messageValue);
        else
          setInstance.initialize(messageValue);
      }
      else
        throw new IllegalStateException("illegal property value: initiate="
            + initiate);
    }
  }
}
