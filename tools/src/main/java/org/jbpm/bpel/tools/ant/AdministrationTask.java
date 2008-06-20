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

import java.io.IOException;

import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.tools.ant.types.EnumeratedAttribute;

/**
 * Posts requests to the database administration service.
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2008/02/05 10:29:36 $
 */
public class AdministrationTask extends PostTask {

  private Operation operation;

  {
    setServiceUri("http://localhost:8080/jbpm-bpel/administration");
  }

  protected void writeRequest(PostMethod post) throws IOException {
    post.addParameter("operation", operation != null ? operation.getValue()
        : Operation.CREATE_SCHEMA);

    log("performing operation: " + operation);
  }

  public void setOperation(Operation operation) {
    this.operation = operation;
  }

  public static class Operation extends EnumeratedAttribute {

    /** Enumeration value: create schema */
    public static final String CREATE_SCHEMA = "create_schema";

    /** Enumeration value: drop schema */
    public static final String DROP_SCHEMA = "drop_schema";

    private static final String[] values = { CREATE_SCHEMA, DROP_SCHEMA };

    public String[] getValues() {
      return values;
    }
  }
}
