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
package org.jbpm.bpel.integration.jms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jms.Destination;
import javax.naming.Context;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.bpel.BpelException;
import org.jbpm.bpel.deploy.MyRoleDescriptor;
import org.jbpm.bpel.deploy.PartnerLinkDescriptor;
import org.jbpm.bpel.deploy.PartnerRoleDescriptor;
import org.jbpm.bpel.deploy.ScopeDescriptor;
import org.jbpm.bpel.deploy.PartnerRoleDescriptor.InitiateMode;
import org.jbpm.bpel.endpointref.EndpointReference;
import org.jbpm.bpel.endpointref.EndpointReferenceFactory;
import org.jbpm.bpel.graph.def.AbstractBpelVisitor;
import org.jbpm.bpel.graph.scope.Scope;
import org.jbpm.bpel.integration.def.PartnerLinkDefinition;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/10/13 02:53:30 $
 */
class PartnerLinkEntriesBuilder extends AbstractBpelVisitor {

  private final Map scopeDescriptors;
  private final Context jmsContext;
  private final Destination defaultDestination;

  private List partnerLinkEntries = new ArrayList();

  private static final Log log = LogFactory.getLog(PartnerLinkEntriesBuilder.class);

  PartnerLinkEntriesBuilder(Map scopeDescriptors, Context jmsContext, Destination defaultDestination) {
    this.scopeDescriptors = scopeDescriptors;
    this.jmsContext = jmsContext;
    this.defaultDestination = defaultDestination;
  }

  public List getPartnerLinkEntries() {
    return partnerLinkEntries;
  }

  public void visit(Scope scope) {
    Map appPartnerLinks;
    // extract partner link descriptors and the default destination name
    ScopeDescriptor scopeDescriptor = (ScopeDescriptor) scopeDescriptors.get(scope);
    if (scopeDescriptor != null) {
      // take partner link descriptors
      appPartnerLinks = scopeDescriptor.getPartnerLinks();
    }
    else {
      // there is no scope descriptor, so there are no partner link descriptors
      appPartnerLinks = Collections.EMPTY_MAP;
    }
    Iterator partnerLinkIt = scope.getPartnerLinks().values().iterator();
    while (partnerLinkIt.hasNext()) {
      PartnerLinkDefinition definition = (PartnerLinkDefinition) partnerLinkIt.next();
      PartnerLinkDescriptor descriptor = (PartnerLinkDescriptor) appPartnerLinks.get(definition.getName());

      PartnerLinkEntry entry = buildEntry(definition, descriptor);
      partnerLinkEntries.add(entry);
    }
    // propagate visit
    scope.getActivity().accept(this);
  }

  protected PartnerLinkEntry buildEntry(PartnerLinkDefinition definition, PartnerLinkDescriptor descriptor) {
    PartnerLinkEntry entry = new PartnerLinkEntry();
    entry.setId(definition.getId());

    if (definition.getMyRole() != null) {
      // my reference contains only the port type name
      EndpointReferenceFactory referenceFactory = EndpointReferenceFactory.getInstance(
          IntegrationConstants.DEFAULT_REFERENCE_NAME, null);
      EndpointReference myReference = referenceFactory.createEndpointReference();
      myReference.setPortTypeName(definition.getMyRole().getPortType().getQName());
      entry.setMyReference(myReference);

      // the default handle is the partner link name
      String handle = definition.getName();

      // my role descriptor may override the defaults
      if (descriptor != null) {
        MyRoleDescriptor myRole = descriptor.getMyRole();
        if (myRole != null) {
          // override handle
          if (myRole.getHandle() != null)
            handle = myRole.getHandle();
          // complete my reference
          myReference.setServiceName(myRole.getService());
          myReference.setPortName(myRole.getPort());
        }
      }

      // establish the jms destination
      Destination destination = lookupDestination(handle);
      entry.setDestination(destination);

      log.debug("configured my role: partnerLink="
          + definition.getName()
          + ", reference="
          + myReference
          + ", destination="
          + destination);
    }

    if (definition.getPartnerRole() != null) {
      // the default initiate mode is pull a reference from the catalog
      InitiateMode initiateMode = InitiateMode.PULL;
      // the default partner reference is left unspecified
      EndpointReference partnerReference = null;

      // partner role descriptor may override the defaults
      if (descriptor != null) {
        PartnerRoleDescriptor partnerRole = descriptor.getPartnerRole();
        if (partnerRole != null) {
          // override initiate mode
          if (partnerRole.getInitiateMode() != null)
            initiateMode = partnerRole.getInitiateMode();
          // initialize partner endpoint reference
          partnerReference = partnerRole.getEndpointReference();
        }
      }

      entry.setInitiateMode(initiateMode);
      entry.setPartnerReference(partnerReference);

      log.debug("configured partner role: partnerLink="
          + definition.getName()
          + ", initiateMode="
          + initiateMode
          + ", reference="
          + partnerReference);
    }

    return entry;
  }

  private Destination lookupDestination(String partnerLinkHandle) {
    Destination destination;
    try {
      destination = (Destination) jmsContext.lookup(partnerLinkHandle);
      log.debug("retrieved jms destination: " + partnerLinkHandle);
    }
    catch (NamingException e) {
      if (defaultDestination == null)
        throw new BpelException("could not retrieve jms destination: " + partnerLinkHandle, e);
      // fall back to default destination
      destination = defaultDestination;
    }
    return destination;
  }
}