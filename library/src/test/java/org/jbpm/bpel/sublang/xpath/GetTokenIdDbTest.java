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
package org.jbpm.bpel.sublang.xpath;

import org.jaxen.Context;
import org.jaxen.ContextSupport;
import org.jaxen.FunctionCallException;

import org.jbpm.JbpmContext;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.persistence.db.AbstractDbTestCase;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/08/08 11:13:28 $
 */
public class GetTokenIdDbTest extends AbstractDbTestCase {

  private Context jaxenContext;
  private long tokenId;

  private static final String TOKEN_NAME = "t";

  private static final String VARIABLE_NAME = "countryCode";
  private static final Object VARIABLE_VALUE = new Integer(52);

  protected void setUp() throws Exception {
    super.setUp();

    // process definition
    BpelProcessDefinition processDefinition = new BpelProcessDefinition("pd",
        BpelConstants.NS_EXAMPLES);
    bpelGraphSession.deployProcessDefinition(processDefinition);

    // token
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    Token token = new Token(processInstance.getRootToken(), TOKEN_NAME);
    tokenId = token.getId();

    // variable
    processInstance.getContextInstance().setVariable(VARIABLE_NAME, VARIABLE_VALUE, token);

    // jaxen context
    ContextSupport support = new ContextSupport();
    support.setVariableContext(new TokenVariableContext(token));

    jaxenContext = new Context(support);
  }

  public void testEvaluate() throws FunctionCallException {
    assertEquals(tokenId, GetTokenIdFunction.evaluate(jaxenContext).longValue());

    // load token from a separate context
    JbpmContext otherJbpmContext = jbpmConfiguration.createJbpmContext();
    try {
      Token token = otherJbpmContext.loadToken(tokenId);
      // assert it can be accessed
      assertEquals(TOKEN_NAME, token.getName());
      assertEquals(VARIABLE_VALUE, token.getProcessInstance().getContextInstance().getVariable(
          VARIABLE_NAME, token));
    }
    finally {
      otherJbpmContext.close();
    }
  }
}
