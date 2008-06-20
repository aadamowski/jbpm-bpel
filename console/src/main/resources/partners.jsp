<%@page language="java"%>
<%@page import="org.jbpm.JbpmConfiguration"%>
<%@page import="org.jbpm.JbpmContext"%>
<%@page import="org.jbpm.bpel.persistence.db.IntegrationSession"%>
<%@page import="java.util.Collection"%>
<%@page import="java.util.Iterator"%>
<%@page import="org.jbpm.bpel.integration.catalog.CatalogEntry"%>
<%@page import="javax.wsdl.Definition"%>
<%@page import="javax.wsdl.xml.WSDLReader"%>
<%@page import="org.jbpm.bpel.wsdl.xml.WsdlUtil"%>
<%@page import="javax.xml.namespace.QName"%>
<%@page import="javax.wsdl.WSDLException"%>
<%@page import="org.jbpm.bpel.web.RegistrationServlet"%>
<html>
<head>
<title>jBPM BPEL Console</title>
<link rel="stylesheet" type="text/css" href="css/screen.css" />
<link rel="stylesheet" type="text/css" href="css/common.css" />
</head>
<body>
<%@include file="header.jspf"%>
<h3>Partner Service Catalog</h3>
<%
      String configResource = application.getInitParameter("JbpmCfgResource");
      JbpmConfiguration jbpmConfiguration = JbpmConfiguration.getInstance(configResource);
      JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
      try {
        IntegrationSession integrationSession = IntegrationSession.getContextInstance(jbpmContext);
        Collection catalogEntries = integrationSession.findCatalogEntries();
        if (!catalogEntries.isEmpty()) {
%>
<table class="infotable">
  <tr>
    <th class="header">Base location</th>
    <th class="header">Target namespace</th>
    <th class="header">Services</th>
  </tr>
  <%
            WSDLReader reader = WsdlUtil.getFactory().newWSDLReader();
            for (Iterator i = catalogEntries.iterator(); i.hasNext();) {
              CatalogEntry catalogEntry = (CatalogEntry) i.next();
  %>
  <tr>
    <td class="desc">
    <%
                String documentUri = catalogEntry.getBaseLocation();
                out.print(documentUri != null ? documentUri : "<em>None</em>");
    %>
    </td>
    <%
                  try {
                  Definition definition = catalogEntry.readDefinition(reader);
    %>
    <td class="desc"><%=definition.getTargetNamespace()%></td>
    <td class="desc">
    <%
                    for (Iterator s = definition.getServices().keySet().iterator(); s.hasNext();) {
                    QName serviceName = (QName) s.next();
                    out.print(serviceName.getLocalPart());
                    out.print(' ');
                  }
    %>
    </td>
    <%
                }
                catch (WSDLException e) {
                  log("could not read wsdl document", e);
    %>
    <td class="desc" colspan="2"><em>WSDL read failure</em></td>
    <%
    }
    %>
  </tr>
  <%
  }
  %>
</table>
<%
        }
        else {
%>
<p><em>The catalog is empty</em></p>
<%
      }
      }
      finally {
        jbpmContext.close();
      }
%>
<h3>Registration</h3>
<p>There are two ways to register a partner service description:</p>
<ol>
  <li>Enter an absolute URL pointing to a WSDL document as the base location. Leave the
  description file blank.</li>
  <li>Browse the file system for a WSDL document. Provide a base location to resolve any
  relative URLs within the document.</li>
</ol>
<form action="registration" method="post" enctype="multipart/form-data">
<table cellspacing="5">
  <tr>
    <td>Base location</td>
    <td><input type="text" name="<%= RegistrationServlet.PARAM_BASE_LOCATION %>" size="30" /></td>
  </tr>
  <tr>
    <td>Description file</td>
    <td><input type="file" name="<%= RegistrationServlet.PARAM_DESCRIPTION_FILE %>" size="30"
      accept="text/xml" /></td>
  </tr>
  <tr>
    <td colspan="2"><input type="submit" value="Register" /></td>
  </tr>
</table>
</form>
<%@include file="trailer.jspf"%>
</body>
</html>
