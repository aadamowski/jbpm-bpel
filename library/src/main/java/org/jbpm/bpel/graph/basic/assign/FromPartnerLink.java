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

import org.apache.commons.lang.enums.Enum;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.JbpmContext;
import org.jbpm.bpel.endpointref.EndpointReference;
import org.jbpm.bpel.integration.IntegrationService;
import org.jbpm.bpel.integration.def.PartnerLinkDefinition;
import org.jbpm.bpel.integration.def.ReceiveAction;
import org.jbpm.graph.exe.Token;

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/07/22 05:57:25 $
 */
public class FromPartnerLink extends From {

  private PartnerLinkDefinition partnerLink;
  private Reference endpointReference;

  private static final long serialVersionUID = 1L;

  private static final Log log = LogFactory.getLog(FromPartnerLink.class);

  public Object extract(Token token) {
    log.debug("extracting " + endpointReference.getName() + " of " + partnerLink + " for " + token);

    EndpointReference reference;
    if (Reference.PARTNER_ROLE.equals(endpointReference)) {
      reference = partnerLink.getInstance(token).getPartnerReference();
    }
    else {
      IntegrationService integrationService = ReceiveAction.getIntegrationService(JbpmContext.getCurrentJbpmContext());
      reference = integrationService.getMyReference(partnerLink, token);
    }
    return reference;
  }

  public PartnerLinkDefinition getPartnerLink() {
    return partnerLink;
  }

  public void setPartnerLink(PartnerLinkDefinition partnerLink) {
    this.partnerLink = partnerLink;
  }

  public Reference getEndpointReference() {
    return endpointReference;
  }

  public void setEndpointReference(Reference endpointReference) {
    this.endpointReference = endpointReference;
  }

  public static final class Reference extends Enum {

    public static final Reference MY_ROLE = new Reference("myRole");

    public static final Reference PARTNER_ROLE = new Reference("partnerRole");

    private static final long serialVersionUID = 1L;

    /**
     * Enumeration constructor.
     * @param name the desired textual representation.
     */
    private Reference(String name) {
      super(name);
    }

    /**
     * Gets an enumeration object by name.
     * @param name a string that identifies one element
     * @return the appropiate enumeration object, or <code>null</code> if the object does not
     * exist
     */
    public static Reference valueOf(String name) {
      return (Reference) getEnum(Reference.class, name);
    }
  }
}