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
package org.jbpm.bpel;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.jbpm.bpel.alarm.AlarmActionDbTest;
import org.jbpm.bpel.alarm.AlarmExeDbTest;
import org.jbpm.bpel.endpointref.wsa.WsaEndpointReferenceDbTest;
import org.jbpm.bpel.endpointref.wsdl.WsdlEndpointReferenceDbTest;
import org.jbpm.bpel.graph.basic.AssignDbTest;
import org.jbpm.bpel.graph.basic.InvokeDbTest;
import org.jbpm.bpel.graph.basic.ReceiveDbTest;
import org.jbpm.bpel.graph.basic.ReplyDbTest;
import org.jbpm.bpel.graph.basic.ThrowDbTest;
import org.jbpm.bpel.graph.basic.WaitDbTest;
import org.jbpm.bpel.graph.def.BpelProcessDefinitionDbTest;
import org.jbpm.bpel.graph.def.ImportDbTest;
import org.jbpm.bpel.graph.exe.FaultInstanceDbTest;
import org.jbpm.bpel.graph.exe.LinkInstanceDbTest;
import org.jbpm.bpel.graph.exe.ScopeInstanceDbTest;
import org.jbpm.bpel.graph.scope.CatchAllDbTest;
import org.jbpm.bpel.graph.scope.CatchDbTest;
import org.jbpm.bpel.graph.scope.CompensateDbTest;
import org.jbpm.bpel.graph.scope.CompensateScopeDbTest;
import org.jbpm.bpel.graph.scope.CompensationHandlerDbTest;
import org.jbpm.bpel.graph.scope.OnAlarmDbTest;
import org.jbpm.bpel.graph.scope.OnEventDbTest;
import org.jbpm.bpel.graph.scope.ScopeDbTest;
import org.jbpm.bpel.graph.scope.TerminationHandlerDbTest;
import org.jbpm.bpel.graph.struct.FlowDbTest;
import org.jbpm.bpel.graph.struct.IfDbTest;
import org.jbpm.bpel.graph.struct.PickDbTest;
import org.jbpm.bpel.graph.struct.RepeatUntilDbTest;
import org.jbpm.bpel.graph.struct.SequenceDbTest;
import org.jbpm.bpel.graph.struct.WhileDbTest;
import org.jbpm.bpel.integration.catalog.CatalogEntryDbTest;
import org.jbpm.bpel.integration.def.CorrelationDbTest;
import org.jbpm.bpel.integration.def.CorrelationSetDefinitionDbTest;
import org.jbpm.bpel.integration.def.InvokeActionDbTest;
import org.jbpm.bpel.integration.def.PartnerLinkDefinitionDbTest;
import org.jbpm.bpel.integration.def.ReceiveActionDbTest;
import org.jbpm.bpel.integration.def.ReplyActionDbTest;
import org.jbpm.bpel.integration.exe.CorrelationSetInstanceDbTest;
import org.jbpm.bpel.integration.exe.PartnerLinkInstanceDbTest;
import org.jbpm.bpel.persistence.db.BpelGraphSessionDbTest;
import org.jbpm.bpel.persistence.db.IntegrationSessionDbTest;
import org.jbpm.bpel.persistence.db.ScopeSessionDbTest;
import org.jbpm.bpel.persistence.db.type.QNameTypeDbTest;
import org.jbpm.bpel.sublang.def.ExpressionDbTest;
import org.jbpm.bpel.sublang.def.JoinConditionDbTest;
import org.jbpm.bpel.sublang.def.PropertyQueryDbTest;
import org.jbpm.bpel.sublang.def.VariableQueryDbTest;
import org.jbpm.bpel.sublang.xpath.GetTokenIdDbTest;
import org.jbpm.bpel.variable.def.MessageTypeDbTest;
import org.jbpm.bpel.variable.def.VariableDefinitionDbTest;
import org.jbpm.bpel.variable.def.XmlTypeDbTest;
import org.jbpm.bpel.variable.exe.ElementInstanceDbTest;
import org.jbpm.bpel.variable.exe.ElementValueDbTest;
import org.jbpm.bpel.variable.exe.MessageValueDbTest;
import org.jbpm.bpel.variable.exe.SchemaValueDbTest;
import org.jbpm.bpel.wsdl.impl.MessageImplDbTest;
import org.jbpm.bpel.wsdl.impl.OperationImplDbTest;
import org.jbpm.bpel.wsdl.impl.PartnerLinkTypeImplDbTest;
import org.jbpm.bpel.wsdl.impl.PropertyImplDbTest;

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2008/01/30 08:15:35 $
 */
