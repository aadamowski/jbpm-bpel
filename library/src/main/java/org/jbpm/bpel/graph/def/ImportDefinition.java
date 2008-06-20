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
package org.jbpm.bpel.graph.def;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.wsdl.Definition;
import javax.wsdl.Fault;
import javax.wsdl.Input;
import javax.wsdl.Message;
import javax.wsdl.Operation;
import javax.wsdl.Output;
import javax.wsdl.PortType;
import javax.xml.namespace.QName;

import org.jbpm.bpel.graph.def.Import.Type;
import org.jbpm.bpel.variable.def.ElementType;
import org.jbpm.bpel.variable.def.MessageType;
import org.jbpm.bpel.variable.def.SchemaType;
import org.jbpm.bpel.wsdl.PartnerLinkType;
import org.jbpm.bpel.wsdl.Property;
import org.jbpm.bpel.wsdl.xml.WsdlUtil;
import org.jbpm.module.def.ModuleDefinition;
import org.jbpm.module.exe.ModuleInstance;

/**
 * Groups imported WSDL and XML Schema documents. Provides lookup facilites for the various elements
 * defined in those documents.
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/10/17 22:02:52 $
 */
public class ImportDefinition extends ModuleDefinition {

  private List imports = new ArrayList();

  private Map partnerLinkTypes = new HashMap();
  private Map portTypes = new HashMap();
  private Map properties = new HashMap();
  private Map messages = new HashMap();
  private Map messageTypes = new HashMap();
  private Map elementTypes = new HashMap();
  private Map schemaTypes = new HashMap();

  private static final long serialVersionUID = 1L;

  public List getImports() {
    return imports;
  }

  public void addImport(Import _import) {
    imports.add(_import);
  }

  public void addImports(Collection imports) {
    this.imports.addAll(imports);
  }

  public PartnerLinkType getPartnerLinkType(QName name) {
    PartnerLinkType partnerLinkType = (PartnerLinkType) partnerLinkTypes.get(name);
    if (partnerLinkType != null)
      return partnerLinkType;

    for (int i = 0, n = imports.size(); i < n; i++) {
      Import _import = (Import) imports.get(i);
      if (!Type.WSDL.equals(_import.getType()))
        continue; // not a wsdl document, skip it

      partnerLinkType = WsdlUtil.getPartnerLinkType((Definition) _import.getDocument(), name);
      if (partnerLinkType != null) {
        addPartnerLinkType(partnerLinkType);
        return partnerLinkType;
      }
    }
    return null;
  }

  public void addPartnerLinkType(PartnerLinkType partnerLinkType) {
    // register port type of first role
    PartnerLinkType.Role role = partnerLinkType.getFirstRole();
    role.setPortType(addPortType(role.getPortType()));

    // register port type of second role
    role = partnerLinkType.getSecondRole();
    if (role != null)
      role.setPortType(addPortType(role.getPortType()));

    // register the partner link type
    partnerLinkTypes.put(partnerLinkType.getQName(), partnerLinkType);
  }

  public PortType getPortType(QName name) {
    PortType portType = (PortType) portTypes.get(name);
    if (portType != null)
      return portType;

    for (int i = 0, n = imports.size(); i < n; i++) {
      Import _import = (Import) imports.get(i);
      if (!Type.WSDL.equals(_import.getType()))
        continue; // not a wsdl document, skip it

      portType = WsdlUtil.getPortType((Definition) _import.getDocument(), name);
      if (portType != null) {
        portTypes.put(name, portType);
        return portType;
      }
    }
    return null;
  }

  public PortType addPortType(PortType portType) {
    QName name = portType.getQName();

    PortType internPortType = (PortType) portTypes.get(name);
    if (internPortType != null)
      return internPortType;

    // for all operations, register the input, output and fault messages
    List operations = portType.getOperations();
    for (int i = 0, n = operations.size(); i < n; i++) {
      Operation operation = (Operation) operations.get(i);

      // input
      Input input = operation.getInput();
      if (input != null)
        input.setMessage(addMessage(input.getMessage()));

      // output
      Output output = operation.getOutput();
      if (output != null)
        output.setMessage(addMessage(output.getMessage()));

      // faults
      for (Iterator f = operation.getFaults().values().iterator(); f.hasNext();) {
        Fault fault = (Fault) f.next();
        fault.setMessage(addMessage(fault.getMessage()));
      }
    }

    // register the port type itself
    portTypes.put(name, portType);
    return portType;
  }

