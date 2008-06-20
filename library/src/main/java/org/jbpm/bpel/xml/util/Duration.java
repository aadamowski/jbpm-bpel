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

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Duration represents an interval of time. The value space of duration is a six-dimensional space
 * where the coordinates designate the Gregorian year, month, day, hour, minute, and second
 * components defined in <a href="http://www.w3.org/TR/xmlschema-2/#ISO8601">ISO 8601</a>.
 * @author Alejandro Guízar
 * @version $Revision$ $Date: 2008/02/01 05:45:29 $
 * @see <a href="http://www.w3.org/TR/xmlschema-2/#duration"> XML Schema Part 2: Datatypes
 * &sect;3.2.6</a>
 */
public class Duration implements Serializable {

  private static final long serialVersionUID = 1L;

  private int years;
  private int months;
  private int days;
  private int hours;
  private int minutes;
  private int seconds;
  private int millis;

  private boolean negative;

  private static final Pattern durationPattern = Pattern.compile("(-)?" // negative sign
      + "P" // designator
      + "(?:(\\p{Digit}+)Y)?" // years
      + "(?:(\\p{Digit}+)M)?" // months
      + "(?:(\\p{Digit}+)D)?" // days
      + "(?:T" // time *begin
      + "(?:(\\p{Digit}+)H)?" // hours
      + "(?:(\\p{Digit}+)M)?" // minutes
      + "(?:" // seconds (begin)
      + "(\\p{Digit}+)" // whole seconds
      + "(?:\\." // fractional seconds (begin)
      + "(\\p{Digit}{1,3})" // milliseconds
      + "\\p{Digit}*" // duration below milliseconds (ignored)
      + ")?" // fractional seconds (end)
      + "S)?" // seconds (end)
      + ")?" // time (end)
  );

  public Duration() {
  }

  public Duration(int year, int month, int day, int hour, int minute, int second, int millis) {
    setYears(year);
    setMonths(month);
    setDays(day);
    setHours(hour);
    setMinutes(minute);
    setSeconds(second);
    setMillis(millis);
  }

  public boolean isNegative() {
    return negative;
  }

  public void setNegative(boolean negative) {
    this.negative = negative;
  }

  public int getYears() {
    return years;
  }

  public void setYears(int years) {
    if (years < 0)
      throw new IllegalArgumentException("years cannot be negative");

    this.years = years;
  }

  public int getMonths() {
    return months;
  }

  public void setMonths(int months) {
    if (months < 0)
      throw new IllegalArgumentException("months cannot be negative");

    this.months = months;
  }

  public int getDays() {
    return days;
  }

  public void setDays(int days) {
    if (days < 0)
      throw new IllegalArgumentException("days cannot be negative");

    this.days = days;
  }

  public int getHours() {
    return hours;
  }

  public void setHours(int hours) {
    if (hours < 0)
      throw new IllegalArgumentException("hours cannot be negative");

    this.hours = hours;
  }

  public int getMinutes() {
    return minutes;
  }

  public void setMinutes(int minutes) {
    if (minutes < 0)
      throw new IllegalArgumentException("minutes cannot be negative");

    this.minutes = minutes;
  }

  public int getSeconds() {
    return seconds;
  }

  public void setSeconds(int seconds) {
    if (seconds < 0)
      throw new IllegalArgumentException("seconds cannot be negative");

    this.seconds = seconds;
  }

  public int getMillis() {
    return millis;
  }

  public void setMillis(int millis) {
    if (millis < 0)
      throw new IllegalArgumentException("milliseconds cannot be negative");

    this.millis = millis;
  }

