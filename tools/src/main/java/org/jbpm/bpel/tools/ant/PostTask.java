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
package org.jbpm.bpel.tools.ant;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.LogOutputStream;

/**
 * Common base of tasks that post requests to remote services.
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2008/02/05 10:29:36 $
 */
public abstract class PostTask extends Task {

  private String serviceUri;

  public void execute() throws BuildException {
    PostMethod post = new PostMethod(serviceUri);
    try {
      // write request
      writeRequest(post);

      // execute method
      HttpClient client = new HttpClient();
      int statusCode = client.executeMethod(post);
      log("status code: " + statusCode, Project.MSG_DEBUG);

      // read response
      readResponse(post);
    }
    catch (IOException e) {
      logException(e);
      throw new BuildException("connection to remote service failed", e);
    }
    finally {
      post.releaseConnection();
    }
  }

  /**
   * Specifies the location of the remote service.
   */
  public void setServiceUri(String serviceUri) {
    this.serviceUri = serviceUri;
  }

  /**
   * Writes the request to be posted.
   * @throws IOException if an I/O error occurs
   */
  protected abstract void writeRequest(PostMethod post) throws IOException;

  /**
   * Reads the response of the given post.
   * @throws IOException if an I/O error occurs
   */
  protected void readResponse(PostMethod post) throws IOException {
    logResponse(post);

    int statusClass = post.getStatusCode() / 100;
    if (statusClass == 4 || statusClass == 5)
      log("operation failed: " + post.getStatusText(), Project.MSG_ERR);
  }

  /**
   * Logs the response of the given post.
   * @throws IOException if an I/O error occurs
   */
  protected void logResponse(PostMethod post) throws IOException {
    BufferedReader responseSource = new BufferedReader(new InputStreamReader(
        post.getResponseBodyAsStream(), post.getResponseCharSet()));
    try {
      for (String line; (line = responseSource.readLine()) != null;)
        log(line, Project.MSG_DEBUG);
    }
    finally {
      responseSource.close();
    }
  }

  /**
   * Logs the given exception.
   */
  protected void logException(Exception exception) {
    PrintWriter logSink = new PrintWriter(new LogOutputStream(this, Project.MSG_ERR), true);
    try {
      exception.printStackTrace(logSink);
    }
    finally {
      logSink.close();
    }
  }
}