  public Message getMessage(QName name) {
    Message message = (Message) messages.get(name);
    if (message != null)
      return message;

    for (int i = 0, n = imports.size(); i < n; i++) {
      Import _import = (Import) imports.get(i);
      if (!Type.WSDL.equals(_import.getType()))
        continue; // not a wsdl document, skip it

      message = WsdlUtil.getMessage((Definition) _import.getDocument(), name);
      if (message != null) {
        messages.put(name, message);
        return message;
      }
    }
    return null;
  }

  public Message addMessage(Message message) {
    QName name = message.getQName();

    Message internMessage = (Message) messages.get(name);
    if (internMessage != null)
      return internMessage;

    messages.put(name, message);
    addMessageType(message);
    return message;
  }

  public Property getProperty(QName name) {
    Property property = (Property) properties.get(name);
    if (property != null)
      return property;

    for (int i = 0, n = imports.size(); i < n; i++) {
      Import _import = (Import) imports.get(i);
      if (!Type.WSDL.equals(_import.getType()))
        continue; // not a wsdl document, skip it

      property = WsdlUtil.getProperty((Definition) _import.getDocument(), name);
      if (property != null) {
        properties.put(name, property);
        return property;
      }
    }
    return null;
  }

  public void addProperty(Property property) {
    properties.put(property.getQName(), property);
  }

  public Definition getDeclaringDefinition(PortType portType) {
    QName portTypeName = portType.getQName();

    for (int i = 0, n = imports.size(); i < n; i++) {
      Import _import = (Import) imports.get(i);

      if (!Type.WSDL.equals(_import.getType()))
        continue;

      Definition baseDef = (Definition) _import.getDocument();

      // look in local elements
      if (baseDef.getPortTypes().containsKey(portTypeName))
        return baseDef;

      Definition innerDef = getDeclaringDefinition(baseDef, portTypeName);
      if (innerDef != null)
        return innerDef;
    }

    return null;
  }

  private static Definition getDeclaringDefinition(Definition baseDef, QName portTypeName) {
    // look in imports with a matching target namespace
    List imports = baseDef.getImports(portTypeName.getNamespaceURI());
    if (imports != null) {
      for (int i = 0, n = imports.size(); i < n; i++) {
        javax.wsdl.Import _import = (javax.wsdl.Import) imports.get(i);
        Definition innerDef = _import.getDefinition();
        if (innerDef.getPortTypes().containsKey(portTypeName))
          return innerDef;
      }
    }

    // look in imported definitions of all imports
    for (Iterator l = baseDef.getImports().values().iterator(); l.hasNext();) {
      imports = (List) l.next();
      for (int i = 0, n = imports.size(); i < n; i++) {
        javax.wsdl.Import _import = (javax.wsdl.Import) imports.get(i);
        Definition innerDef = getDeclaringDefinition(_import.getDefinition(), portTypeName);
        if (innerDef != null)
          return innerDef;
      }
    }
    return null;
  }

  public MessageType getMessageType(QName name) {
    MessageType type = (MessageType) messageTypes.get(name);
    if (type == null) {
      Message message = getMessage(name);
      if (message != null)
        type = addMessageType(message);
    }
    return type;
  }

  private MessageType addMessageType(Message message) {
    MessageType type = new MessageType(message);
    messageTypes.put(message.getQName(), type);
    return type;
  }

  public ElementType getElementType(QName name) {
    ElementType type = (ElementType) elementTypes.get(name);
    if (type == null) {
      type = new ElementType(name);
      elementTypes.put(name, type);
    }
    return type;
  }

  public SchemaType getSchemaType(QName name) {
    SchemaType type = (SchemaType) schemaTypes.get(name);
    if (type == null) {
      type = new SchemaType(name);
      schemaTypes.put(name, type);
    }
    return type;
  }

  public ModuleInstance createInstance() {
    return null;
  }
}
