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

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;

import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;

import org.jbpm.bpel.tools.FileUtil;

/**
 * Posts process archives to the deployment service.
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2008/02/05 10:29:36 $
 */
public class DeploymentTask extends PostTask {

  private File processArchive;

  {
    setServiceUri("http://localhost:8080/jbpm-bpel/deployment");
  }

  protected void writeRequest(PostMethod post) throws IOException {
    // process part
    String contentType = URLConnection.getFileNameMap().getContentTypeFor(processArchive.getName());
    FilePart processPart = new FilePart("processArchive", processArchive, contentType,
        FileUtil.DEFAULT_CHARSET.name());

    // multipart request
    post.setRequestEntity(new MultipartRequestEntity(new Part[] { processPart }, post.getParams()));

    log("deploying process: " + processArchive.getName());
  }

  public void setProcessArchive(File processArchive) {
    this.processArchive = processArchive;
  }
}