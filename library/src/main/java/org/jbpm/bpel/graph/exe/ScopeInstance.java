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

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;

import org.jbpm.JbpmContext;
import org.jbpm.bpel.alarm.AlarmAction;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.exe.state.ActiveState;
import org.jbpm.bpel.graph.scope.Handler;
import org.jbpm.bpel.graph.scope.OnAlarm;
import org.jbpm.bpel.graph.scope.OnEvent;
import org.jbpm.bpel.graph.scope.Scope;
import org.jbpm.bpel.integration.IntegrationService;
import org.jbpm.bpel.integration.def.CorrelationSetDefinition;
import org.jbpm.bpel.integration.def.PartnerLinkDefinition;
import org.jbpm.bpel.integration.def.ReceiveAction;
import org.jbpm.bpel.persistence.db.BpelGraphSession;
import org.jbpm.bpel.persistence.db.ScopeSession;
import org.jbpm.bpel.variable.def.VariableDefinition;
import org.jbpm.bpel.variable.def.VariableType;
import org.jbpm.bpel.variable.exe.MessageValue;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;
import org.jbpm.scheduler.SchedulerService;

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2008/06/11 19:58:04 $
 */
public class ScopeInstance implements Serializable, Compensator {

  long id;
  private Scope definition;
  private ScopeState state;
  private Token token;
  private FaultInstance faultInstance;
  private Compensator compensator;

  private static final String PRIMARY_TOKEN = "primary";
  private static final String HANDLER_TOKEN = "handler";
  private static final String EVENTS_TOKEN = "events";
  private static final String EVENT_TOKEN_PREFIX = "event-";

  private static final Log log = LogFactory.getLog(ScopeInstance.class);

  private static final long serialVersionUID = 1L;

  ScopeInstance() {
  }

  protected ScopeInstance(Scope definition, Token token) {
    this.definition = definition;
    this.token = token;
    // initial state is performing primary activity
    state = ActiveState.PRIMARY;
    // create a token for the primary activity
    Activity activity = definition.getActivity();
    new Token(token, activity != null ? activity.getName() : PRIMARY_TOKEN);
  }

  public void initializeData() {
    // TODO variable initialization and bpws:scopeInitializationFault handling
    // variables
    for (Iterator i = definition.getVariables().values().iterator(); i.hasNext();)
      ((VariableDefinition) i.next()).createInstance(token);

    // correlation sets
    for (Iterator i = definition.getCorrelationSets().values().iterator(); i.hasNext();)
      ((CorrelationSetDefinition) i.next()).createInstance(token);

    // partner links
    for (Iterator i = definition.getPartnerLinks().values().iterator(); i.hasNext();)
      ((PartnerLinkDefinition) i.next()).createInstance(token);
  }

  // signals
  // ///////////////////////////////////////////////////////////////////////////

  public void faulted(FaultInstance faultInstance) {
    this.faultInstance = faultInstance;
    state.faulted(this);
  }

  public void terminate() {
    state.terminate(this);
  }

  public void compensate(Compensator compensator) {
    this.compensator = compensator;
    state.compensate(this);
  }

  public void completed() {
    state.completed(this);
  }

  public void scopeCompensated(ScopeInstance nestedInstance) {
    ScopeSession scopeSession = ScopeSession.getContextInstance(JbpmContext.getCurrentJbpmContext());
    ScopeInstance nextNestedInstance = scopeSession.nextChildToCompensate(this);
    if (nextNestedInstance != null)
      nextNestedInstance.compensate(this);
    else
      state.childrenCompensated(this);
  }

  public void scopeTerminated(ScopeInstance nestedInstance) {
    if (!hasTerminableChildren())
      state.childrenTerminated(this);
  }

  private boolean hasTerminableChildren() {
    for (Iterator i = new ScopeInstanceIterator(token); i.hasNext();) {
      ScopeInstance scopeInstance = (ScopeInstance) i.next();
      if (scopeInstance.getState().isTerminable())
        return true;
    }
    return false;
  }

  // behavior methods
  // ///////////////////////////////////////////////////////////////////////////

