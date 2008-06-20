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

import org.jbpm.bpel.graph.basic.Empty;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.scope.Scope;
import org.jbpm.bpel.graph.struct.Pick;
import org.jbpm.bpel.integration.def.Correlation;
import org.jbpm.bpel.integration.def.CorrelationSetDefinition;
import org.jbpm.bpel.integration.def.Correlations;
import org.jbpm.bpel.integration.def.ReceiveAction;
import org.jbpm.bpel.persistence.db.AbstractDbTestCase;
import org.jbpm.bpel.xml.BpelConstants;

/**
 * @author Juan Cantu
 * @version $Revision$ $Date: 2007/10/13 02:53:23 $
 */
public class CorrelationDbTest extends AbstractDbTestCase {

  BpelProcessDefinition processDefinition;
  Correlation correlation;

  private static final String CSET_NAME = "cs";

  protected void setUp() throws Exception {
    super.setUp();

    CorrelationSetDefinition set = new CorrelationSetDefinition();
    set.setName(CSET_NAME);

    correlation = new Correlation();
    correlation.setSet(set);

    Correlations correlations = new Correlations();
    correlations.addCorrelation(correlation);

    ReceiveAction receiveAction = new ReceiveAction();
    receiveAction.setCorrelations(correlations);

    Activity activity = new Empty("child");

    Pick pick = new Pick("parent");
    pick.addNode(activity);
    pick.setOnMessage(activity, receiveAction);

    processDefinition = new BpelProcessDefinition("pd", BpelConstants.NS_EXAMPLES);
    Scope globalScope = processDefinition.getGlobalScope();
    globalScope.addCorrelationSet(set);
    globalScope.setActivity(pick);
  }

  public void testInitiateYes() {
    correlation.setInitiate(Correlation.Initiate.YES);

    processDefinition = saveAndReload(processDefinition);
    correlation = getCorrelation();

    assertEquals(Correlation.Initiate.YES, correlation.getInitiate());
  }

  public void testInitiateNo() {
    correlation.setInitiate(Correlation.Initiate.NO);

    processDefinition = saveAndReload(processDefinition);
    correlation = getCorrelation();

    assertEquals(Correlation.Initiate.NO, correlation.getInitiate());
  }

  public void testInitiateJoin() {
    correlation.setInitiate(Correlation.Initiate.JOIN);

    processDefinition = saveAndReload(processDefinition);
    correlation = getCorrelation();

    assertEquals(Correlation.Initiate.JOIN, correlation.getInitiate());
  }

  public void testSet() {
    processDefinition = saveAndReload(processDefinition);
    assertEquals(CSET_NAME, getCorrelation().getSet().getName());
  }

  private Correlation getCorrelation() {
    Pick pick = (Pick) session.load(Pick.class, new Long(processDefinition.getGlobalScope()
        .getActivity()
        .getId()));
    ReceiveAction receptor = (ReceiveAction) pick.getOnMessages().get(0);
    return receptor.getCorrelations().getCorrelation(CSET_NAME);
  }
}
