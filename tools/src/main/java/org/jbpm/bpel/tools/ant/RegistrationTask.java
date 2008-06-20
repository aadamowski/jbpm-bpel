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
import org.apache.commons.httpclient.methods.multipart.StringPart;

import org.jbpm.bpel.tools.FileUtil;

/**
 * Posts service descriptions to the registration service.
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2008/02/05 10:29:36 $
 */
public class RegistrationTask extends PostTask {

  private String baseLocation;
  private File descriptionFile;

  {
    setServiceUri("http://localhost:8080/jbpm-bpel/registration");
  }

  protected void writeRequest(PostMethod post) throws IOException {
    // base location
    StringPart locationPart = new StringPart("baseLocation", baseLocation);

    // description file
    String contentType = descriptionFile != null ? URLConnection.getFileNameMap()
        .getContentTypeFor(descriptionFile.getName()) : null;
    FilePart descriptionPart = new FilePart("descriptionFile", descriptionFile, contentType,
        FileUtil.DEFAULT_CHARSET.name());

    // multipart request
    post.setRequestEntity(new MultipartRequestEntity(new Part[] { locationPart, descriptionPart },
        post.getParams()));

    log("registering description: "
        + (descriptionFile != null ? descriptionFile.getName() : baseLocation));
  }

  public void setBaseLocation(String baseLocation) {
    this.baseLocation = baseLocation;
  }

  public void setDescriptionFile(File descriptionFile) {
    this.descriptionFile = descriptionFile;
  }
}
