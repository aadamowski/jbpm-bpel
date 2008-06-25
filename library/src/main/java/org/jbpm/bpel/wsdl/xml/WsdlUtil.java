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
package org.jbpm.bpel.wsdl.xml;

import java.io.BufferedWriter;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.wsdl.Definition;
import javax.wsdl.Message;
import javax.wsdl.Part;
import javax.wsdl.PortType;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLWriter;
import javax.xml.namespace.QName;

import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.iterators.FilterIterator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.util.EmptyIterator;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.jbpm.bpel.graph.def.Import;
import org.jbpm.bpel.graph.def.Import.Type;
import org.jbpm.bpel.wsdl.PartnerLinkType;
import org.jbpm.bpel.wsdl.Property;
import org.jbpm.bpel.xml.util.XmlUtil;

/**
 * Utility methods for dealing with JWSDL objects.
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/10/17 22:02:52 $
 */
public class WsdlUtil {

  private static final WSDLFactory factory = new WsdlFactoryImpl();
  private static Definition sharedDefinition;
  private static ExtensionRegistry sharedRegistry;

  private static final Log log = LogFactory.getLog(WsdlUtil.class);

  private WsdlUtil() {
    // supress default constructor, ensuring non-instantiability
  }

  public static WSDLFactory getFactory() {
    return factory;
  }

  public static Definition getSharedDefinition() {
    if (sharedDefinition == null)
      sharedDefinition = factory.newDefinition();

    return sharedDefinition;
  }

  public static ExtensionRegistry getSharedExtensionRegistry() {
    if (sharedRegistry == null)
      sharedRegistry = factory.newPopulatedExtensionRegistry();

    return sharedRegistry;
  }

  public static Definition readText(String text) throws WSDLException {
    return factory.newWSDLReader().readWSDL(null, new InputSource(new StringReader(text)));
  }

  public static Definition readResource(Class clazz, String resource) throws WSDLException {
    return factory.newWSDLReader().readWSDL(clazz.getResource(resource).toExternalForm());
  }

  public static void writeFile(File file, Definition def, WSDLWriter writer) throws WSDLException {
    // create parent directory if it does not exist
    File parentDir = file.getParentFile();
    if (parentDir != null)
      parentDir.mkdirs();

    // write wsdl document
    try {
      writeFileImpl(file, def, writer);
    }
    catch (IOException e) {
      throw new WSDLException(WSDLException.OTHER_ERROR, "could not write file: " + file, e);
    }
  }

  private static void writeFileImpl(File file, Definition def, WSDLWriter writer)
      throws WSDLException, IOException {
    // open a file for writing
    OutputStream fileSink = new FileOutputStream(file);
    try {
      Writer encoderSink;
      try {
        // try UTF-8 encoding
        encoderSink = new OutputStreamWriter(fileSink, "UTF-8");
      }
      catch (UnsupportedEncodingException e) {
        log.debug("could not use UTF-8 to write WSDL document", e);
        // fall back to platform's default encoding
        encoderSink = new OutputStreamWriter(fileSink);
      }
      // write wsdl document to file
      writer.writeWSDL(def, new BufferedWriter(encoderSink));
    }
    finally {
      fileSink.close();
    }
  }

  public static Definition writeAndRead(Definition definition) throws WSDLException {
    // write the definition to an in-memory sink
    CharArrayWriter memorySink = new CharArrayWriter();
    factory.newWSDLWriter().writeWSDL(definition, memorySink);
    // read the definition back from memory
    try {
      return factory.newWSDLReader().readWSDL(definition.getDocumentBaseURI(),
          XmlUtil.parseText(memorySink.toString()));
    }
    catch (SAXException e) {
      throw new WSDLException(WSDLException.PARSER_ERROR,
          "could not read the WSDL definitions back", e);
    }
  }

  public static PortType getPortType(Definition def, QName name) {
    return (PortType) new WsdlElementLookup() {

      protected Object getLocalElement(Definition def, QName name) {
        return def.getPortTypes().get(name);
      }
    }.getElement(def, name);
  }

