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

import java.io.ByteArrayInputStream;
import java.net.URI;

import org.xml.sax.InputSource;

import org.jbpm.jpdl.par.ProcessArchive;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/05/03 12:25:53 $
 */
public class ProcessArchiveWsdlLocator extends ProcessWsdlLocator {

  private final ProcessArchive archive;

  public ProcessArchiveWsdlLocator(URI processURI, ProcessArchive archive) {
    super(processURI);
    this.archive = archive;
  }

  protected InputSource createInputSource(String documentLocation) {
    InputSource inputSource = new InputSource(documentLocation);
    byte[] entry = archive.getEntry(documentLocation);
    if (entry != null)
      inputSource.setByteStream(new ByteArrayInputStream(entry));
    return inputSource;
  }
}