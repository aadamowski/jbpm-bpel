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
package org.jbpm.bpel.integration.client;

import java.util.Map;

import javax.xml.rpc.Call;
import javax.xml.rpc.handler.Handler;

/**
 * Provides support for the dynamic invocation of a service endpoint. This is the counterpart of
 * {@link Handler} on the client side and is akin to {@link Call}, except that it is assumed to be
 * preconfigured and treats input/output parts as maps instead of lists.
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2008/02/05 05:27:24 $
 */
public interface Caller {

  public Map call(String operation, Map inputParts);

  public void callOneWay(String operation, Map inputParts);

  public void close();

  public static class Key {

    private final long invokeActionId;
    private final long tokenId;

    public Key(long invokeActionId, long tokenId) {
      this.invokeActionId = invokeActionId;
      this.tokenId = tokenId;
    }

    public long getInvokeActionId() {
      return invokeActionId;
    }

    public long getTokenId() {
      return tokenId;
    }

    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (int) (invokeActionId ^ (invokeActionId >>> 32));
      result = prime * result + (int) (tokenId ^ (tokenId >>> 32));
      return result;
    }

    public boolean equals(Object other) {
      if (this == other)
        return true;
      if (!(other instanceof Key))
        return false;
      final Key that = (Key) other;
      return invokeActionId == that.invokeActionId && tokenId == that.tokenId;
    }

  }
}