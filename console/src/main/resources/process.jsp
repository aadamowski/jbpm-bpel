<%@page language="java"%>
<%@page import="java.util.Iterator"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Map"%>
<%@page import="javax.jms.JMSException"%>
<%@page import="javax.jms.Queue"%>
<%@page import="javax.jms.QueueReceiver"%>
<%@page import="org.jbpm.JbpmContext"%>
<%@page import="org.jbpm.JbpmConfiguration"%>
<%@page import="org.jbpm.graph.exe.Token"%>
<%@page import="org.jbpm.bpel.graph.def.Activity"%>
<%@page import="org.jbpm.bpel.graph.def.BpelProcessDefinition"%>
<%@page import="org.jbpm.bpel.integration.def.ReceiveAction"%>
<%@page import="org.jbpm.bpel.integration.exe.PartnerLinkInstance"%>
<%@page import="org.jbpm.bpel.integration.jms.IntegrationControl"%>
<%@page import="org.jbpm.bpel.integration.jms.JmsIntegrationServiceFactory"%>
<%@page import="org.jbpm.bpel.integration.jms.OutstandingRequest"%>
<%@page import="org.jbpm.bpel.integration.jms.RequestListener"%>
<%@page import="org.jbpm.bpel.integration.jms.StartListener"%>
<%@page import="org.jbpm.bpel.persistence.db.BpelGraphSession"%>
<%@page import="org.jbpm.bpel.persistence.db.IntegrationSession"%>
<%@page import="org.jbpm.bpel.integration.client.Caller"%>
<%@page import="org.jbpm.bpel.integration.def.InvokeAction"%>
<html>
<head>
<title>jBPM BPEL Console</title>
<link rel="stylesheet" type="text/css" href="css/screen.css" />
<link rel="stylesheet" type="text/css" href="css/common.css" />
</head>
<body>
<%@include file="header.jspf"%>
<%
  String name = request.getParameter("name");
  if (name == null)
    throw new ServletException("parameter 'name' not found");

  String targetNamespace = request.getParameter("targetNamespace");
  if (targetNamespace == null)
    throw new ServletException("parameter 'targetNamespace' not found");

  String version = request.getParameter("version");

  String configurationResource = application.getInitParameter("JbpmCfgResource");
  JbpmConfiguration jbpmConfiguration = JbpmConfiguration.getInstance(configurationResource);
  JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
  try {
    BpelGraphSession graphSession = BpelGraphSession.getContextInstance(jbpmContext);

    BpelProcessDefinition processDefinition;
    if (version == null)
      processDefinition = graphSession.findLatestProcessDefinition(name, targetNamespace);
    else {
      try {
        processDefinition = graphSession.findProcessDefinition(name, targetNamespace,
            Integer.parseInt(version));
      }
      catch (NumberFormatException e) {
        throw new ServletException("parameter 'version' is not a valid integer", e);
      }
    }
%>
<h3>Process Details</h3>
<table class="deftable">
  <tr>
    <td class="term">Name</td>
    <td class="def"><%=name%></td>
  </tr>
  <tr>
    <td class="term">Target namespace</td>
    <td class="def"><%=targetNamespace%></td>
  </tr>
  <%
    if (version != null) {
  %>
  <tr>
    <td class="term">Version</td>
    <td class="def"><%=version%></td>
  </tr>
  <%
    }
  %>
</table>
<h3>Start activities</h3>
<table class="infotable">
  <tr>
    <th class="header">Activity</th>
    <th class="header">Partner link</th>
    <th class="header">Operation</th>
    <th class="header">Queue</th>
  </tr>
  <%
    JmsIntegrationServiceFactory integrationServiceFactory = JmsIntegrationServiceFactory.getConfigurationInstance(jbpmConfiguration);
      IntegrationControl integrationControl = integrationServiceFactory.getIntegrationControl(processDefinition);
      IntegrationSession integrationSession = IntegrationSession.getContextInstance(jbpmContext);

      List startListeners = integrationControl.getStartListeners();
      synchronized (startListeners) {
        for (Iterator i = startListeners.iterator(); i.hasNext();) {
          StartListener startListener = (StartListener) i.next();

          ReceiveAction receiveAction = integrationSession.loadReceiveAction(startListener.getReceiveActionId());
          Activity activity = (Activity) receiveAction.getInboundMessageActivity();
  %>
  <tr>
    <td class="desc"><%=activity.getName()%></td>
    <td class="desc"><%=receiveAction.getPartnerLink().getName()%></td>
    <td class="desc"><%=receiveAction.getOperation().getName()%></td>
    <%
      QueueReceiver messageConsumer = (QueueReceiver) startListener.getMessageConsumer();
            try {
              String queueName = messageConsumer.getQueue().getQueueName();
    %>
    <td class="desc"><%=queueName%></td>
    <%
      }
            catch (JMSException e) {
              log("could not get queue", e);
    %>
    <td class="desc"><em>JMS failure</em></td>
    <%
      }
    %>
  </tr>
  <%
    }
      }
  %>
</table>
<h3>Inbound message activities</h3>
<%
  Map requestListeners = integrationControl.getRequestListeners();
    if (requestListeners.isEmpty()) {
%>
<p><em>No inbound message activities</em></p>
<%
  }
    else {
%>
<table class="infotable">
  <tr>
    <th class="header">Activity</th>
    <th class="header">Partner link</th>
    <th class="header">Operation</th>
    <th class="header">Message exchange</th>
    <th class="header">Token</th>
    <th class="header">Queue</th>
    <th class="header">Selector</th>
  </tr>
  <%
    synchronized (requestListeners) {
          for (Iterator i = requestListeners.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();

            RequestListener.Key key = (RequestListener.Key) entry.getKey();
            ReceiveAction receiveAction = integrationSession.loadReceiveAction(key.getReceiveActionId());
            Token token = jbpmContext.loadToken(key.getTokenId());
  %>
  <tr>
    <td class="desc"><%=token.getNode().getName()%></td>
    <td class="desc"><%=receiveAction.getPartnerLink().getName()%></td>
    <td class="desc"><%=receiveAction.getOperation().getName()%></td>
    <td class="desc"><%=receiveAction.getMessageExchange()%></td>
    <td class="desc"><%=token.getFullName()%></td>
    <%
      RequestListener requestListener = (RequestListener) entry.getValue();
              QueueReceiver messageConsumer = (QueueReceiver) requestListener.getMessageConsumer();
              try {
                String queueName = messageConsumer.getQueue().getQueueName();
                String selector = messageConsumer.getMessageSelector();
    %>
    <td class="desc"><%=queueName%></td>
    <td class="desc"><%=selector%></td>
    <%
      }
              catch (JMSException e) {
                log("could not get queue and selector", e);
    %>
    <td class="desc" colspan="2"><em>JMS failure</em></td>
    <%
      }
    %>
  </tr>
  <%
    }
        }
  %>
</table>
<%
  }
%>
<h3>Outstanding requests</h3>
<%
  Map outstandingRequests = integrationControl.getOutstandingRequests();
    if (outstandingRequests.isEmpty()) {
%>
<p><em>No outstanding requests</em></p>
<%
  }
    else {
%>
<table class="infotable">
  <tr>
    <th class="header">Partner link</th>
    <th class="header">Operation</th>
    <th class="header">Message exchange</th>
    <th class="header">Queue</th>
    <th class="header">Correlation ID</th>
  </tr>
  <%
    synchronized (outstandingRequests) {
          for (Iterator i = outstandingRequests.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();

            OutstandingRequest.Key key = (OutstandingRequest.Key) entry.getKey();
            PartnerLinkInstance partnerLinkInstance = integrationSession.loadPartnerLinkInstance(key.getPartnerLinkId());
  %>
  <tr>
    <td class="desc"><%=partnerLinkInstance.getDefinition().getName()%></td>
    <td class="desc"><%=key.getOperationName()%></td>
    <td class="desc"><%=key.getMessageExchange()%></td>
    <%
      OutstandingRequest outstandingRequest = (OutstandingRequest) entry.getValue();
              Queue queue = (Queue) outstandingRequest.getReplyDestination();
              try {
                String queueName = queue.getQueueName();
    %>
    <td class="desc"><%=queueName%></td>
    <td class="desc"><%=outstandingRequest.getCorrelationID()%> <%
   }
           catch (JMSException e) {
             log("could not get queue", e);
 %>
    </td>
    <td class="desc" colspan='2'><em>JMS failure</em></td>
    <%
      }
    %>
  </tr>
  <%
    }
        }
  %>
</table>
<%
  }
%>
<h3>Ongoing invocations</h3>
<%
  Map callers = integrationControl.getCallers();
    if (callers.isEmpty()) {
%>
<p><em>No ongoing invocations</em></p>
<%
  }
    else {
%>
<table class="infotable">
  <tr>
    <th class="header">Activity</th>
    <th class="header">Partner link</th>
    <th class="header">Operation</th>
    <th class="header">Token</th>
  </tr>
  <%
    synchronized (callers) {
          for (Iterator i = callers.keySet().iterator(); i.hasNext();) {
            Caller.Key key = (Caller.Key) i.next();
            InvokeAction invokeAction = integrationSession.loadInvokeAction(key.getInvokeActionId());
            Token token = jbpmContext.loadToken(key.getTokenId());
  %>
  <tr>
    <td class="desc"><%=token.getNode().getName()%></td>
    <td class="desc"><%=invokeAction.getPartnerLink().getName()%></td>
    <td class="desc"><%=invokeAction.getOperation().getName()%></td>
    <td class="desc"><%=token.getFullName()%></td>
  </tr>
  <%
    }
        }
  %>
</table>
<%
  }
  }
  finally {
    jbpmContext.close();
  }
%>
<%@include file="trailer.jspf"%>
</body>
</html>