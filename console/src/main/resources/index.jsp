<%@page language="java"%>
<html>
<head>
<title>jBPM BPEL Console</title>
<link rel="stylesheet" type="text/css" href="css/screen.css" />
<link rel="stylesheet" type="text/css" href="css/common.css" />
</head>
<body>
<%@include file="header.jspf"%>
<p>Welcome to the JBoss jBPM BPEL Console.</p>
<p>Use the links on the left to reach the management pages.</p>
<table cellspacing="5" class="deftable">
  <tr>
    <td class="term">Database Connection</td>
    <td class="def">
    <ul>
      <li>View database connectivity information</li>
      <li>Perform administrative operations</li>
    </ul>
    </td>
  </tr>
  <tr>
    <td class="term">Process Definitions</td>
    <td class="def">
    <ul>
      <li>Examine process definitions in production</li>
      <li>Deploy new process definitions</li>
    </ul>
    </td>
  </tr>
  <tr>
    <td class="term">Partner Services</td>
    <td class="def">
    <ul>
      <li>Browse services available for invocation</li>
      <li>Register new partner services</li>
    </ul>
    </td>
  </tr>
</table>
<%@include file="trailer.jspf"%>
</body>
</html>
