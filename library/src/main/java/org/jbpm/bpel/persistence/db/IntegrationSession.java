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
package org.jbpm.bpel.persistence.db;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import org.jbpm.JbpmContext;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.integration.catalog.CatalogEntry;
import org.jbpm.bpel.integration.def.InvokeAction;
import org.jbpm.bpel.integration.def.PartnerLinkDefinition;
import org.jbpm.bpel.integration.def.ReceiveAction;
import org.jbpm.bpel.integration.def.ReplyAction;
import org.jbpm.bpel.integration.exe.PartnerLinkInstance;

/**
 * Partner integration database operations.
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2008/06/12 08:18:53 $
 */
public class IntegrationSession {

  private final Session session;

  public IntegrationSession(Session session) {
    this.session = session;
  }

  public PartnerLinkDefinition loadPartnerLinkDefinition(long id) {
    return (PartnerLinkDefinition) session.load(PartnerLinkDefinition.class, new Long(id));
  }

  public PartnerLinkInstance loadPartnerLinkInstance(long id) {
    return (PartnerLinkInstance) session.load(PartnerLinkInstance.class, new Long(id));
  }

  public ReceiveAction loadReceiveAction(long id) {
    return (ReceiveAction) session.load(ReceiveAction.class, new Long(id));
  }

  public ReplyAction loadReplyAction(long id) {
    return (ReplyAction) session.load(ReplyAction.class, new Long(id));
  }

  public InvokeAction loadInvokeAction(long id) {
    return (InvokeAction) session.load(InvokeAction.class, new Long(id));
  }

  public Collection findReceiveTokens(BpelProcessDefinition processDefinition) {
    return session.getNamedQuery("IntegrationSession.findReceiveTokens").setEntity(
        "processDefinition", processDefinition).list();
  }

  public Collection findPickTokens(BpelProcessDefinition processDefinition) {
    List tokens = session.getNamedQuery("IntegrationSession.findPickTokens").setEntity(
        "processDefinition", processDefinition).list();
    // discard duplicates caused by eager collection fetching
    return new HashSet(tokens);
  }

  public Collection findEventTokens(BpelProcessDefinition processDefinition) {
    List tokens = session.getNamedQuery("IntegrationSession.findEventTokens").setEntity(
        "processDefinition", processDefinition).list();
    // discard duplicates caused by eager collection fetching
    return new HashSet(tokens);
  }

  public Collection findCatalogEntries() {
    return session.createCriteria(CatalogEntry.class).list();
  }

  public void saveCatalogEntry(CatalogEntry catalogEntry) {
    String baseLocation = catalogEntry.getBaseLocation();
    if (baseLocation != null && !catalogEntry.hasDescriptionBody()) {
      // look for existing entries having the same base location and no description body
      List catalogEntries = session.createCriteria(CatalogEntry.class)
          .add(Restrictions.eq("baseLocation", baseLocation))
          .add(Restrictions.isNull("descriptionBody"))
          .list();
      // avoid duplication
      if (!catalogEntries.isEmpty())
        return;
    }
    session.save(catalogEntry);
  }

  public static IntegrationSession getContextInstance(JbpmContext jbpmContext) {
    return new IntegrationSession(jbpmContext.getSession());
  }
}