public class AllDbTests {

  public static Test suite() {
    TestSuite suite = new TestSuite("all db tests");

    // graph definition
    suite.addTestSuite(BpelProcessDefinitionDbTest.class);
    suite.addTestSuite(ImportDbTest.class);

    // basic activity
    suite.addTestSuite(AssignDbTest.class);
    suite.addTestSuite(CompensateDbTest.class);
    suite.addTestSuite(CompensateScopeDbTest.class);
    suite.addTestSuite(InvokeDbTest.class);
    suite.addTestSuite(ReceiveDbTest.class);
    suite.addTestSuite(ReplyDbTest.class);
    suite.addTestSuite(ThrowDbTest.class);
    suite.addTestSuite(WaitDbTest.class);

    // structured activity
    suite.addTestSuite(FlowDbTest.class);
    suite.addTestSuite(IfDbTest.class);
    suite.addTestSuite(PickDbTest.class);
    suite.addTestSuite(SequenceDbTest.class);
    suite.addTestSuite(WhileDbTest.class);
    suite.addTestSuite(RepeatUntilDbTest.class);

    // scope & handler
    suite.addTestSuite(CatchDbTest.class);
    suite.addTestSuite(CatchAllDbTest.class);
    suite.addTestSuite(OnAlarmDbTest.class);
    suite.addTestSuite(OnEventDbTest.class);
    suite.addTestSuite(ScopeDbTest.class);
    suite.addTestSuite(CompensationHandlerDbTest.class);
    suite.addTestSuite(TerminationHandlerDbTest.class);

    // graph runtime
    suite.addTestSuite(FaultInstanceDbTest.class);
    suite.addTestSuite(ScopeInstanceDbTest.class);
    suite.addTestSuite(LinkInstanceDbTest.class);
    suite.addTestSuite(ScopeSessionDbTest.class);
    suite.addTestSuite(BpelGraphSessionDbTest.class);

    // variable definition
    suite.addTestSuite(VariableDefinitionDbTest.class);
    suite.addTestSuite(XmlTypeDbTest.class);
    suite.addTestSuite(MessageTypeDbTest.class);

    // variable runtime
    suite.addTestSuite(SchemaValueDbTest.class);
    suite.addTestSuite(ElementValueDbTest.class);
    suite.addTestSuite(MessageValueDbTest.class);
    suite.addTestSuite(ElementInstanceDbTest.class);
    suite.addTestSuite(QNameTypeDbTest.class);

    // sublang definition
    suite.addTestSuite(ExpressionDbTest.class);
    suite.addTestSuite(JoinConditionDbTest.class);
    suite.addTestSuite(PropertyQueryDbTest.class);
    suite.addTestSuite(VariableQueryDbTest.class);

    // sublang xpath
    suite.addTestSuite(GetTokenIdDbTest.class);

    // integration definition
    suite.addTestSuite(ReceiveActionDbTest.class);
    suite.addTestSuite(ReplyActionDbTest.class);
    suite.addTestSuite(InvokeActionDbTest.class);
    suite.addTestSuite(CorrelationDbTest.class);
    suite.addTestSuite(CorrelationSetDefinitionDbTest.class);
    suite.addTestSuite(PartnerLinkDefinitionDbTest.class);

    // integration runtime
    suite.addTestSuite(CorrelationSetInstanceDbTest.class);
    suite.addTestSuite(PartnerLinkInstanceDbTest.class);
    suite.addTestSuite(WsaEndpointReferenceDbTest.class);
    suite.addTestSuite(WsdlEndpointReferenceDbTest.class);
    suite.addTest(IntegrationSessionDbTest.suite());

    // integration catalog
    suite.addTestSuite(CatalogEntryDbTest.class);

    // wsdl
    suite.addTestSuite(MessageImplDbTest.class);
    suite.addTestSuite(OperationImplDbTest.class);
    suite.addTestSuite(PartnerLinkTypeImplDbTest.class);
    suite.addTestSuite(PropertyImplDbTest.class);

    // alarm
    suite.addTestSuite(AlarmActionDbTest.class);
    suite.addTestSuite(AlarmExeDbTest.class);

    return suite;
  }
}