  /**
   * Adds this duration to the given time instant.
   */
  public void addTo(Date dateTime) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(dateTime);
    addTo(calendar);
    dateTime.setTime(calendar.getTimeInMillis());
  }

  /**
   * Adds this duration to the given calendar.
   */
  public void addTo(Calendar calendar) {
    if (negative) {
      calendar.add(Calendar.YEAR, -years);
      calendar.add(Calendar.MONTH, -months);
      calendar.add(Calendar.DAY_OF_MONTH, -days);
      calendar.add(Calendar.HOUR_OF_DAY, -hours);
      calendar.add(Calendar.MINUTE, -minutes);
      calendar.add(Calendar.SECOND, -seconds);
      calendar.add(Calendar.MILLISECOND, -millis);
    }
    else {
      calendar.add(Calendar.YEAR, years);
      calendar.add(Calendar.MONTH, months);
      calendar.add(Calendar.DAY_OF_MONTH, days);
      calendar.add(Calendar.HOUR_OF_DAY, hours);
      calendar.add(Calendar.MINUTE, minutes);
      calendar.add(Calendar.SECOND, seconds);
      calendar.add(Calendar.MILLISECOND, millis);
    }
  }

  static final long SECOND = 1000;
  static final long MINUTE = 60 * SECOND;
  static final long HOUR = 60 * MINUTE;
  static final long DAY = 24 * HOUR;
  static final long YEAR = 365 * DAY + 5 * HOUR + 49 * MINUTE + 12 * SECOND;
  static final long MONTH = YEAR / 12;

  /**
   * Returns the length of this duration in milliseconds. Because the length of a month or a year
   * varies, this method assumes that a year is 365.2425 days long and a month is 1/12 of a year.
   * @return the number of milliseconds in this duration
   */
  public long getTimeInMillis() {
    long timeInMillis = millis;
    timeInMillis += SECOND * seconds;
    timeInMillis += MINUTE * minutes;
    timeInMillis += HOUR * hours;
    timeInMillis += DAY * days;
    timeInMillis += MONTH * months;
    timeInMillis += YEAR * years;
    return timeInMillis;
  }

  public boolean equals(Object other) {
    if (this == other)
      return true;
    if (!(other instanceof Duration))
      return false;
    final Duration that = (Duration) other;
    return years == that.years
        && months == that.months
        && days == that.days
        && hours == that.hours
        && minutes == that.minutes
        && seconds == that.seconds
        && millis == that.millis
        && negative == that.negative;
  }

  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + years;
    result = prime * result + months;
    result = prime * result + days;
    result = prime * result + hours;
    result = prime * result + minutes;
    result = prime * result + seconds;
    result = prime * result + millis;
    result = prime * result + (negative ? 1231 : 1237);
    return result;
  }

  public String toString() {
    StringBuffer literal = new StringBuffer();

    if (negative)
      literal.append('-');

    literal.append('P');

    if (years != 0)
      literal.append(years).append('Y');
    if (months != 0)
      literal.append(months).append('M');
    if (days != 0)
      literal.append(days).append('D');

    boolean hasHours = hours != 0;
    boolean hasMinutes = minutes != 0;
    boolean hasMillis = millis != 0;
    boolean hasSeconds = hasMillis || seconds != 0;

    if (hasHours || hasMinutes || hasSeconds) {
      literal.append('T');

      if (hasHours)
        literal.append(hours).append('H');
      if (hasMinutes)
        literal.append(minutes).append('M');
      if (hasSeconds) {
        literal.append(seconds);
        if (hasMillis) {
          literal.append('.');
          formatMillis(millis, literal);
        }
        literal.append('S');
      }
    }
    return literal.toString();
  }

  public static Duration valueOf(String literal) {
    Matcher matcher = durationPattern.matcher(literal);
    Duration duration = null;
    if (matcher.matches()) {
      duration = new Duration();
      // group 1: negative sign
      if (matcher.group(1) != null)
        duration.setNegative(true);
      // group 2: years
      String group = matcher.group(2);
      if (group != null)
        duration.setYears(Integer.parseInt(group));
      // group 3: months
      group = matcher.group(3);
      if (group != null)
        duration.setMonths(Integer.parseInt(group));
      // group 4: days
      group = matcher.group(4);
      if (group != null)
        duration.setDays(Integer.parseInt(group));
      // group 5: hours
      group = matcher.group(5);
      if (group != null)
        duration.setHours(Integer.parseInt(group));
      // group 6: minutes
      group = matcher.group(6);
      if (group != null)
        duration.setMinutes(Integer.parseInt(group));
      // group 7: seconds
      group = matcher.group(7);
      if (group != null)
        duration.setSeconds(Integer.parseInt(group));
      // group 8: milliseconds
      group = matcher.group(8);
      if (group != null)
        duration.setMillis(parseMillis(group));
    }
    return duration;
  }

  private static int parseMillis(String literal) {
    int number = Integer.parseInt(literal);
    assert number >= 0 : number;

    int digits = literal.length();
    assert digits < 4 : digits;

    // put trailing zeros
    if (digits < 3)
      number *= digits == 1 ? 100 : 10;

    return number;
  }

  private static void formatMillis(int number, StringBuffer buffer) {
    assert number >= 0 && number < 1000 : number;

    // put leading zeros to complete 3 decimal positions
    if (number < 100)
      buffer.append(number < 10 ? "00" : "0");

    // drop trailing zeros
    if (number % 10 == 0)
      number /= number % 100 == 0 ? 100 : 10;

    buffer.append(number);
  }
}
