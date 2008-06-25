<%@page language="java"%>
<%@page import="org.jbpm.JbpmConfiguration"%>
<%@page import="org.jbpm.JbpmContext"%>
<%@page import="java.sql.Connection"%>
<%@page import="java.sql.DatabaseMetaData"%>
<%@page import="java.sql.ResultSet"%>
<%@page import="java.sql.SQLException"%>
<%@page import="org.jbpm.bpel.web.AdministrationServlet"%>
<html>
<head>
<title>jBPM BPEL Console</title>
<link rel="stylesheet" type="text/css" href="css/screen.css" />
<link rel="stylesheet" type="text/css" href="css/common.css" />
</head>
<body>
<%@include file="header.jspf"%>
<h3>Database Connection</h3>
<%
      String configResource = application.getInitParameter("JbpmCfgResource");
      JbpmConfiguration jbpmConfiguration = JbpmConfiguration.getInstance(configResource);
      JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
      try {
        DatabaseMetaData metadata = jbpmContext.getConnection().getMetaData();
%>
<table class="deftable">
  <tr>
    <td class="term">Product name</td>
    <td class="def"><%=metadata.getDatabaseProductName()%></td>
  </tr>
  <tr>
    <td class="term">Product version</td>
    <td class="def"><%=metadata.getDatabaseProductVersion()%></td>
  </tr>
  <tr>
    <td class="term">Driver name</td>
    <td class="def"><%=metadata.getDriverName()%></td>
  </tr>
  <tr>
    <td class="term">Driver version</td>
    <td class="def"><%=metadata.getDriverVersion()%></td>
  </tr>
  <tr>
    <td class="term">jBPM tables</td>
    <td class="def">
    <%
            ResultSet tableSet = metadata.getTables(null, null, "JBPM_%", null);
            boolean tablesPresent = tableSet.next();
            tableSet.close();

            if (tablesPresent) {
              out.write("Present");
              request.setAttribute("tablesPresent", Boolean.TRUE);
            }
            else
              out.write("Absent");
    %>
    </td>
  </tr>
</table>
<%
      }
      catch (SQLException e) {
        log("could not retrieve database metadata", e);
%>
<p><em>Database access failure</em></p>
<%
      }
      finally {
        jbpmContext.close();
      }
%>
<h3>Administration</h3>
<form action="administration" method='post'><input type="hidden" name="operation"
  value="<%= AdministrationServlet.OP_CREATE_SCHEMA %>" /><input type="submit"
  value="Create schema" /></form>
<%
if (request.getAttribute("tablesPresent") == Boolean.TRUE) {
%>
<form action="administration" method='post'><input type="hidden" name="operation"
  value="<%= AdministrationServlet.OP_DROP_SCHEMA %>" /><input type="submit" value="Drop schema" /></form>
<%
}
%>
<%@include file="trailer.jspf"%>
</body>
</html>
