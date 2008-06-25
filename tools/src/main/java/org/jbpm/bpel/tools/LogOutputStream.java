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
package org.jbpm.bpel.tools;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import org.apache.commons.logging.Log;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2008/02/05 05:37:34 $
 */
public class LogOutputStream extends OutputStream {

  private final Log log;
  private final ByteArrayOutputStream out = new ByteArrayOutputStream();
  private boolean skip;

  /** Carriage return */
  private static final byte CR = 0x0d;

  /** Line feed */
  private static final byte LF = 0x0a;

  public LogOutputStream(Log log) {
    this.log = log;
  }

  /**
   * Writes the specified byte to the internal buffer. Logs the buffer content if a line separator
   * is detected.
   * @param i the byte
   */
  public void write(int i) {
    writeByte((byte) i);
  }

  private void writeByte(byte b) {
    if (b != LF && b != CR)
      out.write(b);
    else if (!skip)
      logLine();

    skip = b == CR;
  }

  private void logLine() {
    if (out.size() > 0) {
      try {
        log.error(out.toString());
      }
      finally {
        out.reset();
      }
    }
  }

  /**
   * Writes bytes from the specified byte array to the internal buffer. Logs the buffer content if a
   * line separator is detected.
   * @param buffer the data
   * @param offset the start offset in the array
   * @param length the number of bytes to write
   */
  public void write(byte[] buffer, int offset, final int length) {
    if (offset < 0 || offset > buffer.length)
      throw new IndexOutOfBoundsException("offset: " + offset);

    if (length < 0)
      throw new IndexOutOfBoundsException("length: " + length);

    final int boundary = offset + length;
    if (boundary > buffer.length || boundary < 0)
      throw new IndexOutOfBoundsException("offset + length: " + boundary);

    // find line separators and pass other chars through in blocks
    while (offset < boundary) {
      // remember the offset where the block begins
      int blockOffset = offset;

      // find a line separator (or the buffer boundary)
      do {
        byte b = buffer[offset];
        if (b == LF || b == CR)
          break;
      } while (++offset < boundary);

      // write chars from the beginning of the block thru the line separator
      int blockLength = offset - blockOffset;
      if (blockLength > 0)
        out.write(buffer, blockOffset, blockLength);

      // write line separator
      for (; offset < boundary; offset++) {
        byte b = buffer[offset];
        if (b != LF && b != CR)
          break;
        writeByte(b);
      }
    }
  }

  /**
   * Forces the buffered output bytes to be logged.
   */
  public void flush() {
    logLine();
  }

  /**
   * Logs the output bytes remaining in the buffer.
   */
  public void close() {
    logLine();
  }
}
