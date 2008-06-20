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
package org.jbpm.bpel.xml;

import javax.xml.namespace.QName;

/**
 * Constant values used in BPEL process definitions.
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/11/29 10:17:06 $
 */
public class BpelConstants {

  // namespaces
  public static final String NS_BPEL = "http://docs.oasis-open.org/wsbpel/2.0/process/executable";
  public static final String NS_SERVICE_REF = "http://docs.oasis-open.org/wsbpel/2.0/serviceref";
  public static final String NS_BPEL_1_1 = "http://schemas.xmlsoap.org/ws/2003/03/business-process/";
  public static final String NS_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";
  public static final String NS_XML_SCHEMA_INSTANCE = "http://www.w3.org/2001/XMLSchema-instance";
  public static final String NS_XMLNS = "http://www.w3.org/2000/xmlns/";
  public static final String NS_VENDOR = "urn:jbpm.org:bpel-1.1";
  public static final String NS_DEFINITION_DESCRIPTOR = "urn:jbpm.org:bpel-1.1:definition";
  public static final String NS_DEPLOYMENT_DESCRIPTOR = "urn:jbpm.org:bpel-1.1:deployment";
  public static final String NS_EXAMPLES = "http://jbpm.org/bpel/examples";

  public static final String URN_XPATH_1_0 = "urn:oasis:names:tc:wsbpel:2.0:sublang:xpath1.0";

  public static final String YES = "yes";
  public static final String NO = "no";

  // process
  public static final String ATTR_NAME = "name";
  public static final String ATTR_TARGET_NAMESPACE = "targetNamespace";
  public static final String ELEM_IMPORT = "import";
  public static final String ATTR_EXPRESSION_LANGUAGE = "expressionLanguage";
  public static final String ATTR_QUERY_LANGUAGE = "queryLanguage";
  public static final String ATTR_ABSTRACT_PROCESS = "abstractProcess";

  // import
  public static final String ATTR_NAMESPACE = "namespace";
  public static final String ATTR_LOCATION = "location";
  public static final String ATTR_IMPORT_TYPE = "importType";

  // activity
  public static final String ATTR_SUPPRESS_JOIN_FAILURE = "suppressJoinFailure";
  public static final String ATTR_LINK_NAME = "linkName";
  public static final String ELEM_SOURCES = "sources";
  public static final String ELEM_SOURCE = "source";
  public static final String ELEM_TRANSITION_CONDITION = "transitionCondition";
  public static final String ELEM_TARGETS = "targets";
  public static final String ELEM_JOIN_CONDITION = "joinCondition";
  public static final String ELEM_TARGET = "target";

  // scope
  public static final String ELEM_SCOPE = "scope";
  public static final String ATTR_ISOLATED = "isolated";
  public static final String ELEM_COMPENSATION_HANDLER = "compensationHandler";
  public static final String ELEM_TERMINATION_HANDLER = "terminationHandler";
  public static final String ELEM_FAULT_HANDLERS = "faultHandlers";
  public static final String ELEM_CATCH_ALL = "catchAll";
  public static final String ELEM_EVENT_HANDLERS = "eventHandlers";
  public static final String ELEM_ON_EVENT = "onEvent";
  public static final String ELEM_CATCH = "catch";
  public static final String ATTR_FAULT_NAME = "faultName";
  public static final String ATTR_FAULT_VARIABLE = "faultVariable";
  public static final String ATTR_FAULT_MESSAGE_TYPE = "faultMessageType";
  public static final String ATTR_FAULT_ELEMENT = "faultElement";
  public static final String ELEM_VARIABLES = "variables";
  public static final String ELEM_VARIABLE = "variable";
  public static final String ATTR_TYPE = "type";
  public static final String ATTR_MESSAGE_TYPE = "messageType";
  public static final String ATTR_ELEMENT = "element";
  public static final String ELEM_PARTNER_LINKS = "partnerLinks";
  public static final String ELEM_PARTNER_LINK = "partnerLink";
  public static final String ATTR_PARTNER_LINK_TYPE = "partnerLinkType";
  public static final String ATTR_MY_ROLE = "myRole";
  public static final String ATTR_PARTNER_ROLE = "partnerRole";
  public static final String ELEM_CORRELATION_SETS = "correlationSets";
  public static final String ELEM_CORRELATION_SET = "correlationSet";
  public static final String ATTR_PROPERTIES = "properties";

  // empty
  public static final String ELEM_EMPTY = "empty";

  // flow
  public static final String ELEM_FLOW = "flow";
  public static final String ELEM_LINKS = "links";
  public static final String ELEM_LINK = "link";

  // while
  public static final String ELEM_WHILE = "while";
  public static final String ELEM_CONDITION = "condition";

  // switch
  public static final String ELEM_IF = "if";
  public static final String ELEM_ELSEIF = "elseif";
  public static final String ELEM_ELSE = "else";

  // service activities
  public static final String ATTR_PARTNER_LINK = "partnerLink";
  public static final String ATTR_OPERATION = "operation";
  public static final String ATTR_PORT_TYPE = "portType";
  public static final String ELEM_CORRELATIONS = "correlations";
  public static final String ELEM_CORRELATION = "correlation";
  public static final String ATTR_SET = "set";
  public static final String ATTR_INITIATE = "initiate";
  public static final String ATTR_VARIABLE = "variable";

