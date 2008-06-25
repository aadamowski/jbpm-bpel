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
package org.jbpm.bpel.integration.def;

import java.io.Serializable;

import org.apache.commons.lang.enums.Enum;

/**
 * Indicates a correlation set ocurring in the message being sent or received in
 * an activity.
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/05/31 12:42:19 $
 */
public class Correlation implements Serializable {

  long id;
  private CorrelationSetDefinition set;
  private Initiate initiate;

  private static final long serialVersionUID = 1L;

  public CorrelationSetDefinition getSet() {
    return set;
  }

  public void setSet(CorrelationSetDefinition set) {
    this.set = set;
  }

  public Initiate getInitiate() {
    return initiate;
  }

  public void setInitiate(Initiate initiate) {
    this.initiate = initiate;
  }

  /**
   * The correlation value specifies whether the related activity should attempt
   * to initiate the correlation set.
   */
  public static final class Initiate extends Enum {

    /**
     * The related activity <i>must</i> attempt to initiate the correlation
     * set.
     */
    public static final Initiate YES = new Initiate("yes");

    /**
     * The related activity <i>must not</i> attempt to initiate the correlation
     * set.
     */
    public static final Initiate NO = new Initiate("no");

    /**
     * The related activity <i>must</i> attempt to initiate the correlation
     * set, if the correlation set is <i>not</i> initiated yet.
     */
    public static final Initiate JOIN = new Initiate("join");

    private static final long serialVersionUID = 1L;

    /**
     * Enumeration constructor.
     * @param name the desired textual representation.
     */
    private Initiate(String name) {
      super(name);
    }

    /**
     * Gets an enumeration object by name.
     * @param name a string that identifies one element
     * @return the appropiate enumeration object, or <code>null</code> if the
     * object does not exist
     */
    public static Initiate valueOf(String name) {
      return name != null ? (Initiate) getEnum(Initiate.class, name) : Initiate.NO;
    }
  }

  /**
   * The pattern value specifies whether a correlation applies to the outbound
   * (request) message, the inbound (response) message, or both. Used in the
   * case of invoke, when the operation is synchronous request/response.
   */
  public static final class Pattern extends Enum {

    /**
     * The correlation applies to the response message only.
     */
    public static final Pattern RESPONSE = new Pattern("response");

    /**
     * The correlation applies to the request message only.
     */
    public static final Pattern REQUEST = new Pattern("request");

    /**
     * The correlation applies to both request and response messages.
     */
    public static final Pattern REQUEST_RESPONSE = new Pattern("request-response");

    private static final long serialVersionUID = 1L;

    /**
     * Enumeration constructor.
     * @param name the desired textual representation.
     */
    private Pattern(String name) {
      super(name);
    }

    /**
     * Gets an enumeration object by name.
     * @param name a string that identifies one element
     * @return the appropiate enumeration object, or <code>null</code> if the
     * object does not exist
     */
    public static Pattern valueOf(String name) {
      return (Pattern) getEnum(Pattern.class, name);
    }
  }
}
