<%@page language="java"%>
<%@page import="java.util.Iterator"%>
<%@page import="java.util.List"%>
<%@page import="org.jbpm.JbpmConfiguration"%>
<%@page import="org.jbpm.JbpmContext"%>
<%@page import="org.jbpm.bpel.graph.def.BpelProcessDefinition"%>
<%@page import="org.jbpm.bpel.persistence.db.BpelGraphSession"%>
<%@page import="org.jbpm.bpel.web.DeploymentServlet"%>
<html>
<head>
<title>jBPM BPEL Console</title>
<link rel="stylesheet" type="text/css" href="css/screen.css" />
<link rel="stylesheet" type="text/css" href="css/common.css" />
</head>
<body>
<%@include file="header.jspf"%>
<h3>Process Definitions</h3>
<%
  String configurationResource = application.getInitParameter("JbpmCfgResource");
  JbpmConfiguration jbpmConfiguration = JbpmConfiguration.getInstance(configurationResource);
  JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
  try {
    BpelGraphSession bpelGraphSession = BpelGraphSession.getContextInstance(jbpmContext);
    List processDefinitions = bpelGraphSession.findLatestProcessDefinitions();
    if (!processDefinitions.isEmpty()) {
%>
<table class="infotable">
  <tr>
    <th class="header">Name</th>
    <th class="header">Target namespace</th>
    <th class="header">Version</th>
    <th class="header"/>
  </tr>
  <%
    for (Iterator i = processDefinitions.iterator(); i.hasNext();) {
          BpelProcessDefinition processDefinition = (BpelProcessDefinition) i.next();
          String name = processDefinition.getName();
          String targetNamespace = processDefinition.getTargetNamespace();
          int version = processDefinition.getVersion();
  %>
  <tr>
    <td class="desc"><%=name%></td>
    <td class="desc"><%=targetNamespace%></td>
    <td class="desc"><%=version%></td>
    <td class="desc"><a
      href="process.jsp?name=<%=name%>&targetNamespace=<%=targetNamespace%>&version=<%=version%>">
      <img src="images/glass.png" alt="View" height="16" width="16" border="0" /></a></td>
  </tr>
  <%
    }
  %>
</table>
<%
  }
    else {
%>
<p><em>No process definitions have been deployed</em></p>
<%
  }
  }
  finally {
    jbpmContext.close();
  }
%>
<h3>Deployment</h3>
<form action="deployment" method="post" enctype="multipart/form-data">
<p>Process archive <input type="file" name="<%= DeploymentServlet.PARAM_PROCESS_ARCHIVE %>"
  accept="application/zip" size="30" /></p>
<input type="submit" value="Deploy" /></form>
<%@include file="trailer.jspf"%>
</body>
</html>
