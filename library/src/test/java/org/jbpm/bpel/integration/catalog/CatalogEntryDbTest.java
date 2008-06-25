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
package org.jbpm.bpel.integration.catalog;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.wsdl.Definition;
import javax.wsdl.WSDLException;

import org.jbpm.bpel.persistence.db.AbstractDbTestCase;
import org.jbpm.bpel.persistence.db.IntegrationSession;
import org.jbpm.bpel.wsdl.xml.WsdlUtil;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/11/02 16:46:21 $
 */
public class CatalogEntryDbTest extends AbstractDbTestCase {

  private IntegrationSession integrationSession;

  protected void initializeMembers() {
    super.initializeMembers();
    integrationSession = IntegrationSession.getContextInstance(jbpmContext);
  }

  public void testBaseUri() throws IOException, WSDLException {
    String documentUri = getClass().getResource("atm.wsdl").toExternalForm();
    CatalogEntry catalogEntry = new CatalogEntry(documentUri, null);

    integrationSession.saveCatalogEntry(catalogEntry);
    newTransaction();

    assertEquals(documentUri, catalogEntry.getBaseLocation());
    assertFalse(catalogEntry.hasDescriptionBody());

    Definition definition = catalogEntry.readDefinition(WsdlUtil.getFactory().newWSDLReader());
    assertEquals("urn:samples:ATMService", definition.getTargetNamespace());
  }

  public void testUriAndStream() throws IOException, WSDLException {
    URL documentUrl = getClass().getResource("atm.wsdl");
    InputStream documentStream = documentUrl.openStream();
    try {
      String documentUri = documentUrl.toExternalForm();
      CatalogEntry catalogEntry = new CatalogEntry(documentUri, documentStream);

      integrationSession.saveCatalogEntry(catalogEntry);
      newTransaction();

      assertEquals(documentUri, catalogEntry.getBaseLocation());
      assertTrue(catalogEntry.hasDescriptionBody());

      Definition definition = catalogEntry.readDefinition(WsdlUtil.getFactory().newWSDLReader());
      assertEquals("urn:samples:ATMService", definition.getTargetNamespace());
    }
    finally {
      documentStream.close();
    }
  }

  public void testStream() throws IOException, WSDLException {
    InputStream documentStream = getClass().getResourceAsStream("atm.wsdl");
    try {
      CatalogEntry catalogEntry = new CatalogEntry(null, documentStream);

      integrationSession.saveCatalogEntry(catalogEntry);
      newTransaction();

      assertNull(catalogEntry.getBaseLocation());
      assertTrue(catalogEntry.hasDescriptionBody());

      Definition definition = catalogEntry.readDefinition(WsdlUtil.getFactory().newWSDLReader());
      assertEquals("urn:samples:ATMService", definition.getTargetNamespace());
    }
    finally {
      documentStream.close();
    }
  }
}
