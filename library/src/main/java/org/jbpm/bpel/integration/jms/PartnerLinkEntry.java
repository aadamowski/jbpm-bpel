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

import javax.jms.Destination;

import org.jbpm.bpel.deploy.PartnerRoleDescriptor.InitiateMode;
import org.jbpm.bpel.endpointref.EndpointReference;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/10/13 02:53:30 $
 */
public class PartnerLinkEntry {

  private long id;

  private Destination destination;
  private EndpointReference myReference;

  private InitiateMode initiateMode;
  private EndpointReference partnerReference;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public Destination getDestination() {
    return destination;
  }

  public void setDestination(Destination destination) {
    this.destination = destination;
  }

  public EndpointReference getMyReference() {
    return myReference;
  }

  public void setMyReference(EndpointReference myReference) {
    this.myReference = myReference;
  }

  public InitiateMode getInitiateMode() {
    return initiateMode;
  }

  public void setInitiateMode(InitiateMode initiateMode) {
    this.initiateMode = initiateMode;
  }

  public EndpointReference getPartnerReference() {
    return partnerReference;
  }

  public void setPartnerReference(EndpointReference partnerReference) {
    this.partnerReference = partnerReference;
  }
}
