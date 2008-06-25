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

import java.util.Iterator;

import javax.wsdl.Operation;
import javax.wsdl.OperationType;
import javax.wsdl.PortType;

import org.w3c.dom.Element;

import org.jbpm.bpel.graph.basic.Invoke;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.CompositeActivity;
import org.jbpm.bpel.graph.scope.Scope;
import org.jbpm.bpel.integration.def.Correlation;
import org.jbpm.bpel.integration.def.CorrelationSetDefinition;
import org.jbpm.bpel.integration.def.Correlations;
import org.jbpm.bpel.integration.def.InvokeAction;
import org.jbpm.bpel.integration.def.PartnerLinkDefinition;
import org.jbpm.bpel.integration.def.Correlation.Pattern;
import org.jbpm.bpel.variable.def.VariableDefinition;
import org.jbpm.bpel.wsdl.PartnerLinkType.Role;
import org.jbpm.bpel.xml.util.XmlUtil;

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2007/07/26 00:36:13 $
 */
public class InvokeReader extends ActivityReader {

  public Activity read(Element activityElem, CompositeActivity parent) {
    Element faultHandlersElem = XmlUtil.getElement(activityElem, BpelConstants.NS_BPEL,
        BpelConstants.ELEM_FAULT_HANDLERS);
    Element compensationHandlerElem = XmlUtil.getElement(activityElem, BpelConstants.NS_BPEL,
        BpelConstants.ELEM_COMPENSATION_HANDLER);

    if (compensationHandlerElem == null && faultHandlersElem == null) {
      Invoke invoke = new Invoke();
      readStandardProperties(activityElem, invoke, parent);
      readInvoke(activityElem, invoke);
      return invoke;
    }

    /*
     * WS-BPEL section 11.3: Semantically, the specification of local fault and/or compensation
     * handlers is equivalent to the presence of an implicit scope immediately enclosing the
     * activity and providing those handlers.
     */
    Scope scope = new Scope();
    scope.installFaultExceptionHandler();
    scope.setImplicit(true);

    readStandardProperties(activityElem, scope, parent);

    if (compensationHandlerElem != null)
      bpelReader.readCompensationHandler(compensationHandlerElem, scope);
    if (faultHandlersElem != null)
      bpelReader.readFaultHandlers(faultHandlersElem, scope);

    Invoke invoke = new Invoke();
    scope.setActivity(invoke);
    readInvoke(activityElem, invoke);

    return scope;
  }

  public void readInvoke(Element invokeElem, Invoke invoke) {
    validateNonInitial(invokeElem, invoke);
    invoke.setAction(readInvokeAction(invokeElem, invoke.getCompositeActivity()));
  }

  public InvokeAction readInvokeAction(Element invokeElem, CompositeActivity parent) {
    InvokeAction invokeAction = new InvokeAction();
    // partner link
    String partnerLinkName = invokeElem.getAttribute(BpelConstants.ATTR_PARTNER_LINK);
    PartnerLinkDefinition partnerLink = parent.findPartnerLink(partnerLinkName);
    if (partnerLink == null) {
      bpelReader.getProblemHandler().add(new ParseProblem("partner link not found", invokeElem));
      return invokeAction;
    }
    invokeAction.setPartnerLink(partnerLink);

    // port type
    Role partnerRole = partnerLink.getPartnerRole();
    // BPEL-181 detect absence of partner role
    if (partnerRole == null) {
      bpelReader.getProblemHandler().add(
          new ParseProblem("partner link does not indicate partner role", invokeElem));
      return invokeAction;
    }
    PortType portType = bpelReader.getMessageActivityPortType(invokeElem, partnerRole);

    // operation
    Operation operation = bpelReader.getMessageActivityOperation(invokeElem, portType);
    invokeAction.setOperation(operation);

    // input variable
    VariableDefinition input = bpelReader.getMessageActivityVariable(invokeElem,
        BpelConstants.ATTR_INPUT_VARIABLE, parent, operation.getInput().getMessage());
    invokeAction.setInputVariable(input);

    // output variable
    VariableDefinition output = null;
    if (operation.getStyle() == OperationType.REQUEST_RESPONSE) {
      output = bpelReader.getMessageActivityVariable(invokeElem,
          BpelConstants.ATTR_OUTPUT_VARIABLE, parent, operation.getOutput().getMessage());
      invokeAction.setOutputVariable(output);
    }

    // correlations
    Element correlationsElem = XmlUtil.getElement(invokeElem, BpelConstants.NS_BPEL,
        BpelConstants.ELEM_CORRELATIONS);
    if (correlationsElem != null) {
      Correlations inCorrelations = new Correlations();
      Correlations outCorrelations = new Correlations();
      Iterator correlationElemIt = XmlUtil.getElements(correlationsElem, BpelConstants.NS_BPEL,
          BpelConstants.ELEM_CORRELATION);

      while (correlationElemIt.hasNext()) {
        Element correlationElem = (Element) correlationElemIt.next();
        Correlation correlation = bpelReader.readCorrelation(correlationElem, parent);
        CorrelationSetDefinition set = correlation.getSet();

        // correlation pattern
        String patternAttr = correlationElem.getAttribute(BpelConstants.ATTR_PATTERN);
        Pattern pattern = Pattern.valueOf(patternAttr);
        if (pattern == null) {
          /*
           * XXX try the old constants for compatibility with the xml schema used by the eclipse
           * bpel designer as of 2007/07/24
           */
          if ("out".equals(patternAttr))
            pattern = Pattern.REQUEST;
          else if ("in".equals(patternAttr))
            pattern = Pattern.RESPONSE;
          else if ("out-in".equals(patternAttr))
            pattern = Pattern.REQUEST_RESPONSE;
          else {
            bpelReader.getProblemHandler()
                .add(new ParseProblem("invalid pattern", correlationElem));
            continue;
          }
        }

        // is pattern 'request' or 'request-response'?
        if (pattern != Pattern.RESPONSE) {
          bpelReader.checkVariableProperties(input, set, correlationElem);
          outCorrelations.addCorrelation(correlation);
        }

        // is pattern 'response' or 'request-response'?
        if (pattern != Pattern.REQUEST) {
          if (output != null) {
            bpelReader.checkVariableProperties(output, set, correlationElem);
          }
          else {
            bpelReader.getProblemHandler().add(
                new ParseProblem(
                    "correlation cannot apply to inbound message in one-way operation",
                    correlationElem));
          }
          inCorrelations.addCorrelation(correlation);
        }
      }

      if (inCorrelations.getCorrelations() != null)
        invokeAction.setRequestCorrelations(inCorrelations);

      if (outCorrelations.getCorrelations() != null)
        invokeAction.setResponseCorrelations(outCorrelations);
    }
    return invokeAction;
  }
}
