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

import org.jbpm.bpel.graph.basic.Receive;
import org.jbpm.bpel.graph.struct.Flow;
import org.jbpm.graph.exe.Token;

public class ControlDependencyTest extends AbstractExeTestCase {

  /* WS-BPEL 2.0 Issue 200 */
  String xml = "<flow name='F'>"
      + "  <links>"
      + "   <link name='x1'/>"
      + "   <link name='x2'/>"
      + "  </links>"
      + "  <receive name='A1' partnerLink='aPartner' operation='o'>"
      + "   <sources>"
      + "     <source linkName='x1'/>"
      + "   </sources>"
      + " </receive>"
      + " <switch name='Sw'>"
      + "   <case>"
      + "     <condition>'true'</condition>"
      + "     <empty name='A2'/>"
      + "    </case>"
      + "   <otherwise> "
      + "     <empty name='A3'>"
      + "       <targets>"
      + "         <target linkName='x1'/>"
      + "       </targets>"
      + "       <sources>"
      + "         <source linkName='x2'/>"
      + "       </sources>"
      + "     </empty>"
      + "   </otherwise>"
      + " </switch>"
      + " <receive name='A4' partnerLink='aPartner' operation='o'>"
      + "   <targets>"
      + "     <joinCondition>"
      + "        not($x2)"
      + "     </joinCondition>"
      + "     <target linkName='x2'/>"
      + "   </targets>"
      + " </receive>"
      + "</flow>";

  public void testControlDependencyScenario() throws Exception {
    Flow flow = (Flow) readActivity(xml, false);
    plugInner(flow);
    Receive A1 = (Receive) flow.getNode("A1");
    Receive A4 = (Receive) flow.getNode("A4");

    /*
     * The switch gets executed and the A2 path is taken. A negative status is
     * set to A3
     */
    Token token = executeInner();
    Token A1Token = (Token) token.getChildrenAtNode(A1).get(0);
    Token A4Token = (Token) token.getChildrenAtNode(A4).get(0);

    /*
     * A4 is not yet able to receive due to its indirect control dependency to
     * A1
     */
    assertReceiveDisabled(A4Token, A4);
    /* A1 receives a message, link x1 and x2 are determined as a consequence */
    assertReceiveAndAdvance(A1Token, A1, flow.getEnd());
    /* A4 is now able to receive. The execution of this process is completed */
    assertReceiveAndComplete(A4Token, A4);
  }
}
