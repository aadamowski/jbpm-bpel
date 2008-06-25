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

import junit.framework.Test;
import junit.framework.TestSuite;

import org.jbpm.bpel.deploy.ScopeMatcherTest;
import org.jbpm.bpel.par.BpelArchiveParserTest;
import org.jbpm.bpel.par.DescriptorArchiveParserTest;
import org.jbpm.bpel.par.FileArchiveParserTest;
import org.jbpm.bpel.par.ProcessArchiveTest;
import org.jbpm.bpel.xml.util.DurationTest;
import org.jbpm.bpel.xml.util.XmlUtilTest;

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/11/25 13:03:13 $
 */
public class XmlTests {

  public static Test suite() {
    TestSuite suite = new TestSuite("xml tests");

    // process
    suite.addTestSuite(BpelReaderTest.class);
    suite.addTestSuite(ImportReaderTest.class);
    suite.addTestSuite(PartnerLinkReaderTest.class);
    suite.addTestSuite(CorrelationsReaderTest.class);

    // activities
    suite.addTestSuite(ActivityReaderTest.class);
    suite.addTestSuite(InvokeReaderTest.class);
    suite.addTestSuite(ReceiveReaderTest.class);
    suite.addTestSuite(WaitReaderTest.class);
    suite.addTestSuite(SequenceReaderTest.class);
    suite.addTestSuite(FlowReaderTest.class);
    suite.addTestSuite(IfReaderTest.class);
    suite.addTestSuite(SwitchReaderTest.class);
    suite.addTestSuite(WhileReaderTest.class);
    suite.addTestSuite(PickReaderTest.class);
    suite.addTestSuite(ScopeReaderTest.class);
    suite.addTestSuite(ReplyReaderTest.class);
    suite.addTestSuite(CompensateReaderTest.class);
    suite.addTestSuite(CompensateScopeReaderTest.class);
    suite.addTestSuite(ThrowReaderTest.class);
    suite.addTestSuite(AssignReaderTest.class);
    suite.addTestSuite(ValidateReaderTest.class);
    suite.addTestSuite(RepeatUntilReaderTest.class);

    // transformation tests
    suite.addTestSuite(BpelConverterTest.class);
    suite.addTestSuite(WsdlConverterTest.class);

    // application descriptor tests
    suite.addTestSuite(DeploymentDescriptorReaderTest.class);
    suite.addTestSuite(DeploymentDescriptorWriterTest.class);
    suite.addTestSuite(UrlCatalogReaderTest.class);
    suite.addTestSuite(UrlCatalogWriterTest.class);
    suite.addTest(ScopeMatcherTest.suite());

    // process archive tests
    suite.addTestSuite(DefinitionDescriptorReaderTest.class);
    suite.addTestSuite(BpelArchiveParserTest.class);
    suite.addTestSuite(DescriptorArchiveParserTest.class);
    suite.addTestSuite(FileArchiveParserTest.class);
    suite.addTestSuite(ProcessArchiveTest.class);

    // utility tests
    suite.addTestSuite(DurationTest.class);
    suite.addTestSuite(XmlUtilTest.class);

    return suite;
  }
}
