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
package org.jbpm.bpel.graph.basic.assign;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;

import org.jbpm.bpel.endpointref.EndpointReference;
import org.jbpm.bpel.graph.exe.BpelFaultException;
import org.jbpm.bpel.integration.def.PartnerLinkDefinition;
import org.jbpm.bpel.integration.exe.PartnerLinkInstance;
import org.jbpm.bpel.xml.BpelConstants;
import org.jbpm.graph.exe.Token;

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/07/22 05:57:25 $
 */
public class ToPartnerLink extends To {

  private PartnerLinkDefinition partnerLink;

  private static final long serialVersionUID = 1L;

  private static final Log log = LogFactory.getLog(ToPartnerLink.class);

  public void assign(Token token, Object value) {
    log.debug("assigning " + partnerLink + " for " + token);

    EndpointReference reference;
    if (value instanceof EndpointReference)
      reference = (EndpointReference) value;
    else if (value instanceof Element)
      reference = EndpointReference.readServiceRef((Element) value);
    else
      throw new BpelFaultException(BpelConstants.FAULT_MISMATCHED_ASSIGNMENT);

    PartnerLinkInstance instance = partnerLink.getInstance(token);
    instance.setPartnerReference(reference);
  }

  public PartnerLinkDefinition getPartnerLink() {
    return partnerLink;
  }

  public void setPartnerLink(PartnerLinkDefinition partnerLink) {
    this.partnerLink = partnerLink;
  }
}
