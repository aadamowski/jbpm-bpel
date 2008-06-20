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
package org.jbpm.bpel.graph.scope;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.iterators.FilterIterator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;

import org.jbpm.JbpmContext;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.def.BpelVisitor;
import org.jbpm.bpel.graph.def.CompositeActivity;
import org.jbpm.bpel.graph.exe.BpelFaultException;
import org.jbpm.bpel.graph.exe.ScopeInstance;
import org.jbpm.bpel.integration.def.CorrelationSetDefinition;
import org.jbpm.bpel.integration.def.PartnerLinkDefinition;
import org.jbpm.bpel.persistence.db.BpelGraphSession;
import org.jbpm.bpel.variable.def.MessageType;
import org.jbpm.bpel.variable.def.VariableDefinition;
import org.jbpm.bpel.variable.def.VariableType;
import org.jbpm.bpel.wsdl.xml.WsdlUtil;
import org.jbpm.graph.def.Action;
import org.jbpm.graph.def.ExceptionHandler;
import org.jbpm.graph.def.GraphElement;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;
import org.jbpm.instantiation.Delegation;

/**
 * Provides a context which influences the execution behavior of enclosed activities. This
 * behavioral context includes {@linkplain VariableDefinition variables},
 * {@linkplain PartnerLinkDefinition partner links}, message exchanges,
 * {@linkplain CorrelationSetDefinition correlation sets}, {@linkplain Handler event handlers},
 * {@linkplain Catch fault handlers}, a {@linkplain Handler compensation handler} and a
 * {@linkplain Handler termination handler}.
 * @author Juan Cantú
 * @version $Revision$ $Date: 2008/02/04 14:35:49 $
 */
public class Scope extends CompositeActivity {

  private Activity activity;

  private Map variables = new HashMap();
  private Map partnerLinks = new HashMap();
  private Map correlationSets = new HashMap();

  private Handler compensationHandler;
  private Handler terminationHandler;
  private Handler catchAll;
  private List faultHandlers = new ArrayList();

  private List onEvents = new ArrayList();
  private List onAlarms = new ArrayList();

  private boolean isolated;
  private boolean implicit;

  public static final String VARIABLE_NAME = "s:instance";

  private static final Log log = LogFactory.getLog(Scope.class);
  private static final boolean traceEnabled = log.isTraceEnabled();

  private static final long serialVersionUID = 1L;

  public Scope() {
  }

  public Scope(String name) {
    super(name);
  }

  public void execute(ExecutionContext exeContext) {
    // instantiate scope
    Token scopeToken = new Token(exeContext.getToken(), name);
    ScopeInstance scopeInstance = createInstance(scopeToken);

    // initialize data and events
    scopeInstance.initializeData();
    scopeInstance.enableEvents();

    // execute primary token on activity
    Token primaryToken = scopeInstance.getPrimaryToken();
    activity.enter(new ExecutionContext(primaryToken));
  }

  public void terminate(ExecutionContext context) {
    Token scopeToken = context.getToken().getChild(name);
    ScopeInstance scopeInstance = Scope.getInstance(scopeToken);
    scopeInstance.terminate();
  }

  public void eliminatePath(Token token) {
    activity.eliminatePath(token);
    super.eliminatePath(token);
  }

  public void accept(BpelVisitor visitor) {
    visitor.visit(this);
  }

  // scope properties
  // //////////////////////////////////////////////////////////////////////////

  public void installFaultExceptionHandler() {
    ExceptionHandler exceptionHandler = new ExceptionHandler();
    exceptionHandler.setExceptionClassName(BpelFaultException.class.getName());
    exceptionHandler.addAction(new Action(new Delegation(FaultActionHandler.class.getName())));
    addExceptionHandler(exceptionHandler);
  }

  public Activity getActivity() {
    return activity;
  }

  public void setActivity(Activity activity) {
    if (this.activity != null)
      unsetActivity();

    if (activity != null) {
      activity.detachFromParent();
      adoptActivity(activity);

      this.activity = activity;
    }
  }

  private void unsetActivity() {
    disadoptActivity(activity);
    activity = null;
  }

  /**
   * Gets the variable with the given name. Only checks variables defined in this scope.
   * @param variableName the variable name
   * @return the variable with the given name, or <code>null</code> if no such variable exists
   */
  public VariableDefinition getVariable(String variableName) {
    return (VariableDefinition) variables.get(variableName);
  }

  /**
   * Gets the variables defined in scope.
   * @return map&lt;name, variable&gt;
   */
  public Map getVariables() {
    return variables;
  }

