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
package org.jbpm.bpel.sublang.def;

import javax.wsdl.Definition;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.xml.namespace.QName;

import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.def.ImportDefinition;
import org.jbpm.bpel.variable.def.VariableType;
import org.jbpm.bpel.wsdl.Property;
import org.jbpm.bpel.wsdl.PropertyAlias;
import org.jbpm.bpel.wsdl.xml.WsdlConstants;
import org.jbpm.bpel.wsdl.xml.WsdlUtil;
import org.jbpm.bpel.xml.BpelConstants;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/09/12 23:20:17 $
 */
public class PropertyQueryDbTest extends SnippetDbTestCase {

  private static final QName TYPE_NAME = new QName(BpelConstants.NS_EXAMPLES, "car");
  private static final QName PROPERTY_NAME = new QName(BpelConstants.NS_EXAMPLES, "color");

  protected Snippet createSnippet(BpelProcessDefinition processDefinition) {
    // query
    PropertyQuery query = new PropertyQuery();

    try {
      // property
      ExtensionRegistry registry = WsdlUtil.getSharedExtensionRegistry();
      Property property = (Property) registry.createExtension(Definition.class,
          WsdlConstants.Q_PROPERTY);
      property.setQName(PROPERTY_NAME);

      ImportDefinition importDefinition = processDefinition.getImportDefinition();
      importDefinition.addProperty(property);

      // alias
      PropertyAlias alias = (PropertyAlias) registry.createExtension(Definition.class,
          WsdlConstants.Q_PROPERTY_ALIAS);
      alias.setProperty(property);
      alias.setType(TYPE_NAME);
      alias.setQuery(query);

      // type
      VariableType type = importDefinition.getSchemaType(TYPE_NAME);
      type.addPropertyAlias(alias);

      return query;
    }
    catch (WSDLException e) {
      // cannot happen
      throw new Error(e);
    }
  }

  protected Snippet getSnippet(BpelProcessDefinition processDefinition) {
    return processDefinition.getImportDefinition().getSchemaType(TYPE_NAME).getPropertyAlias(
        PROPERTY_NAME).getQuery();
  }
}
