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
package org.jbpm.bpel.tutorial.trip;

import java.rmi.RemoteException;
import java.util.Calendar;

import javax.naming.InitialContext;

import junit.framework.Test;
import junit.framework.TestCase;

import org.jbpm.bpel.tools.ModuleDeployTestSetup;

/**
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/11/06 22:06:28 $
 */
public class TravelAgentTest extends TestCase {

  private TravelAgent agent;

  private Flight flight = new Flight();
  private Hotel hotel = new Hotel();
  private RentalCar car = new RentalCar();
  private Calendar tripDate = Calendar.getInstance();

  protected void setUp() throws Exception {
    /*
     * "service/Trip" is the JNDI name of the service interface instance relative to the client
     * environment context. This name matches the <service-ref-name> in application-client.xml
     */
    InitialContext iniCtx = new InitialContext();
    TripReservationService tripService = (TripReservationService) iniCtx.lookup("java:comp/env/service/Trip");
    agent = tripService.getAgentPort();

    flight.setAirline("AM");
    flight.setNumber(637);
    hotel.setName("Maria Isabel");
    car.setCompany("Alamo");
    tripDate.add(Calendar.SECOND, 10);
  }

  public void testPurchaseTrip() throws RemoteException {
    ItemSet items = new ItemSet();
    items.setFlight(flight); // cost: 300
    items.setHotel(hotel); // cost: 100

    Order order = new Order();
    order.setDate(tripDate);
    order.setItems(items);

    Invoice invoice = agent.purchaseTrip(order);

    assertEquals(300 + 100, invoice.getCost(), 0);
  }

  public void testGetTripDetails() throws RemoteException {
    ItemSet items = new ItemSet();
    items.setFlight(flight);
    items.setHotel(hotel);
    items.setRentalCar(car);

    Order order = new Order();
    order.setDate(tripDate);
    order.setItems(items);

    Invoice invoice = agent.purchaseTrip(order);

    Query query = new Query();
    query.setLocator(invoice.getLocator());

    Detail detail = agent.getTripDetail(query);
    items = detail.getItems();

    assertEquals(flight.getAirline(), items.getFlight().getAirline());
    assertEquals(flight.getNumber(), items.getFlight().getNumber());
    assertEquals(hotel.getName(), items.getHotel().getName());
    assertEquals(car.getCompany(), items.getRentalCar().getCompany());
    assertEquals(invoice.getCost(), detail.getCost(), 0);
  }

  public void testCancelTrip() throws Exception {
    ItemSet items = new ItemSet();
    items.setFlight(flight); // fee: 100
    items.setRentalCar(car); // fee: 5

    Order order = new Order();
    order.setDate(tripDate);
    order.setItems(items);

    Invoice invoice = agent.purchaseTrip(order);

    Cancelation reference = new Cancelation();
    reference.setLocator(invoice.getLocator());

    Penalty penalty = agent.cancelTrip(reference);

    assertEquals(100 + 5, penalty.getFee(), 0);
  }

  public static Test suite() {
    return new ModuleDeployTestSetup(TravelAgentTest.class, "trip-client.jar");
  }
}