  public void addVariable(VariableDefinition variable) {
    variables.put(variable.getName(), variable);
  }

  public void setVariables(Map variables) {
    this.variables = variables;
  }

  /**
   * Gets the correlation set with the given name. Only checks correlation sets defined in this
   * scope.
   * @param correlationSetName the correlation set name
   * @return the correlation set with the given name, or <code>null</code> if no such correlation
   * set exists
   */
  public CorrelationSetDefinition getCorrelationSet(String correlationSetName) {
    return (CorrelationSetDefinition) correlationSets.get(correlationSetName);
  }

  public Map getCorrelationSets() {
    return correlationSets;
  }

  public void addCorrelationSet(CorrelationSetDefinition correlation) {
    correlationSets.put(correlation.getName(), correlation);
  }

  public void setCorrelationSets(Map correlationSets) {
    this.correlationSets = correlationSets;
  }

  public PartnerLinkDefinition getPartnerLink(String plinkName) {
    return (PartnerLinkDefinition) partnerLinks.get(plinkName);
  }

  public Map getPartnerLinks() {
    return partnerLinks;
  }

  public void addPartnerLink(PartnerLinkDefinition partnerLink) {
    partnerLinks.put(partnerLink.getName(), partnerLink);
  }

  public void setPartnerLinks(Map partnerLinks) {
    this.partnerLinks = partnerLinks;
  }

  public Handler getCompensationHandler() {
    return compensationHandler;
  }

  public void setCompensationHandler(Handler handler) {
    if (compensationHandler != null)
      disadoptActivity(compensationHandler);

    adoptActivity(handler);
    compensationHandler = handler;
  }

  public Handler getTerminationHandler() {
    return terminationHandler;
  }

  public void setTerminationHandler(Handler handler) {
    if (terminationHandler != null)
      disadoptActivity(terminationHandler);

    adoptActivity(handler);
    terminationHandler = handler;
  }

  public Handler getCatchAll() {
    return catchAll;
  }

  public void setCatchAll(Handler handler) {
    if (catchAll != null)
      disadoptActivity(catchAll);

    adoptActivity(handler);
    catchAll = handler;
  }

  public List getFaultHandlers() {
    return faultHandlers;
  }

  public void addCatch(Catch catcher) {
    adoptActivity(catcher);
    faultHandlers.add(catcher);
  }

  public Handler selectFaultHandler(QName name, VariableType dataType) {
    Catch selectedCatch;
    // if the fault has no data,
    if (dataType == null) {
      // select a handler with a matching faultName and no faultVariable
      selectedCatch = selectCatch(name);
    }
    // if the fault has data and name,
    else if (name != null) {
      // select a handler with a matching faultName and a matching faultVariable
      selectedCatch = selectCatch(name, dataType);
      // if there is no such handler,
      if (selectedCatch == null) {
        // select a handler with a matching faultVariable and no faultName
        selectedCatch = selectCatch(dataType);
      }
    }
    // if the fault has no name,
    else {
      // select a handler with a matching faultVariable and no faultName
      selectedCatch = selectCatch(dataType);
    }
    // otherwise, select the catchAll handler if it exists
    return selectedCatch != null ? selectedCatch : catchAll;
  }

  /**
   * Finds a handler for a fault thrown without associated data among the handlers with the given
   * name.
   * @param name the fault name
   * @return a fault handler with the given name and no variable; <code>null</code> if no such
   * handler was found
   */
  public Catch selectCatch(QName name) {
    if (traceEnabled)
      log.trace("looking for fault handler with name '" + name + "' and no variable");

    Iterator namedCatchIt = new FilterIterator(faultHandlers.iterator(), new NamedCatchPredicate(
        name));
    while (namedCatchIt.hasNext()) {
      Catch namedCatch = (Catch) namedCatchIt.next();

      if (namedCatch.getFaultVariable() == null) {
        if (traceEnabled)
          log.trace("selected catch with matching name: " + namedCatch);
        return namedCatch;
      }
    }
    return null;
  }

  /**
   * Finds a handler for a fault thrown with associated data among the handlers with the given name.
   * @param name the fault name
   * @param dataType the type of fault data
   * @return a fault handler with the given name for the specified data type; <code>null</code> if
   * no such handler was found
   */
  public Catch selectCatch(QName name, VariableType dataType) {
    if (traceEnabled) {
      log.trace("looking for fault handler with name '"
          + name
          + "' and variable type '"
          + dataType
          + '\'');
    }
    Iterator namedCatchIt = new FilterIterator(faultHandlers.iterator(), new NamedCatchPredicate(
        name));
    return selectCatch(namedCatchIt, dataType);
  }

