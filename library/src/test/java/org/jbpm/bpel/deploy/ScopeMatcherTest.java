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
package org.jbpm.bpel.deploy;

import java.io.StringReader;
import java.util.Map;

import javax.xml.transform.dom.DOMSource;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.xml.sax.InputSource;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.bpel.BpelException;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.scope.Scope;
import org.jbpm.bpel.graph.struct.Sequence;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.bpel.xml.BpelReader;
import org.jbpm.bpel.xml.DeploymentDescriptorReader;
import org.jbpm.bpel.xml.util.XmlUtil;

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/11/25 13:03:15 $
 */
public class ScopeMatcherTest extends TestCase {

  private DeploymentDescriptorReader deploymentDescriptorReader = new DeploymentDescriptorReader();
  private BpelProcessDefinition processDefinition;
  private ScopeMatcher scopeMatcher;

  private JbpmContext jbpmContext;

  private static DOMSource processSource;

  protected void setUp() throws Exception {
    /*
     * the reader accesses the jbpm configuration, so create a context before creating the reader to
     * avoid loading another configuration from the default resource
     */
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.getInstance("org/jbpm/bpel/graph/exe/test.jbpm.cfg.xml");
    jbpmContext = jbpmConfiguration.createJbpmContext();

    processDefinition = new BpelProcessDefinition();
    new BpelReader().read(processDefinition, processSource);
    scopeMatcher = new ScopeMatcher(processDefinition);
  }

  protected void tearDown() throws Exception {
    jbpmContext.close();
  }

  public void testMatchProcessDescriptor() throws Exception {
    String text = "<bpelDeployment xmlns='"
        + BpelConstants.NS_DEPLOYMENT_DESCRIPTOR
        + "'>"
        + " <partnerLinks>"
        + "  <partnerLink name='schedulingPL'>"
        + "   <myRole handle='globalScheduling'/>"
        + "  </partnerLink>"
        + " </partnerLinks>"
        + "</bpelDeployment>";

    DeploymentDescriptor deploymentDescriptor = new DeploymentDescriptor();
    deploymentDescriptorReader.read(deploymentDescriptor, new InputSource(new StringReader(text)));

    scopeMatcher.visit(deploymentDescriptor);
    Map scopeDescriptors = scopeMatcher.getScopeDescriptors();

    assertSame(deploymentDescriptor, scopeDescriptors.get(processDefinition.getGlobalScope()));
  }

  public void testMatchScopeDescriptor() throws Exception {
    String text = "<bpelDeployment xmlns='"
        + BpelConstants.NS_DEPLOYMENT_DESCRIPTOR
        + "'>"
        + " <scopes>"
        + "  <scope name='s1'>"
        + "   <scopes>"
        + "    <scope name='s1.1'/>"
        + "   </scopes>"
        + "  </scope>"
        + " </scopes>"
        + "</bpelDeployment>";

    DeploymentDescriptor deploymentDescriptor = new DeploymentDescriptor();
    deploymentDescriptorReader.read(deploymentDescriptor, new InputSource(new StringReader(text)));

    Sequence sequence = (Sequence) processDefinition.getGlobalScope().getActivity();
    Scope s1Def = (Scope) sequence.getNodes().get(1);
    ScopeDescriptor s1Config = (ScopeDescriptor) deploymentDescriptor.getScopes().get(0);

    Scope s11Def = (Scope) s1Def.getActivity();
    ScopeDescriptor s11Config = (ScopeDescriptor) s1Config.getScopes().get(0);

    scopeMatcher.visit(deploymentDescriptor);
    Map scopeDescriptors = scopeMatcher.getScopeDescriptors();

    assertSame(s1Config, scopeDescriptors.get(s1Def));
    assertSame(s11Config, scopeDescriptors.get(s11Def));
  }

  public void testMatchUnnamedScopeDescriptor() throws Exception {
    String text = "<bpelDeployment xmlns='"
        + BpelConstants.NS_DEPLOYMENT_DESCRIPTOR
        + "'>"
        + " <scopes>"
        + "  <scope />"
        + " </scopes>"
        + "</bpelDeployment>";

    DeploymentDescriptor deploymentDescriptor = new DeploymentDescriptor();
    deploymentDescriptorReader.read(deploymentDescriptor, new InputSource(new StringReader(text)));
    ScopeDescriptor s1Descriptor = (ScopeDescriptor) deploymentDescriptor.getScopes().get(0);

    scopeMatcher.visit(deploymentDescriptor);
    Map scopeDescriptors = scopeMatcher.getScopeDescriptors();

    Sequence sequence = (Sequence) processDefinition.getGlobalScope().getActivity();
    Scope s1Def = (Scope) sequence.getNodes().get(2);

    assertSame(s1Descriptor, scopeDescriptors.get(s1Def));
  }

  public void testConflictingScopeDescriptor() throws Exception {
    String text = "<bpelDeployment xmlns='"
        + BpelConstants.NS_DEPLOYMENT_DESCRIPTOR
        + "'>"
        + " <scopes>"
        + "  <scope name='conflictingName' />"
        + " </scopes>"
        + "</bpelDeployment>";

    DeploymentDescriptor deploymentDescriptor = new DeploymentDescriptor();
    deploymentDescriptorReader.read(deploymentDescriptor, new InputSource(new StringReader(text)));

    try {
      scopeMatcher.visit(deploymentDescriptor);
      fail("conflicting name resolution must fail");
    }
    catch (BpelException e) {
      // this exception is expected
    }
  }

  public static Test suite() {
    return new Setup();
  }

  private static class Setup extends TestSetup {

    private Setup() {
      super(new TestSuite(ScopeMatcherTest.class));
    }

    protected void setUp() throws Exception {
      processSource = XmlUtil.parseResource("mergeProcess.bpel", ScopeMatcherTest.class);
    }

    protected void tearDown() throws Exception {
      processSource = null;
    }
  }
}
