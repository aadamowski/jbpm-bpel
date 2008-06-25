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

import javax.wsdl.Definition;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;

import org.jbpm.bpel.wsdl.impl.DefinitionImpl;
import org.jbpm.bpel.wsdl.impl.PartnerLinkTypeImpl;
import org.jbpm.bpel.wsdl.impl.PropertyAliasImpl;
import org.jbpm.bpel.wsdl.impl.PropertyImpl;

/**
 * Enhances the WSDL4J implementation of the {@link WSDLFactory} to accommodate
 * extension definitions introduced by BPEL.
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/01/22 00:24:55 $
 */
public class WsdlFactoryImpl extends com.ibm.wsdl.factory.WSDLFactoryImpl {

  /**
   * Creates a {@link Definition} instance.
   * @return a newly created definition
   */
  public Definition newDefinition() {
    Definition def = new DefinitionImpl();
    ExtensionRegistry extReg = newPopulatedExtensionRegistry();

    def.setExtensionRegistry(extReg);

    return def;
  }

  /**
   * Creates a {@link WSDLReader} instance, with an extension registry as
   * returned by the {@link #newPopulatedExtensionRegistry()} method.
   * @return a newly created reader
   */
  public WSDLReader newWSDLReader() {
    WSDLReader reader = super.newWSDLReader();
    reader.setFactoryImplName(getClass().getName());
    reader.setExtensionRegistry(newPopulatedExtensionRegistry());
    reader.setFeature("javax.wsdl.verbose", false);
    return reader;
  }

  /**
   * Create an {@link ExtensionRegistry} instance with pre-registered
   * serializers/deserializers, and Java extensionTypes mapped, for the SOAP,
   * HTTP, MIME, Partner LinkDefinition Type and Message Property extensions.
   * @return a newly created extension registry
   */
  public ExtensionRegistry newPopulatedExtensionRegistry() {
    ExtensionRegistry registry = super.newPopulatedExtensionRegistry();

    // partner link type
    PartnerLinkTypeSerializer plinkTypeSerializer = new PartnerLinkTypeSerializer();
    registry.registerDeserializer(Definition.class,
        WsdlConstants.Q_PARTNER_LINK_TYPE, plinkTypeSerializer);
    registry.registerSerializer(Definition.class,
        WsdlConstants.Q_PARTNER_LINK_TYPE, plinkTypeSerializer);
    registry.mapExtensionTypes(Definition.class,
        WsdlConstants.Q_PARTNER_LINK_TYPE, PartnerLinkTypeImpl.class);

    // property
    PropertySerializer propertySerializer = new PropertySerializer();
    registry.registerDeserializer(Definition.class, WsdlConstants.Q_PROPERTY,
        propertySerializer);
    registry.registerSerializer(Definition.class, WsdlConstants.Q_PROPERTY,
        propertySerializer);
    registry.mapExtensionTypes(Definition.class, WsdlConstants.Q_PROPERTY,
        PropertyImpl.class);

    // property alias
    PropertyAliasSerializer aliasSerializer = new PropertyAliasSerializer();
    registry.registerDeserializer(Definition.class,
        WsdlConstants.Q_PROPERTY_ALIAS, aliasSerializer);
    registry.registerSerializer(Definition.class,
        WsdlConstants.Q_PROPERTY_ALIAS, aliasSerializer);
    registry.mapExtensionTypes(Definition.class,
        WsdlConstants.Q_PROPERTY_ALIAS, PropertyAliasImpl.class);

    return registry;
  }
}
