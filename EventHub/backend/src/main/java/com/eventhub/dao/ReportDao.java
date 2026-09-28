package com.eventhub.dao;

import com.eventhub.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Admin analytics: per-event sales and gross revenue. */
public class ReportDao {

    /** One line of the admin sales report. */
    public static class SalesRow {
        public final String     eventTitle;
        public final String     category;
        public final int        bookings;
        public final int        ticketsSold;
        public final BigDecimal revenue;

        SalesRow(String eventTitle, String category, int bookings, int ticketsSold, BigDecimal revenue) {
            this.eventTitle  = eventTitle;
            this.category    = category;
            this.bookings    = bookings;
            this.ticketsSold = ticketsSold;
            this.revenue     = revenue;
        }
    }

    /** Sales grouped per event, highest revenue first. Cancelled bookings excluded. */
    public List<SalesRow> getSalesSummary() throws SQLException {
        String sql =
            "SELECT e.title, e.category, " +
            "       COUNT(b.booking_id)               AS bookings, " +
            "       COALESCE(SUM(b.quantity), 0)      AS tickets_sold, " +
            "       COALESCE(SUM(b.total_amount), 0)  AS revenue " +
            "FROM events e " +
            "LEFT JOIN bookings b ON b.event_id = e.event_id AND b.status = 'CONFIRMED' " +
            "GROUP BY e.event_id, e.title, e.category " +
            "ORDER BY revenue DESC";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<SalesRow> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(new SalesRow(
                        rs.getString("title"),
                        rs.getString("category"),
                        rs.getInt("bookings"),
                        rs.getInt("tickets_sold"),
                        rs.getBigDecimal("revenue")));
            }
            return rows;
        }
    }

    /** Total confirmed revenue across the platform. */
    public BigDecimal getGrossRevenue() throws SQLException {
        String sql = "SELECT COALESCE(SUM(total_amount), 0) AS gross FROM bookings WHERE status = 'CONFIRMED'";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getBigDecimal("gross") : BigDecimal.ZERO;
        }
    }

    /** Total confirmed tickets issued. */
    public int getTotalTicketsSold() throws SQLException {
        String sql = "SELECT COALESCE(SUM(quantity), 0) AS total FROM bookings WHERE status = 'CONFIRMED'";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt("total") : 0;
        }
    }

    /** Registered customer count. */
    public int getCustomerCount() throws SQLException {
        String sql = "SELECT COUNT(*) AS c FROM users WHERE role = 'CUSTOMER'";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt("c") : 0;
        }
    }
}
