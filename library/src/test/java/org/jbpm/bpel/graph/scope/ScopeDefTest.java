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

import javax.wsdl.Message;
import javax.wsdl.Part;
import javax.xml.namespace.QName;

import junit.framework.TestCase;

import com.ibm.wsdl.MessageImpl;
import com.ibm.wsdl.PartImpl;

import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.def.ImportDefinition;
import org.jbpm.bpel.graph.exe.BpelFaultException;
import org.jbpm.bpel.variable.def.ElementType;
import org.jbpm.bpel.variable.def.MessageType;
import org.jbpm.bpel.variable.def.VariableDefinition;
import org.jbpm.bpel.xml.BpelConstants;

/**
 * @author Juan Cantu
 * @version $Revision$ $Date: 2007/10/13 02:53:25 $
 */
public class ScopeDefTest extends TestCase {

  Scope scope;
  BpelFaultException fault;
  Catch catchName = new Catch();
  Catch catchMessage = new Catch();
  Catch catchNameMessage = new Catch();
  Catch catchElement = new Catch();
  Catch catchNameElement = new Catch();
  Handler catchAll = new Handler();

  static final QName NON_MATCHING_NAME = new QName(BpelConstants.NS_EXAMPLES, "unexpected");

  protected void setUp() {
    BpelProcessDefinition processDefinition = new BpelProcessDefinition("pd",
        BpelConstants.NS_EXAMPLES);
    ImportDefinition importDefinition = processDefinition.getImportDefinition();

    scope = new Scope();
    scope.installFaultExceptionHandler();
    processDefinition.getGlobalScope().setActivity(scope);

    QName faultName = new QName(BpelConstants.NS_EXAMPLES, "aFault");

    catchName.setFaultName(faultName);
    scope.addCatch(catchName);

    QName elementName = new QName(BpelConstants.NS_EXAMPLES, "anElement");

    VariableDefinition elementVariable = new VariableDefinition();
    elementVariable.setName("anElementVariable");
    elementVariable.setType(importDefinition.getElementType(elementName));

    catchElement.setFaultVariable(elementVariable);
    scope.addCatch(catchElement);

    /*
     * handlers with faultName have priority over handlers without faultName in the case of a fault
     * with a matching faultName; add handler with faultName later to verify this
     */
    catchNameElement.setFaultName(faultName);
    catchNameElement.setFaultVariable(elementVariable);
    scope.addCatch(catchNameElement);

    /*
     * handlers with a faultVariable of message type have priority over handlers with a
     * faultVariable of type element in the case of a fault with a matching message that contains a
     * single part defined by a matching element; add handlers with a message type later to verify
     * this
     */
    Part part = new PartImpl();
    part.setName("aPart");
    part.setElementName(elementName);

    QName messageName = new QName(BpelConstants.NS_EXAMPLES, "aMessage");
    Message message = new MessageImpl();
    message.setQName(messageName);
    message.addPart(part);
    importDefinition.addMessage(message);

    VariableDefinition messageVariable = new VariableDefinition();
    messageVariable.setName("aMessageVariable");
    messageVariable.setType(importDefinition.getMessageType(messageName));

    catchMessage.setFaultVariable(messageVariable);
    scope.addCatch(catchMessage);

    /*
     * handlers with faultName have priority over handlers without faultName in the case of a fault
     * with a matching faultName; add handler with faultName later to verify this
     */
    catchNameMessage.setFaultName(faultName);
    catchNameMessage.setFaultVariable(messageVariable);
    scope.addCatch(catchNameMessage);
  }

  /* In the case of faults thrown with no associated data: */

  /*
   * If there is a catch activity with a matching faultName value that does not specify a
   * faultVariable attribute then the fault is passed to the identified catch activity
   */
  public void test_faultNoData_catchName() {
    assertSame(catchName, scope.selectFaultHandler(catchName.getFaultName(), null));
  }

  /*
   * Otherwise if there is a catchAll handler then the fault is passed to the catchAll handler
   */
  public void test_faultNoData_catchAll() {
    scope.setCatchAll(catchAll);
    assertSame(catchAll, scope.selectFaultHandler(NON_MATCHING_NAME, null));
  }

  /* Otherwise the fault is thrown to the immediately enclosing scope */
  public void test_faultNoData_noCatch() {
    assertNull(scope.selectFaultHandler(NON_MATCHING_NAME, null));
  }

  /* In the case of faults thrown with associated data: */

