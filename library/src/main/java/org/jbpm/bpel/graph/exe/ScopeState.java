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
package org.jbpm.bpel.graph.exe;

import java.io.Serializable;

import org.apache.commons.lang.enums.Enum;

/**
 * @author Juan Cantú
 * @version $Revision$ $Date: 2008/02/01 05:43:09 $
 */
public abstract class ScopeState extends Enum implements Serializable {

  /**
   * Constructs a scope state identified by the given name.
   * @param name
   */
  protected ScopeState(String name) {
    super(name);
  }

  public final Class getEnumClass() {
    return ScopeState.class;
  }

  /**
   * Requests cancellation of the scope instance argument.
   * @param scope the scope instance to terminate
   */
  public void terminate(ScopeInstance scope) {
    throwStateException("terminate");
  }

  /**
   * Requests compensation of the scope instance argument.
   * @param scope the scope instance to compensate
   */
  public void compensate(ScopeInstance scope) {
    throwStateException("compensate");
  }

  /**
   * Notifies the completion of the given scope instance.
   * @param scope the scope instance that completed
   */
  public void completed(ScopeInstance scope) {
    throwStateException("completed");
  }

  /**
   * Notifies the occurrence of a fault in the given scope instance.
   * @param scope the scope instance that faulted
   */
  public void faulted(ScopeInstance scope) {
    throwStateException("faulted");
  }

  /**
   * Notifies the termination of the children of the scope instance argument.
   * @param scope the scope instance whose children were terminated
   */
  public void childrenTerminated(ScopeInstance scope) {
    throwStateException("childrenTerminated");
  }

  /**
   * Notifies the compensation of the children of the scope instance argument.
   * @param scope the scope instance whose children were compensated
   */
  public void childrenCompensated(ScopeInstance scope) {
    throwStateException("childrenCompensated");
  }
  
  /**
   * Tells whether this state is terminable.
   */
  public boolean isTerminable() {
    return false;
  }
  
  /**
   * Tells whether this state is an end state.
   */
  public boolean isEnd() {
    return false;
  }

  /**
   * Creates an exception to signal that the given transition is illegal from the current state.
   * @param transition the name of the illegal transition
   */
  private void throwStateException(String transition) throws IllegalStateException {
    throw new IllegalStateException(this + ": transition=" + transition);
  }
}
