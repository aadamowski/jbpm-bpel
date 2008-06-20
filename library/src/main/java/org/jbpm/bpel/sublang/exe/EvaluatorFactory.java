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
package org.jbpm.bpel.sublang.exe;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import org.jbpm.JbpmConfiguration;
import org.jbpm.bpel.sublang.def.VariableQuery;
import org.jbpm.bpel.sublang.def.Expression;
import org.jbpm.bpel.sublang.def.PropertyQuery;
import org.jbpm.bpel.xml.util.XmlUtil;
import org.jbpm.util.ClassLoaderUtil;

/**
 * Evaluator factories produce BPEL expression and query evaluators.
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/09/12 23:20:15 $
 */
public abstract class EvaluatorFactory {

  public static final String RESOURCE_EVALUATOR_FACTORIES = "resource.evaluator.factories";

  private static final Log log = LogFactory.getLog(EvaluatorFactory.class);

  private static Map factories = readEvaluatorFactories();

  protected EvaluatorFactory() {
  }

  public abstract ExpressionEvaluator createEvaluator(Expression expression);

  public abstract PropertyQueryEvaluator createEvaluator(PropertyQuery query);

  public abstract VariableQueryEvaluator createEvaluator(VariableQuery query);

  public static EvaluatorFactory getInstance(String language) {
    return (EvaluatorFactory) factories.get(language);
  }

  private static Map readEvaluatorFactories() {
    // get evaluator factories resource name
    String resource = JbpmConfiguration.Configs.getString(RESOURCE_EVALUATOR_FACTORIES);

    // parse evaluator factories document
    Element factoriesElem;
    try {
      // parse xml document
      factoriesElem = XmlUtil.parseResource(resource);
    }
    catch (SAXException e) {
      log.error("evaluator factories document contains invalid xml: " + resource, e);
      return Collections.EMPTY_MAP;
    }
    catch (IOException e) {
      log.error("could not read evaluator factories document: " + resource, e);
      return Collections.EMPTY_MAP;
    }

    // walk through evaluatorFactory elements
    HashMap factories = new HashMap();
    for (Iterator i = XmlUtil.getElements(factoriesElem, null, "evaluatorFactory"); i.hasNext();) {
      Element factoryElem = (Element) i.next();
      String language = factoryElem.getAttribute("language");

      // load factory class
      String factoryClassName = factoryElem.getAttribute("class");
      Class factoryClass = ClassLoaderUtil.loadClass(factoryClassName);

      // validate factory class
      if (!EvaluatorFactory.class.isAssignableFrom(factoryClass)) {
        log.warn("not an evaluator factory: " + factoryClassName);
        continue;
      }

      try {
        // instantiate factory
        EvaluatorFactory factory = (EvaluatorFactory) factoryClass.newInstance();

        // register factory instance
        factories.put(language, factory);
        log.debug("registered evaluator factory: language="
            + language
            + ", class="
            + factoryClassName);
      }
      catch (InstantiationException e) {
        log.warn("evaluator factory class not instantiable: " + factoryClassName, e);
      }
      catch (IllegalAccessException e) {
        log.warn("evaluator factory class or constructor not public: " + factoryClassName, e);
      }
    }
    return factories;
  }
}