  // service reference
  public static final String ELEM_SERVICE_REF = "service-ref";
  public static final String ATTR_REFERENCE_SCHEME = "reference-scheme";

  // invoke
  public static final String ELEM_INVOKE = "invoke";
  public static final String ATTR_INPUT_VARIABLE = "inputVariable";
  public static final String ATTR_OUTPUT_VARIABLE = "outputVariable";
  public static final String ATTR_PATTERN = "pattern";

  // receive
  public static final String ELEM_RECEIVE = "receive";
  public static final String ATTR_CREATE_INSTANCE = "createInstance";
  public static final String ATTR_MESSAGE_EXCHANGE = "messageExchange";

  // reply
  public static final String ELEM_REPLY = "reply";

  // wait
  public static final String ELEM_WAIT = "wait";
  public static final String ELEM_FOR = "for";
  public static final String ELEM_UNTIL = "until";
  public static final String ELEM_REPEAT_EVERY = "repeatEvery";

  // pick
  public static final String ELEM_PICK = "pick";
  public static final String ELEM_ON_MESSAGE = "onMessage";
  public static final String ELEM_ON_ALARM = "onAlarm";

  // compensate
  public static final String ELEM_COMPENSATE = "compensate";

  // compensateScope
  public static final String ELEM_COMPENSATE_SCOPE = "compensateScope";
  public static final String ATTR_TARGET = "target";

  // assign
  public static final String ELEM_ASSIGN = "assign";
  public static final String ELEM_COPY = "copy";
  public static final String ELEM_FROM = "from";
  public static final String ELEM_TO = "to";
  public static final String ATTR_EXPRESSION = "expression";
  public static final String ATTR_PART = "part";
  public static final String ELEM_QUERY = "query";
  public static final String ATTR_PROPERTY = "property";
  public static final String ATTR_ENDPOINT_REFERENCE = "endpointReference";
  public static final String ELEM_LITERAL = "literal";

  // validate
  public static final String ELEM_VALIDATE = "validate";
  public static final String ATTR_VARIABLES = "variables";

  // other activities
  public static final String ELEM_SEQUENCE = "sequence";
  public static final String ELEM_THROW = "throw";
  public static final String ELEM_RETHROW = "rethrow";
  public static final String ELEM_EXIT = "exit";

  // standard faults
  public static final QName FAULT_SELECTION_FAILURE = new QName(NS_BPEL, "selectionFailure");
  public static final QName FAULT_CONFLICTING_REQUEST = new QName(NS_BPEL, "conflictingRequest");
  public static final QName FAULT_MISSING_REQUEST = new QName(NS_BPEL, "missingRequest");
  public static final QName FAULT_MISMATCHED_ASSIGNMENT = new QName(NS_BPEL,
      "mismatchedAssignmentFailure");
  public static final QName FAULT_JOIN_FAILURE = new QName(NS_BPEL, "joinFailure");
  public static final QName FAULT_CORRELATION_VIOLATION = new QName(NS_BPEL, "correlationViolation");
  public static final QName FAULT_UNINITIALIZED_VARIABLE = new QName(NS_BPEL,
      "uninitializedVariable");
  public static final QName FAULT_UNSUPPORTED_REFERENCE = new QName(NS_BPEL, "unsupportedReference");
  public static final QName FAULT_UNINITIALIZED_PARTNER_ROLE = new QName(NS_BPEL,
      "uninitializedPartnerRole");
  public static final QName FAULT_SUB_LANGUAGE_EXECUTION = new QName(BpelConstants.NS_BPEL,
      "subLanguageExecutionFault");
  public static final QName FAULT_FORCED_TERMINATION = new QName(NS_BPEL, "forcedTermination");
  public static final QName FAULT_INVALID_EXPRESSION_VALUE = new QName(BpelConstants.NS_BPEL,
      "invalidExpressionValue");

  // extension faults
  public static final QName FAULT_INVOCATION_FAILURE = new QName(NS_VENDOR, "invocationFailure");

  // definition descriptor
  public static final String ELEM_IMPORTS = "imports";
  public static final String ELEM_WSDL = "wsdl";
  public static final String ELEM_XML_SCHEMA = "schema";

  // deployment descriptor
  public static final String ELEM_BPEL_DEPLOYMENT = "bpelDeployment";
  public static final String ATTR_VERSION = "version";
  public static final String ELEM_SCOPES = "scopes";
  public static final String ELEM_MY_ROLE = "myRole";
  public static final String ATTR_SERVICE = "service";
  public static final String ATTR_PORT = "port";
  public static final String ELEM_PARTNER_ROLE = "partnerRole";
  public static final String ATTR_DESTINATION = "destination";
  public static final String ATTR_HANDLE = "handle";
  public static final String ELEM_SERVICE_CATALOGS = "serviceCatalogs";
  public static final String ATTR_CONTEXT_URL = "contextUrl";
  public static final String ELEM_DEFINITION = "definition";

  // xml variable initialization mark
  public static final String ATTR_INITIALIZED = "initialized";
  public static final String ATTR_NIL = "nil";

  // suppress default constructor, ensuring non-instantiability
  private BpelConstants() {
  }
}