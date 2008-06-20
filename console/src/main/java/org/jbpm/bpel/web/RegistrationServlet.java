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
package org.jbpm.bpel.web;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import org.jbpm.bpel.integration.catalog.CatalogEntry;
import org.jbpm.bpel.integration.catalog.CentralCatalog;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/11/06 02:58:57 $
 */
public class RegistrationServlet extends HttpServlet {

  /** Request parameter: base location. */
  public static final String PARAM_BASE_LOCATION = "baseLocation";
  /** Request parameter: description file. */
  public static final String PARAM_DESCRIPTION_FILE = "descriptionFile";

  private static final long serialVersionUID = 1L;

  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    parseRequest(request);
    registerDefinition(request);
    response.sendRedirect("partners.jsp");
  }

  private void parseRequest(HttpServletRequest request) throws ServletException, IOException {
    if (!ServletFileUpload.isMultipartContent(request))
      throw new ServletException("request does not have multipart content");

    try {
      ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory());
      List items = upload.parseRequest(request);
      if (items.size() != 2)
        throw new ServletException("registration request must contain two parameters");

      request.setAttribute(PARAM_BASE_LOCATION, parseBaseLocation((FileItem) items.get(0)));
      request.setAttribute(PARAM_DESCRIPTION_FILE, parseDescriptionFile((FileItem) items.get(1)));
    }
    catch (FileUploadException e) {
      throw new ServletException("could not parse upload request", e);
    }
  }

  private String parseBaseLocation(FileItem locationItem) throws ServletException {
    if (!PARAM_BASE_LOCATION.equals(locationItem.getFieldName())) {
      throw new ServletException("expected parameter '"
          + PARAM_BASE_LOCATION
          + "', found: "
          + locationItem.getFieldName());
    }

    if (!locationItem.isFormField())
      throw new ServletException("parameter '"
          + PARAM_BASE_LOCATION
          + "' is not a simple form field");

    return locationItem.getString();
  }

  private InputStream parseDescriptionFile(FileItem descriptionItem) throws ServletException,
      IOException {
    if (!PARAM_DESCRIPTION_FILE.equals(descriptionItem.getFieldName())) {
      throw new ServletException("expected parameter '"
          + PARAM_DESCRIPTION_FILE
          + "', found: "
          + descriptionItem.getFieldName());
    }

    if (descriptionItem.isFormField()) {
      throw new ServletException("parameter '"
          + PARAM_DESCRIPTION_FILE
          + "' is not an uploaded file");
    }

    if (descriptionItem.getSize() == 0)
      return null;

    String contentType = descriptionItem.getContentType();
    if (!contentType.startsWith(WebConstants.CONTENT_TYPE_XML)) {
      throw new ServletException("parameter '"
          + PARAM_DESCRIPTION_FILE
          + "' is expected to have content type '"
          + WebConstants.CONTENT_TYPE_XML
          + "', found: "
          + contentType);
    }

    return descriptionItem.getInputStream();
  }

  private void registerDefinition(HttpServletRequest request) throws IOException {
    String baseLocation = (String) request.getAttribute(PARAM_BASE_LOCATION);
    InputStream descriptionSource = (InputStream) request.getAttribute(PARAM_DESCRIPTION_FILE);

    CentralCatalog catalog = CentralCatalog.getConfigurationInstance();
    catalog.addEntry(new CatalogEntry(baseLocation, descriptionSource));
  }
}
