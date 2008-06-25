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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import org.jbpm.JbpmContext;
import org.jbpm.bpel.BpelException;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.scope.Scope;
import org.jbpm.bpel.graph.struct.RepetitiveActivity;
import org.jbpm.graph.exe.Token;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2008/06/12 08:18:53 $
 */
public class BpelGraphSession {

  private final Session session;

  public BpelGraphSession(Session session) {
    this.session = session;
  }

  public BpelProcessDefinition loadProcessDefinition(long id) {
    return (BpelProcessDefinition) session.load(BpelProcessDefinition.class, new Long(id));
  }

  public Activity loadActivity(long id) {
    return (Activity) session.load(Activity.class, new Long(id));
  }

  public Scope loadScope(long id) {
    return (Scope) session.load(Scope.class, new Long(id));
  }

  public RepetitiveActivity loadRepetitiveActivity(long id) {
    return (RepetitiveActivity) session.load(RepetitiveActivity.class, new Long(id));
  }

  public BpelProcessDefinition findProcessDefinition(String name, String targetNamespace,
      int version) {
    Criteria criteria = session.createCriteria(BpelProcessDefinition.class).add(
        Restrictions.eq("name", name)).add(Restrictions.eq("version", new Integer(version)));

    if (targetNamespace != null)
      criteria.add(Restrictions.eq("targetNamespace", targetNamespace));

    return (BpelProcessDefinition) criteria.uniqueResult();
  }

  public BpelProcessDefinition findLatestProcessDefinition(String name, String targetNamespace) {
    Criteria criteria = session.createCriteria(BpelProcessDefinition.class).add(
        Restrictions.eq("name", name)).addOrder(Order.desc("version")).setMaxResults(1);

    if (targetNamespace != null)
      criteria.add(Restrictions.eq("targetNamespace", targetNamespace));

    return (BpelProcessDefinition) criteria.uniqueResult();
  }

  public List findLatestProcessDefinitions() {
    List processDefinitionTuples = session.getNamedQuery(
        "BpelGraphSession.findLatestProcessDefinitions").list();
    ArrayList processDefinitions = new ArrayList();
    for (Iterator i = processDefinitionTuples.iterator(); i.hasNext();) {
      Object[] processDefinitionTuple = (Object[]) i.next();
      String name = (String) processDefinitionTuple[0];
      String targetNamespace = (String) processDefinitionTuple[1];
      Integer version = (Integer) processDefinitionTuple[2];
      BpelProcessDefinition processDefinition = findProcessDefinition(name, targetNamespace,
          version.intValue());
      processDefinitions.add(processDefinition);
    }
    return processDefinitions;
  }

  public void deployProcessDefinition(BpelProcessDefinition processDefinition) {
    String processName = processDefinition.getName();
    if (processName == null)
      throw new BpelException("process definition has no name");

    BpelProcessDefinition latestProcessDefinition = findLatestProcessDefinition(processName,
        processDefinition.getTargetNamespace());
    // find the current latest process definition
    if (latestProcessDefinition != null) {
      // take the next version number
      processDefinition.setVersion(latestProcessDefinition.getVersion() + 1);
    }
    else {
      // start from 1
      processDefinition.setVersion(1);
    }

    session.save(processDefinition);
  }

  public void lockToken(Token token) {
    session.lock(token, LockMode.UPGRADE);
  }

  public static BpelGraphSession getContextInstance(JbpmContext jbpmContext) {
    Session session = jbpmContext.getSession();
    return session != null ? new BpelGraphSession(session) : null;
  }
}
