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

import javax.xml.namespace.QName;

import org.jbpm.bpel.graph.basic.Receive;
import org.jbpm.bpel.graph.def.LinkDefinition;
import org.jbpm.bpel.graph.exe.LinkInstance;
import org.jbpm.bpel.graph.struct.If;
import org.jbpm.bpel.variable.def.SchemaType;
import org.jbpm.bpel.variable.def.VariableDefinition;
import org.jbpm.bpel.variable.def.VariableType;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/03/22 13:32:55 $
 */
public class IfExeTest extends AbstractExeTestCase {

  VariableDefinition condition1;
  VariableDefinition condition2;
  If _if;
  Receive case1R;
  Receive case2R;
  Receive otherwiseR;
  LinkInstance case1L;
  LinkInstance case2L;
  LinkInstance otherwiseL;

  static final String xml = "<if>"
      + "	<condition>$condition1</condition>"
      + "	<receive name='case1' partnerLink='aPartner' operation='o'/>"
      + "	<elseif>"
      + "  <condition>$condition2</condition>"
      + "	 <receive name='case2' partnerLink='aPartner' operation='o'/>"
      + "	</elseif>"
      + "	<else>"
      + "	 <receive name='otherwise' partnerLink='aPartner' operation='o'/>"
      + "	</else>"
      + "</if>";

  protected void setUp() throws Exception {
    super.setUp();
    _if = (If) readActivity(xml, false);
    plugInner(_if);
    initActivities();

    VariableType boolType = new SchemaType(new QName("boolean"));
    condition1 = new VariableDefinition();
    condition1.setName("condition1");
    condition1.setType(boolType);
    scope.addVariable(condition1);
    condition2 = new VariableDefinition();
    condition2.setName("condition2");
    condition2.setType(boolType);
    scope.addVariable(condition2);
  }

  public void initActivities() {
    case1R = (Receive) _if.getNode("case1");
    case2R = (Receive) _if.getNode("case2");
    otherwiseR = (Receive) _if.getNode("otherwise");

    Token root = pi.getRootToken();

    LinkDefinition case1Link = new TestLink("case1");
    case1R.addSource(case1Link);
    case1L = case1Link.createInstance(root);

    LinkDefinition case2Link = new TestLink("case2");
    case2R.addSource(case2Link);
    case2L = case2Link.createInstance(root);

    LinkDefinition otherwiseLink = new TestLink("otherwise");
    otherwiseR.addSource(otherwiseLink);
    otherwiseL = otherwiseLink.createInstance(root);
  }

  public void testFirstBranch() {
    Token normalPath = prepareInner();
    condition1.setValue(normalPath, Boolean.TRUE);
    condition2.setValue(normalPath, Boolean.FALSE);
    firstActivity.leave(new ExecutionContext(normalPath));

    assertReceiveAndComplete(normalPath, case1R);
    assertReceiveDisabled(normalPath, case2R);
    assertReceiveDisabled(normalPath, otherwiseR);

    assertNotNull(case1L.getStatus());
    assertEquals(Boolean.FALSE, case2L.getStatus());
    assertEquals(Boolean.FALSE, otherwiseL.getStatus());
  }

  public void testSecondBranch() {
    Token normalPath = prepareInner();
    condition1.setValue(normalPath, Boolean.FALSE);
    condition2.setValue(normalPath, Boolean.TRUE);
    firstActivity.leave(new ExecutionContext(normalPath));

    assertReceiveAndComplete(normalPath, case2R);
    assertReceiveDisabled(normalPath, case1R);
    assertReceiveDisabled(normalPath, otherwiseR);

    assertNotNull(case2L.getStatus());
    assertEquals(Boolean.FALSE, case1L.getStatus());
    assertEquals(Boolean.FALSE, otherwiseL.getStatus());
  }

  public void testElse() {
    Token normalPath = prepareInner();
    condition1.setValue(normalPath, Boolean.FALSE);
    condition2.setValue(normalPath, Boolean.FALSE);
    firstActivity.leave(new ExecutionContext(normalPath));

    assertReceiveAndComplete(normalPath, otherwiseR);
    assertReceiveDisabled(normalPath, case1R);
    assertReceiveDisabled(normalPath, case2R);

    assertNotNull(otherwiseL.getStatus());
    assertEquals(Boolean.FALSE, case1L.getStatus());
    assertEquals(Boolean.FALSE, case2L.getStatus());
  }

  public void testElseDefault() {
    _if.removeNode(otherwiseR);

    Token normalPath = prepareInner();
    condition1.setValue(normalPath, Boolean.FALSE);
    condition2.setValue(normalPath, Boolean.FALSE);
    firstActivity.leave(new ExecutionContext(normalPath));

    assertReceiveDisabled(normalPath, case1R);
    assertReceiveDisabled(normalPath, case2R);

    assertEquals(Boolean.FALSE, case1L.getStatus());
    assertEquals(Boolean.FALSE, case2L.getStatus());

    assertCompleted(normalPath);
  }
}
