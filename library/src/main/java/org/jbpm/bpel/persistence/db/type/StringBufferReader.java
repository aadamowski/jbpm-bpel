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
package org.jbpm.bpel.persistence.db.type;

import java.io.IOException;
import java.io.Reader;

/**
 * A character stream whose source is a string buffer.
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2007/03/05 12:58:13 $
 */
public class StringBufferReader extends Reader {

  private final StringBuffer buffer;
  private final int length;
  private int next;
  private int mark;

  /**
   * Creates a string buffer reader.
   * @param buffer StringBuffer providing the character source
   */
  public StringBufferReader(StringBuffer buffer) {
    super(buffer);
    this.buffer = buffer;
    length = buffer.length();
  }

  /**
   * Reads a single character.
   * @return the character read, or -1 if the end of the stream has been reached
   */
  public int read() {
    synchronized (lock) {
      if (next >= length)
        return -1;

      return buffer.charAt(next++);
    }
  }

  /**
   * Reads characters into a portion of an array.
   * @param cbuf destination buffer
   * @param off offset at which to start writing characters
   * @param len maximum number of characters to read
   */
  public int read(char[] cbuf, int off, int len) {
    if ((off < 0)
        || (off > cbuf.length)
        || (len < 0)
        || ((off + len) > cbuf.length)
        || ((off + len) < 0)) {
      throw new IndexOutOfBoundsException();
    }
    else if (len == 0)
      return 0;

    synchronized (lock) {
      if (next >= length)
        return -1;

      int n = Math.min(length - next, len);
      buffer.getChars(next, next += n, cbuf, off);
      return n;
    }
  }

  /**
   * Skips the specified number of characters in the stream.
   * <p>
   * The <code>ns</code> parameter may be negative. Negative values of
   * <code>ns</code> cause the stream to skip backwards. Negative return
   * values indicate a skip backwards. It is not possible to skip backwards past
   * the beginning of the string.
   * </p>
   * <p>
   * If the entire string has been read or skipped, then this method has no
   * effect and always returns 0
   * </p>
   * @return the number of characters actually skipped
   */
  public long skip(long ns) {
    synchronized (lock) {
      if (next >= length)
        return 0;

      // bound skip by beginning and end of the source
      long n = Math.min(length - next, ns);
      n = Math.max(-next, n);
      next += n;
      return n;
    }
  }

  /**
   * Tells whether this stream is ready to be read.
   * @return <code>true</code> always
   */
  public boolean ready() {
    return true;
  }

  /**
   * Tells whether this stream supports the {@link #mark(int)} operation.
   * @return <code>true</code> always
   */
  public boolean markSupported() {
    return true;
  }

  /**
   * Mark the present position in the stream. Subsequent calls to
   * {@link #reset()} will reposition the stream to this point.
   * @param readAheadLimit limit on the number of characters that may be read
   *        while still preserving the mark; because the stream's input comes
   *        from a string buffer, there is no actual limit, so this argument
   *        must not be negative, but is otherwise ignored
   * @throws IllegalArgumentException if readAheadLimit is < 0
   */
  public void mark(int readAheadLimit) {
    if (readAheadLimit < 0)
      throw new IllegalArgumentException("Read-ahead limit < 0");

    synchronized (lock) {
      mark = next;
    }
  }

  /**
   * Reset the stream to the most recent mark, or to the beginning of the string
   * if it has never been marked.
   */
  public void reset() {
    synchronized (lock) {
      next = mark;
    }
  }

  /**
   * Closing this stream has no effect. Methods in this class can be called
   * after the stream has been closed without generating an {@link IOException}.
   */
  public void close() {
  }
}
