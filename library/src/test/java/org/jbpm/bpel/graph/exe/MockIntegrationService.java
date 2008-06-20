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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ClassUtils;

import org.jbpm.bpel.endpointref.EndpointReference;
import org.jbpm.bpel.integration.IntegrationService;
import org.jbpm.bpel.integration.def.InvokeAction;
import org.jbpm.bpel.integration.def.PartnerLinkDefinition;
import org.jbpm.bpel.integration.def.ReceiveAction;
import org.jbpm.bpel.integration.def.ReplyAction;
import org.jbpm.graph.exe.Token;
import org.jbpm.svc.Service;
import org.jbpm.svc.ServiceFactory;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2008/02/01 05:43:07 $
 */
public class MockIntegrationService implements IntegrationService {

  private Map myReferences = new HashMap();

  private static final long serialVersionUID = 1L;

  private static String getIdString(Object objectWithId) {
    Class classWithId = objectWithId.getClass();
    // get the class name excluding the package
    String className = ClassUtils.getShortClassName(classWithId);
    // obtain the identifier getter method according to jBPM conventions
    String idString = "0";
    try {
      Method idGetter = classWithId.getMethod("getId", null);
      idGetter.setAccessible(true);
      Long idWrapper = (Long) idGetter.invoke(objectWithId, null);
      long id = idWrapper.longValue();
      if (id != 0L) {
        idString = Long.toHexString(id);
      }
      else {
        // object is transient, fall back to hash code
        idString = Integer.toHexString(objectWithId.hashCode());
      }
    }
    catch (NoSuchMethodException e) {
      // no id getter, fall back to hash code
      idString = Integer.toHexString(objectWithId.hashCode());
    }
    catch (IllegalAccessException e) {
      // we made the getter accessible, should not happen
      e.printStackTrace();
    }
    catch (InvocationTargetException e) {
      // strange a getter throws a checked exception
      e.printStackTrace();
    }
    return className + idString;
  }

  public static void createMark(Object marker, Token token) {
    token.getProcessInstance().getContextInstance().createVariable(getIdString(marker), "xxx",
        token);
  }

  public static void deleteMark(Object marker, Token token) {
    token.getProcessInstance().getContextInstance().deleteVariable(getIdString(marker), token);
  }

  public static boolean hasMark(Object marker, Token token) {
    return token.getProcessInstance().getContextInstance().hasVariable(getIdString(marker), token);
  }

  public void receive(ReceiveAction receiveAction, Token token, boolean oneShot) {
    createMark(receiveAction, token);
  }

  public void receive(List receivers, Token token) {
    Iterator receiverIt = receivers.iterator();
    while (receiverIt.hasNext()) {
      ReceiveAction receiveAction = (ReceiveAction) receiverIt.next();
      createMark(receiveAction, token);
    }
  }

  public void cancelReception(ReceiveAction receiveAction, Token token) {
    deleteMark(receiveAction, token);
  }

  public void endReception(List receivers, Token token) {
    Iterator receiverIt = receivers.iterator();
    while (receiverIt.hasNext()) {
      ReceiveAction receiveAction = (ReceiveAction) receiverIt.next();
      deleteMark(receiveAction, token);
    }
  }

  public void reply(ReplyAction replyAction, Token token) {
    throw new UnsupportedOperationException();
  }

  public void invoke(InvokeAction invokeAction, Token token) {
    throw new UnsupportedOperationException();
  }

  public void cancelInvocation(InvokeAction invokeAction, Token token) {
    throw new UnsupportedOperationException();
  }

  public EndpointReference getMyReference(PartnerLinkDefinition partnerLink, Token token) {
    return (EndpointReference) myReferences.get(getIdString(partnerLink));
  }

  public void setMyReference(PartnerLinkDefinition partnerLink, EndpointReference myReference) {
    myReferences.put(getIdString(partnerLink), myReference);
  }

  public void enableStartActivities() {
    throw new UnsupportedOperationException();
  }

  public void disableStartActivities() {
    throw new UnsupportedOperationException();
  }

  public void close() {
    // noop
  }

  public static class Factory implements ServiceFactory {

    private static final long serialVersionUID = 1L;

    public Service openService() {
      return new MockIntegrationService();
    }

    public void close() {
      // noop
    }
  }
}