  /**
   * Selects a handler for the internal fault. The handler is selected as follows.
   * <ul>
   * <li>if the fault has no data, select a handler with a matching faultName and no faultVariable</li>
   * <li>if the fault has data, select a handler with a matching faultName and a matching
   * faultVariable; if there is no such handler then select a handler with a matching faultVariable
   * and no faultName</li>
   * <li>otherwise, select the catchAll handler if it exists</li>
   * </ul>
   * @return the selected fault handler, or <code>null</code> if no handler is able to catch the
   * fault
   */
  public Handler getFaultHandler() {
    if (faultInstance == null)
      throw new IllegalStateException("scope has not faulted");

    // determine the type of fault data
    VariableType dataType;
    // is it a message?
    MessageValue messageValue = faultInstance.getMessageValue();
    if (messageValue != null)
      dataType = messageValue.getType();
    else {
      // is it an element?
      Element elementValue = faultInstance.getElementValue();
      if (elementValue != null) {
        QName elementName = new QName(elementValue.getNamespaceURI(), elementValue.getLocalName());
        dataType = definition.getBpelProcessDefinition().getImportDefinition().getElementType(
            elementName);
      }
      // it is none of the above
      else
        dataType = null;
    }
    return definition.selectFaultHandler(faultInstance.getName(), dataType);
  }

  public void enableEvents() {
    List onEvents = definition.getOnEvents();
    List onAlarms = definition.getOnAlarms();

    // easy way out: no events to enable
    if (onEvents.isEmpty() && onAlarms.isEmpty())
      return;

    // eventToken is the context for event handlers
    Token eventToken = new Token(token, EVENTS_TOKEN);
    eventToken.setNode(definition);

    JbpmContext jbpmContext = JbpmContext.getCurrentJbpmContext();

    // enable message events
    IntegrationService integrationService = ReceiveAction.getIntegrationService(jbpmContext);
    for (int i = 0, n = onEvents.size(); i < n; i++) {
      OnEvent onEvent = (OnEvent) onEvents.get(i);
      integrationService.receive(onEvent.getReceiveAction(), eventToken, false);
    }

    // enable alarm events
    SchedulerService schedulerService = AlarmAction.getSchedulerService(jbpmContext);
    for (int i = 0, n = onAlarms.size(); i < n; i++) {
      OnAlarm onAlarm = (OnAlarm) onAlarms.get(i);
      onAlarm.getAlarmAction().createTimer(eventToken, schedulerService);
    }
  }

  public void disableEvents() {
    Token eventsToken = getEventsToken();
    // easy way out: no events to disable
    if (eventsToken == null)
      return;

    JbpmContext jbpmContext = JbpmContext.getCurrentJbpmContext();

    // disable message events
    List onEvents = definition.getOnEvents();
    IntegrationService integrationService = ReceiveAction.getIntegrationService(jbpmContext);
    for (int i = 0, n = onEvents.size(); i < n; i++) {
      OnEvent onEvent = (OnEvent) onEvents.get(i);
      integrationService.cancelReception(onEvent.getReceiveAction(), eventsToken);
    }

    // disable alarm events
    List onAlarms = definition.getOnAlarms();
    SchedulerService schedulerService = AlarmAction.getSchedulerService(jbpmContext);
    for (int i = 0, n = onAlarms.size(); i < n; i++) {
      OnAlarm onAlarm = (OnAlarm) onAlarms.get(i);
      onAlarm.getAlarmAction().deleteTimer(eventsToken, schedulerService);
    }
  }

  public void proceed() {
    Token parentToken = token.getParent();
    if (parentToken != null) {
      // end scope token (do not cause parent termination)
      token.end(false);
      // leave the scope
      new ExecutionContext(parentToken).leaveNode();
    }
    else {
      // end global scope token (this will end the process instance)
      token.end();
    }
  }