  public static Message getMessage(Definition def, QName name) {
    return (Message) new WsdlElementLookup() {

      protected Object getLocalElement(Definition def, QName name) {
        return def.getMessages().get(name);
      }
    }.getElement(def, name);
  }

  public static PartnerLinkType getPartnerLinkType(Definition def, QName name) {
    return (PartnerLinkType) new WsdlElementLookup() {

      protected Object getLocalElement(Definition def, QName name) {
        for (Iterator i = getExtensions(def.getExtensibilityElements(),
            WsdlConstants.Q_PARTNER_LINK_TYPE); i.hasNext();) {
          PartnerLinkType partnerLinkType = (PartnerLinkType) i.next();
          if (partnerLinkType.getQName().equals(name))
            return partnerLinkType;
        }
        return null;
      }
    }.getElement(def, name);
  }

  public static Property getProperty(Definition def, QName name) {
    return (Property) new WsdlElementLookup() {

      protected Object getLocalElement(Definition def, QName name) {
        for (Iterator i = getExtensions(def.getExtensibilityElements(), WsdlConstants.Q_PROPERTY); i.hasNext();) {
          Property property = (Property) i.next();
          if (property.getQName().equals(name))
            return property;
        }
        return null;
      }
    }.getElement(def, name);
  }

  public static ExtensibilityElement getExtension(List extensions, QName extensionType) {
    if (extensions != null) {
      for (Iterator i = extensions.iterator(); i.hasNext();) {
        ExtensibilityElement extension = (ExtensibilityElement) i.next();
        if (ExtensionTypePredicate.evaluate(extension, extensionType))
          return extension;
      }
    }
    return null;
  }

  public static Iterator getExtensions(List extensions, QName extensionType) {
    return extensions != null ? new FilterIterator(extensions.iterator(),
        new ExtensionTypePredicate(extensionType)) : EmptyIterator.INSTANCE;
  }

  public static QName getDocLitElementName(Message message) {
    Map parts = message.getParts();
    return parts.size() == 1 ? ((Part) parts.values().iterator().next()).getElementName() : null;
  }

  public static Import createImport(Definition def) {
    Import imp = new Import();
    imp.setNamespace(def.getTargetNamespace());
    imp.setType(Type.WSDL);
    imp.setDocument(def);
    return imp;
  }

  private static abstract class WsdlElementLookup {

    public Object getElement(Definition def, QName name) {
      Object element = getLocalElement(def, name);
      if (element == null)
        element = getImportedElement(def, name);
      return element;
    }

    protected Object getImportedElement(Definition def, QName name) {
      // look in imports with a matching target namespace
      List importList = def.getImports(name.getNamespaceURI());
      if (importList != null) {
        for (int i = 0, n = importList.size(); i < n; i++) {
          javax.wsdl.Import _import = (javax.wsdl.Import) importList.get(i);
          Object element = getLocalElement(_import.getDefinition(), name);
          if (element != null)
            return element;
        }
      }
      // look in imported definitions of all imports
      for (Iterator l = def.getImports().values().iterator(); l.hasNext();) {
        importList = (List) l.next();
        for (int i = 0, n = importList.size(); i < n; i++) {
          javax.wsdl.Import _import = (javax.wsdl.Import) importList.get(i);
          Object element = getImportedElement(_import.getDefinition(), name);
          if (element != null)
            return element;
        }
      }
      return null;
    }

    protected abstract Object getLocalElement(Definition def, QName name);
  }

  private static class ExtensionTypePredicate implements Predicate {

    private final QName extensionType;

    ExtensionTypePredicate(QName type) {
      extensionType = type;
    }

    public boolean evaluate(Object arg) {
      return evaluate((ExtensibilityElement) arg, extensionType);
    }

    public static boolean evaluate(ExtensibilityElement extension, QName extensionType) {
      return extension.getElementType().equals(extensionType);
    }
  }
}