  /**
   * Finds a handler for a fault thrown with associated data among the unnamed handlers.
   * @param dataType the type of fault data
   * @return an unnamed fault handler for the specified data type; <code>null</code> if no such
   * handler was found
   */
  public Catch selectCatch(VariableType dataType) {
    if (traceEnabled)
      log.trace("looking for fault handler with no name and variable type '" + dataType);

    Iterator unnamedCatchIt = new FilterIterator(faultHandlers.iterator(), UNNAMED_CATCH_PREDICATE);
    return selectCatch(unnamedCatchIt, dataType);
  }

  /**
   * Finds a handler for a fault thrown with associated data among the given handlers. For each
   * handler <i>h</i>:
   * <ul>
   * <li>if <i>h</i> has a faultVariable whose type matches the type of the fault data then <i>h</i>
   * is selected</li>
   * <li>otherwise if the type of fault data is a WSDL message which contains a single part defined
   * by an element, and <i>h</i> has a faultVariable whose type matches the type of the element
   * used to define the part then <i>h</i> is selected</li>
   * <li>otherwise if <i>h</i> does not specify a faultVariable then <i>h</i> is selected</li>
   * </ul>
   * @param dataType the type of fault data
   * @return the selected fault handler, or <code>null</code> if no handler is able to catch the
   * fault
   */
  private static Catch selectCatch(Iterator handlerIt, VariableType dataType) {
    Catch selectedCatch = null;
    while (handlerIt.hasNext()) {
      Catch currentCatch = (Catch) handlerIt.next();
      if (traceEnabled)
        log.trace("examining catch: " + currentCatch);

      // look for a fault variable
      VariableDefinition variable = currentCatch.getFaultVariable();
      if (variable != null) {
        VariableType handlerType = variable.getType();
        if (handlerType.equals(dataType)) {
          // current handler matches exactly the fault type; it has the highest
          // priority, select it and stop the search
          selectedCatch = currentCatch;
          if (traceEnabled)
            log.trace("selected catch with matching type: " + selectedCatch);
          break;
        }
        else if (dataType.isMessage() && handlerType.isElement()) {
          // fault data is a WSDL message, and the handler has an element
          // variable
          MessageType messageType;
          if (dataType instanceof MessageType)
            messageType = (MessageType) dataType;
          else {
            // reacquire proxy of the proper type
            Session hbSession = JbpmContext.getCurrentJbpmContext().getSession();
            messageType = (MessageType) hbSession.load(MessageType.class,
                hbSession.getIdentifier(dataType));
          }

          QName elementName = WsdlUtil.getDocLitElementName(messageType.getMessage());
          // do the handler element and the message part element match?
          if (handlerType.getName().equals(elementName)) {
            // current handler matches the element, select it but keep looking
            // for a exact type match
            selectedCatch = currentCatch;
            if (traceEnabled)
              log.trace("selected catch with matching element: " + selectedCatch);
          }
        }
      }
      else if (selectedCatch == null) {
        // this handler does not define a variable, select it only if no other
        // handler (of higher priority) has been selected
        selectedCatch = currentCatch;
        if (traceEnabled)
          log.trace("selected catch with no type:" + selectedCatch);
      }
    }
    return selectedCatch;
  }

  public List getOnEvents() {
    return onEvents;
  }

  public void addOnEvent(OnEvent onEvent) {
    adoptActivity(onEvent);
    onEvents.add(onEvent);
  }

  public List getOnAlarms() {
    return onAlarms;
  }

  public void addOnAlarm(OnAlarm onAlarm) {
    adoptActivity(onAlarm);
    onAlarms.add(onAlarm);
  }

  public boolean isIsolated() {
    return isolated;
  }

  public void setIsolated(boolean isolated) {
    this.isolated = isolated;
  }

  public boolean isImplicit() {
    return implicit;
  }

  public void setImplicit(boolean implicit) {
    this.implicit = implicit;
  }

  // composite activity override
  // //////////////////////////////////////////////////////////////////////////

  public boolean isScope() {
    return true;
  }

  public VariableDefinition findVariable(String name) {
    VariableDefinition variable = getVariable(name);
    return variable != null ? variable : super.findVariable(name);
  }

  public CorrelationSetDefinition findCorrelationSet(String name) {
    CorrelationSetDefinition correlationSet = getCorrelationSet(name);
    return correlationSet != null ? correlationSet : super.findCorrelationSet(name);
  }

