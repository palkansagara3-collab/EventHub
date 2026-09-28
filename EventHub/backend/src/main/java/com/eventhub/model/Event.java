package com.eventhub.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Event row joined with its venue and aggregated ticket pricing/stock,
 * exactly as returned by EventDao#findActiveUpcomingEvents().
 */
public class Event {

    private int        eventId;
    private String     title;
    private String     description;
    private String     category;        // CONCERT | SPORTS | CULTURAL ...
    private Timestamp  eventDate;
    private String     status;

    // venue details (joined)
    private int        venueId;
    private String     venueName;
    private String     venueCity;

    // pricing / stock (aggregated from ticket_categories)
    private BigDecimal minPrice   = BigDecimal.ZERO;
    private BigDecimal maxPrice   = BigDecimal.ZERO;
    private int        seatsLeft;

    public Event() { }

    public int        getEventId()     { return eventId; }
    public String     getTitle()       { return title; }
    public String     getDescription() { return description; }
    public String     getCategory()    { return category; }
    public Timestamp  getEventDate()   { return eventDate; }
    public String     getStatus()      { return status; }
    public int        getVenueId()     { return venueId; }
    public String     getVenueName()   { return venueName; }
    public String     getVenueCity()   { return venueCity; }
    public BigDecimal getMinPrice()    { return minPrice; }
    public BigDecimal getMaxPrice()    { return maxPrice; }
    public int        getSeatsLeft()   { return seatsLeft; }

    public void setEventId(int v)         { this.eventId = v; }
    public void setTitle(String v)        { this.title = v; }
    public void setDescription(String v)  { this.description = v; }
    public void setCategory(String v)     { this.category = v; }
    public void setEventDate(Timestamp v) { this.eventDate = v; }
    public void setStatus(String v)       { this.status = v; }
    public void setVenueId(int v)         { this.venueId = v; }
    public void setVenueName(String v)    { this.venueName = v; }
    public void setVenueCity(String v)    { this.venueCity = v; }
    public void setMinPrice(BigDecimal v) { this.minPrice = v; }
    public void setMaxPrice(BigDecimal v) { this.maxPrice = v; }
    public void setSeatsLeft(int v)       { this.seatsLeft = v; }

    @Override
    public String toString() {
        return String.format("#%d %-28s %-9s %-14s %s  from Rs.%s  (%d left)",
                eventId, title, category, venueCity, eventDate, minPrice, seatsLeft);
    }
}
