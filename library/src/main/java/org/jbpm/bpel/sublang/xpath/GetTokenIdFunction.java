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
package org.jbpm.bpel.sublang.xpath;

import java.util.List;

import org.jaxen.Context;
import org.jaxen.Function;
import org.jaxen.FunctionCallException;
import org.jaxen.VariableContext;

import org.jbpm.persistence.db.DbPersistenceService;
import org.jbpm.svc.Service;
import org.jbpm.svc.Services;

/**
 * The <code>getTokenId</code> function extracts the identifier of the jBPM token in the context
 * where the call occurs.
 * <p>
 * <code><i>number</i> jbpm:getTokenId()</code>
 * </p>
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/11/16 19:47:08 $
 */
public class GetTokenIdFunction implements Function {

  /**
   * Extracts the identifier of the jBPM token in the given context.
   * @param context the context where the function is called
   * @param args an empty list; this function requires no arguments
   * @return the token id
   * @throws FunctionCallException if <code>args</code> is not empty
   */
  public Object call(Context context, List args) throws FunctionCallException {
    if (!args.isEmpty())
      throw new FunctionCallException("getTokenId() requires no arguments");

    return evaluate(context);
  }

  /**
   * Extracts the identifier of the jBPM token in the given context.
   * @param context the context to extract the token from
   * @return the token id
   * @throws FunctionCallException if no token is present in the given context
   */
  public static Number evaluate(Context context) throws FunctionCallException {
    VariableContext variableContext = context.getContextSupport().getVariableContext();
    if (!(variableContext instanceof TokenVariableContext))
      throw new FunctionCallException("no token is present in the given context");

    // ensure the token exists in the database by starting a new transaction
    Service persistenceService = Services.getCurrentService(Services.SERVICENAME_PERSISTENCE, false);
    if (persistenceService instanceof DbPersistenceService) {
      DbPersistenceService dbPersistenceService = (DbPersistenceService) persistenceService;
      dbPersistenceService.endTransaction();
      dbPersistenceService.beginTransaction();
    }

    TokenVariableContext tokenVariableContext = (TokenVariableContext) variableContext;
    return new Long(tokenVariableContext.getToken().getId());
  }
}