  public PartnerLinkDefinition findPartnerLink(String name) {
    PartnerLinkDefinition partnerLink = getPartnerLink(name);
    return partnerLink != null ? partnerLink : super.findPartnerLink(name);
  }

  public Node addNode(Node node) {
    if (!(node instanceof Activity))
      throw new IllegalArgumentException("not an activity: " + node);

    setActivity((Activity) node);
    return node;
  }

  public Node removeNode(Node node) {
    if (node == null)
      throw new IllegalArgumentException("node is null");

    if (!node.equals(activity))
      return null;

    unsetActivity();
    return node;
  }

  public void reorderNode(int oldIndex, int newIndex) {
    if (activity == null || oldIndex != 0 || newIndex != 0) {
      throw new IndexOutOfBoundsException("could not reorder element: oldIndex="
          + oldIndex
          + ", newIndex="
          + newIndex);
    }
  }

  public List getNodes() {
    return activity != null ? Collections.singletonList(activity) : null;
  }

  public Node getNode(String name) {
    return hasNode(name) ? activity : null;
  }

  public Map getNodesMap() {
    return activity != null ? Collections.singletonMap(activity.getName(), activity) : null;
  }

  public boolean hasNode(String name) {
    return activity != null && activity.getName().equals(name);
  }

  protected boolean isChildInitial(Activity child) {
    /*
     * this method is only invoked from child.isInitial() on its composite activity; therefore, it
     * is valid to assume the argument is the primary activity of this scope
     */
    return true;
  }

  // activity override
  // //////////////////////////////////////////////////////////////////////////

  public GraphElement getParent() {
    return isGlobal() ? processDefinition : super.getParent();
  }

  public ProcessDefinition getProcessDefinition() {
    return isGlobal() ? processDefinition : super.getProcessDefinition();
  }

  public BpelProcessDefinition getBpelProcessDefinition() {
    if (!isGlobal())
      return super.getBpelProcessDefinition();

    // check whether process definition reference has the proper type already
    if (processDefinition instanceof BpelProcessDefinition)
      return (BpelProcessDefinition) processDefinition;

    // reacquire proxy of proper type
    JbpmContext jbpmContext = JbpmContext.getCurrentJbpmContext();
    BpelGraphSession graphSession = BpelGraphSession.getContextInstance(jbpmContext);
    BpelProcessDefinition bpelProcessDefinition = graphSession.loadProcessDefinition(processDefinition.getId());

    // update process definition reference
    processDefinition = bpelProcessDefinition;

    return bpelProcessDefinition;
  }

  protected boolean suppressJoinFailure() {
    return isGlobal() ? getSuppressJoinFailure().booleanValue() : super.suppressJoinFailure();
  }

  public boolean isGlobal() {
    return getCompositeActivity() == null;
  }

  public Collection findNestedScopes() {
    /*
     * start from the enclosed activity so that this scope does not appear in the resulting
     * collection
     */
    NestedScopeFinder finder = new NestedScopeFinder();
    activity.accept(finder);
    return finder.getScopes();
  }

  public Scope findNestedScope(String name) {
    for (Iterator i = findNestedScopes().iterator(); i.hasNext();) {
      Scope nestedScope = (Scope) i.next();
      if (nestedScope.getName().equals(name))
        return nestedScope;
    }
    return null;
  }

  public ScopeInstance createInstance(Token token) {
    ScopeInstance instance = ScopeInstance.createScopeInstance(this, token);
    token.getProcessInstance().getContextInstance().createVariable(VARIABLE_NAME, instance, token);
    return instance;
  }

  public ScopeInstance createEventInstance(Token token) {
    ScopeInstance instance = ScopeInstance.createEventInstance(this, token);
    token.getProcessInstance().getContextInstance().createVariable(VARIABLE_NAME, instance, token);
    return instance;
  }

  public static ScopeInstance getInstance(Token token) {
    return (ScopeInstance) token.getProcessInstance().getContextInstance().getVariable(
        VARIABLE_NAME, token);
  }

  private static class NamedCatchPredicate implements Predicate {

    private final QName faultName;

    NamedCatchPredicate(QName faultName) {
      this.faultName = faultName;
    }

    public boolean evaluate(Object arg) {
      Catch catcher = (Catch) arg;
      return faultName.equals(catcher.getFaultName());
    }
  }

  private static final Predicate UNNAMED_CATCH_PREDICATE = new Predicate() {

    public boolean evaluate(Object arg) {
      Catch catcher = (Catch) arg;
      return catcher.getFaultName() == null;
    }
  };
}