  public void terminateChildren() {
    Token handlerToken = getHandlerToken();
    if (handlerToken != null && !handlerToken.hasEnded()) {
      // terminate handler execution
      terminateToken(handlerToken);
    }
    else {
      Token primaryToken = getPrimaryToken();
      if (!primaryToken.hasEnded()) {
        // terminate primary activity execution
        terminateToken(primaryToken);
      }
      // check whether any event token needs termination
      Token eventsToken = getEventsToken();
      if (eventsToken != null && !eventsToken.hasEnded()) {
        // stop listening for events
        disableEvents();
        // terminate active event tokens
        Map eventTokens = eventsToken.getChildren();
        if (eventTokens != null) {
          for (Iterator i = eventTokens.values().iterator(); i.hasNext();) {
            Token eventToken = (Token) i.next();
            if (!eventToken.hasEnded())
              terminateToken(eventToken);
          }
        }
        // end events token (do not cause parent termination)
        eventsToken.end(false);
      }
    }
    // notify children termination if no nested scope instances are being terminated
    if (!hasTerminableChildren())
      state.childrenTerminated(this);
  }

  private static void terminateToken(Token token) {
    Activity activity;
    // check whether node has the proper type already
    Node node = token.getNode();
    if (node instanceof Activity)
      activity = (Activity) node;
    else {
      // acquire proxy of the proper type
      JbpmContext jbpmContext = JbpmContext.getCurrentJbpmContext();
      BpelGraphSession graphSession = BpelGraphSession.getContextInstance(jbpmContext);
      activity = graphSession.loadActivity(node.getId());
    }

    // terminate activity
    activity.terminate(new ExecutionContext(token));

    // end token (do not verify parent termination)
    token.end(false);
  }

  public boolean hasPendingEvents() {
    Token eventsToken = getEventsToken();
    if (eventsToken == null)
      return false;

    for (Iterator i = new ScopeInstanceIterator(eventsToken); i.hasNext();) {
      ScopeInstance scopeInstance = (ScopeInstance) i.next();
      if (!scopeInstance.getState().isEnd())
        return true;
    }
    return false;
  }

  public Token getPrimaryToken() {
    Activity activity = definition.getActivity();
    return token.getChild(activity != null ? activity.getName() : PRIMARY_TOKEN);
  }

  public Token createEventToken() {
    Token eventsToken = getEventsToken();
    Map eventTokens = eventsToken.getChildren();
    return new Token(eventsToken, EVENT_TOKEN_PREFIX
        + (eventTokens != null ? eventTokens.size() : 0));
  }

  public Token createHandlerToken() {
    if (token.hasChild(HANDLER_TOKEN))
      throw new IllegalStateException("handler token already exists");

    return new Token(token, HANDLER_TOKEN);
  }

  public Token getHandlerToken() {
    return token.getChild(HANDLER_TOKEN);
  }

  private Token getEventsToken() {
    return token.getChild(EVENTS_TOKEN);
  }

  public Map getEventTokens() {
    return getEventsToken().getChildren();
  }

  public Token getEventToken(int index) {
    return getEventsToken().getChild(EVENT_TOKEN_PREFIX + index);
  }

  public ScopeState getState() {
    return state;
  }

  public void setState(ScopeState state) {
    log.debug("state change to " + state.getName() + " on " + definition + " for " + token);
    this.state = state;
  }

  public FaultInstance getFaultInstance() {
    return faultInstance;
  }

  public void setFaultInstance(FaultInstance faultInstance) {
    this.faultInstance = faultInstance;
  }

  public Compensator getCompensator() {
    return compensator;
  }

  public void setCompensator(Compensator compensator) {
    this.compensator = compensator;
  }

  public Token getToken() {
    return token;
  }

  public void setToken(Token token) {
    this.token = token;
  }

  public Scope getDefinition() {
    return definition;
  }

  // operations helper methods
  // ///////////////////////////////////////////////////////////////////////////

  public ScopeInstance getParent() {
    Token parentToken = token.getParent();
    return parentToken != null ? Scope.getInstance(parentToken) : null;
  }

  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this).append("name", definition.getName())
        .append("token", token.getFullName())
        .append("state", state.getName());

    if (faultInstance != null)
      builder.append("fault", faultInstance);

    if (compensator != null)
      builder.append("compensator", compensator);

    return builder.toString();
  }

  public static ScopeInstance createScopeInstance(Scope definition, Token token) {
    return new ScopeInstance(definition, token);
  }

  public static ScopeInstance createEventInstance(Scope scope, Token token) {
    return new EventInstance(scope, token);
  }
}