  /*
   * If there is a catch activity with a matching faultName value that has a faultVariable or
   * faultMessageType whose type matches the type of the fault data then the fault is passed to the
   * identified catch activity
   */
  public void test_faultMessage_catchNameMessage() {
    assertSame(catchNameMessage, scope.selectFaultHandler(catchNameMessage.getFaultName(),
        catchNameMessage.getFaultVariable().getType()));
  }

  public void test_faultElement_catchNameElement() {
    assertSame(catchNameElement, scope.selectFaultHandler(catchNameElement.getFaultName(),
        catchNameElement.getFaultVariable().getType()));
  }

  /*
   * Otherwise if the fault data is a WSDL message type where the message contains a single part
   * defined by an element and there exists a catch activity with a matching faultName value that
   * has a faultVariable whose type matches the type of the element used to define the part then the
   * fault is passed to the identified catch activity
   */
  public void test_faultMessage_catchNameElement() {
    // part
    Part part = new PartImpl();
    part.setName("aPart");
    part.setElementName(catchNameElement.getFaultVariable().getType().getName());
    // message
    Message message = new MessageImpl();
    message.setQName(NON_MATCHING_NAME);
    message.addPart(part);

    assertSame(catchNameElement, scope.selectFaultHandler(catchNameMessage.getFaultName(),
        new MessageType(message)));
  }

  /*
   * If there is a catch activity with a matching faultName value that does not specify a
   * faultVariable or faultMessageType value then the fault is passed to the identified catch
   * activity
   */
  public void test_faultMessage_catchName() {
    assertSame(catchName, scope.selectFaultHandler(catchName.getFaultName(), new ElementType(
        NON_MATCHING_NAME)));
  }

  /*
   * Otherwise if there is a catch activity without a faultName attribute that has a faultVariable
   * or faultMessageType whose type matches the type of the fault data then the fault is passed to
   * the identified catch activity
   */
  public void test_faultMessage_catchMessage() {
    assertSame(catchMessage, scope.selectFaultHandler(NON_MATCHING_NAME,
        catchMessage.getFaultVariable().getType()));
  }

  public void test_faultElement_catchElement() {
    assertSame(catchElement, scope.selectFaultHandler(NON_MATCHING_NAME,
        catchElement.getFaultVariable().getType()));
  }

  /*
   * Otherwise if the fault data is a WSDL message type where the message contains a single part
   * defined by an element and there exists a catch activity without a faultName attribute that has
   * a faultVariable whose type matches the type of the element used to define the part then the
   * fault is passed to the identified catch activity
   */
  public void test_faultMessage_catchElement() {
    // element part
    Part part = new PartImpl();
    part.setName("aPart");
    part.setElementName(catchElement.getFaultVariable().getType().getName());
    // message
    Message message = new MessageImpl();
    message.setQName(NON_MATCHING_NAME);
    message.addPart(part);
    // type
    ImportDefinition importDefinition = scope.getBpelProcessDefinition().getImportDefinition();
    importDefinition.addMessage(message);
    MessageType type = importDefinition.getMessageType(NON_MATCHING_NAME);

    assertSame(catchElement, scope.selectFaultHandler(NON_MATCHING_NAME, type));
  }

  /*
   * Otherwise if there is a catchAll handler then the fault is passed to the catchAll handler
   */
  public void test_faultMessage_catchAll() {
    scope.setCatchAll(catchAll);
    // message
    Message message = new MessageImpl();
    message.setQName(NON_MATCHING_NAME);
    // type
    MessageType type = new MessageType(message);

    assertSame(catchAll, scope.selectFaultHandler(NON_MATCHING_NAME, type));
  }

  public void test_faultElement_catchAll() {
    scope.setCatchAll(catchAll);
    // type
    ElementType type = new ElementType(NON_MATCHING_NAME);

    assertSame(catchAll, scope.selectFaultHandler(NON_MATCHING_NAME, type));
  }

  /* Otherwise, the fault will be handled by the default fault handler */
  public void test_faultMessage_noCatch() {
    // message
    Message message = new MessageImpl();
    message.setQName(NON_MATCHING_NAME);
    // type
    MessageType type = new MessageType(message);

    assertNull(scope.selectFaultHandler(NON_MATCHING_NAME, type));
  }

  public void test_faultElement_noCatch() {
    ElementType type = new ElementType(NON_MATCHING_NAME);

    assertNull(scope.selectFaultHandler(NON_MATCHING_NAME, type));
  }
}