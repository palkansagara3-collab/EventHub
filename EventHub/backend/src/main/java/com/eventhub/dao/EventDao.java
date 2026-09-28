package com.eventhub.dao;

import com.eventhub.DatabaseConnection;
import com.eventhub.model.Event;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Read access to active, future events with venue details and price range. */
public class EventDao {

    private static final String BASE_SQL =
        "SELECT e.event_id, e.title, e.description, e.category, e.event_date, e.status, " +
        "       v.venue_id, v.name AS venue_name, v.city AS venue_city, " +
        "       COALESCE(MIN(tc.price), 0)            AS min_price, " +
        "       COALESCE(MAX(tc.price), 0)            AS max_price, " +
        "       COALESCE(SUM(tc.available_seats), 0)  AS seats_left " +
        "FROM events e " +
        "JOIN venues v            ON v.venue_id = e.venue_id " +
        "LEFT JOIN ticket_categories tc ON tc.event_id = e.event_id " +
        "WHERE e.status = 'ACTIVE' AND e.event_date > NOW() ";

    /** All active upcoming events, soonest first. */
    public List<Event> findActiveUpcomingEvents() throws SQLException {
        String sql = BASE_SQL + "GROUP BY e.event_id, v.venue_id ORDER BY e.event_date ASC";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<Event> events = new ArrayList<>();
            while (rs.next()) events.add(mapRow(rs));
            return events;
        }
    }

    /** Same list filtered by category (CONCERT / SPORTS / CULTURAL ...). */
    public List<Event> findByCategory(String category) throws SQLException {
        String sql = BASE_SQL + "AND e.category = ? GROUP BY e.event_id, v.venue_id ORDER BY e.event_date ASC";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, category.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                List<Event> events = new ArrayList<>();
                while (rs.next()) events.add(mapRow(rs));
                return events;
            }
        }
    }

    /** Single event by id (regardless of date), or null. */
    public Event findById(int eventId) throws SQLException {
        String sql =
            "SELECT e.event_id, e.title, e.description, e.category, e.event_date, e.status, " +
            "       v.venue_id, v.name AS venue_name, v.city AS venue_city, " +
            "       COALESCE(MIN(tc.price),0) AS min_price, COALESCE(MAX(tc.price),0) AS max_price, " +
            "       COALESCE(SUM(tc.available_seats),0) AS seats_left " +
            "FROM events e JOIN venues v ON v.venue_id = e.venue_id " +
            "LEFT JOIN ticket_categories tc ON tc.event_id = e.event_id " +
            "WHERE e.event_id = ? GROUP BY e.event_id, v.venue_id";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /** Ticket tiers for one event: [category_id, name, price, available_seats]. */
    public List<Object[]> findTicketCategories(int eventId) throws SQLException {
        String sql = "SELECT category_id, category_name, price, available_seats " +
                     "FROM ticket_categories WHERE event_id = ? ORDER BY price DESC";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Object[]> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(new Object[]{
                        rs.getInt("category_id"),
                        rs.getString("category_name"),
                        rs.getBigDecimal("price"),
                        rs.getInt("available_seats")
                    });
                }
                return rows;
            }
        }
    }

    private Event mapRow(ResultSet rs) throws SQLException {
        Event e = new Event();
        e.setEventId(rs.getInt("event_id"));
        e.setTitle(rs.getString("title"));
        e.setDescription(rs.getString("description"));
        e.setCategory(rs.getString("category"));
        e.setEventDate(rs.getTimestamp("event_date"));
        e.setStatus(rs.getString("status"));
        e.setVenueId(rs.getInt("venue_id"));
        e.setVenueName(rs.getString("venue_name"));
        e.setVenueCity(rs.getString("venue_city"));
        e.setMinPrice(rs.getBigDecimal("min_price") == null ? BigDecimal.ZERO : rs.getBigDecimal("min_price"));
        e.setMaxPrice(rs.getBigDecimal("max_price") == null ? BigDecimal.ZERO : rs.getBigDecimal("max_price"));
        e.setSeatsLeft(rs.getInt("seats_left"));
        return e;
    }
}
