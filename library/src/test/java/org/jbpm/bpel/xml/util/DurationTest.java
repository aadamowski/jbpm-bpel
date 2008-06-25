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
package org.jbpm.bpel.xml.util;

import org.jbpm.bpel.xml.util.Duration;

import junit.framework.TestCase;

/**
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2008/02/01 05:45:29 $
 */
public class DurationTest extends TestCase {

  private Duration fullDuration = new Duration(8, 12, 20, 15, 45, 30, 50);
  private Duration dateDuration = new Duration(12, 75, 60, 0, 0, 0, 0);
  private Duration timeDuration = new Duration(0, 0, 0, 28, 35, 140, 700);

  public DurationTest(String name) {
    super(name);
  }

  public void testParseFull() {
    String literal = "P8Y12M20DT15H45M30.05S";
    assertEquals(fullDuration, Duration.valueOf(literal));
  }

  public void testParseDate() {
    String literal = "P12Y75M60D";
    assertEquals(dateDuration, Duration.valueOf(literal));
  }

  public void testParseTime() {
    String literal = "PT28H35M140.7S";
    assertEquals(timeDuration, Duration.valueOf(literal));
  }

  public void testFormatFull() {
    assertEquals("P8Y12M20DT15H45M30.05S", fullDuration.toString());
  }

  public void testFormatDate() {
    assertEquals("P12Y75M60D", dateDuration.toString());
  }

  public void testFormatTime() {
    assertEquals("PT28H35M140.7S", timeDuration.toString());
  }
}
