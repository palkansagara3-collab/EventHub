package com.eventhub.dao;

import com.eventhub.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Transaction-safe ticket booking.
 *
 * The whole purchase runs in ONE transaction:
 *   1. lock the ticket category row (SELECT ... FOR UPDATE)
 *   2. verify enough stock
 *   3. decrement available_seats
 *   4. insert the booking  -> BK-XXXX
 *   5. insert one digital ticket per seat -> TKT-XXXX
 * Any failure rolls the entire purchase back.
 */
public class BookingDao {

    /** Result of a booking attempt. */
    public static class BookingResult {
        public final boolean    success;
        public final String     message;
        public final String     bookingNumber;
        public final BigDecimal totalAmount;
        public final List<String> ticketNumbers;

        BookingResult(boolean success, String message, String bookingNumber,
                      BigDecimal totalAmount, List<String> ticketNumbers) {
            this.success       = success;
            this.message       = message;
            this.bookingNumber = bookingNumber;
            this.totalAmount   = totalAmount;
            this.ticketNumbers = ticketNumbers;
        }
        static BookingResult fail(String msg) {
            return new BookingResult(false, msg, null, BigDecimal.ZERO, List.of());
        }
    }

    public BookingResult bookTickets(int userId, int eventId, int categoryId, int quantity) {

        if (quantity <= 0) return BookingResult.fail("Quantity must be at least 1.");

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);                       // ---- BEGIN TRANSACTION ----

            // 1 + 2. Lock the price tier and read live stock.
            BigDecimal unitPrice;
            int available;
            String lockSql = "SELECT price, available_seats FROM ticket_categories " +
                             "WHERE category_id = ? AND event_id = ? FOR UPDATE";
            try (PreparedStatement ps = conn.prepareStatement(lockSql)) {
                ps.setInt(1, categoryId);
                ps.setInt(2, eventId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return BookingResult.fail("Ticket category not found for this event.");
                    }
                    unitPrice = rs.getBigDecimal("price");
                    available = rs.getInt("available_seats");
                }
            }

            if (available < quantity) {
                conn.rollback();
                return BookingResult.fail("Only " + available + " seat(s) left in this category.");
            }

            // 3. Decrement stock (guard clause repeats the check atomically).
            String decSql = "UPDATE ticket_categories SET available_seats = available_seats - ? " +
                            "WHERE category_id = ? AND available_seats >= ?";
            try (PreparedStatement ps = conn.prepareStatement(decSql)) {
                ps.setInt(1, quantity);
                ps.setInt(2, categoryId);
                ps.setInt(3, quantity);
                if (ps.executeUpdate() != 1) {
                    conn.rollback();
                    return BookingResult.fail("Seats were just taken. Please try again.");
                }
            }

            // 4. Insert the booking.
            BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(quantity));
            int bookingId;
            String insBooking = "INSERT INTO bookings " +
                "(booking_number, user_id, event_id, category_id, quantity, unit_price, total_amount) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
            String bookingNumber = nextBookingNumber(conn);

            try (PreparedStatement ps = conn.prepareStatement(insBooking, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, bookingNumber);
                ps.setInt(2, userId);
                ps.setInt(3, eventId);
                ps.setInt(4, categoryId);
                ps.setInt(5, quantity);
                ps.setBigDecimal(6, unitPrice);
                ps.setBigDecimal(7, total);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) { conn.rollback(); return BookingResult.fail("Could not create booking."); }
                    bookingId = keys.getInt(1);
                }
            }

            // 5. Issue one digital ticket per seat.
            List<String> ticketNumbers = new ArrayList<>();
            int ticketSeq = nextTicketSeq(conn);
            String insTicket = "INSERT INTO digital_tickets (ticket_number, booking_id, seat_label, qr_payload) " +
                               "VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insTicket)) {
                for (int i = 1; i <= quantity; i++) {
                    String tkt = String.format("TKT-%04d", ticketSeq++);
                    ticketNumbers.add(tkt);
                    ps.setString(1, tkt);
                    ps.setInt(2, bookingId);
                    ps.setString(3, "S-" + i);
                    ps.setString(4, bookingNumber + "|" + i);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            conn.commit();                                   // ---- COMMIT ----
            return new BookingResult(true,
                    "Booking confirmed for " + quantity + " ticket(s).",
                    bookingNumber, total, ticketNumbers);

        } catch (SQLException e) {
            rollbackQuietly(conn);
            return BookingResult.fail("Booking failed: " + e.getMessage());
        } finally {
            closeQuietly(conn);
        }
    }

    /** Booking history for one customer. */
    public List<String> findBookingsByUser(int userId) throws SQLException {
        String sql = "SELECT b.booking_number, e.title, tc.category_name, b.quantity, b.total_amount, b.status " +
                     "FROM bookings b " +
                     "JOIN events e             ON e.event_id = b.event_id " +
                     "JOIN ticket_categories tc ON tc.category_id = b.category_id " +
                     "WHERE b.user_id = ? ORDER BY b.booked_at DESC";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<String> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(String.format("%s | %s | %s x%d | Rs.%s | %s",
                            rs.getString("booking_number"), rs.getString("title"),
                            rs.getString("category_name"), rs.getInt("quantity"),
                            rs.getBigDecimal("total_amount"), rs.getString("status")));
                }
                return rows;
            }
        }
    }

    // ---------------- helpers ----------------

    private String nextBookingNumber(Connection conn) throws SQLException {
        String sql = "SELECT COALESCE(MAX(booking_id), 1000) + 1 AS nxt FROM bookings";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            int n = rs.next() ? rs.getInt("nxt") : 1001;
            return String.format("BK-%04d", n);
        }
    }

    private int nextTicketSeq(Connection conn) throws SQLException {
        String sql = "SELECT COALESCE(MAX(ticket_id), 1000) + 1 AS nxt FROM digital_tickets";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt("nxt") : 1001;
        }
    }

    private void rollbackQuietly(Connection c) {
        if (c != null) try { c.rollback(); } catch (SQLException ignored) { }
    }

    private void closeQuietly(Connection c) {
        if (c != null) try { c.setAutoCommit(true); c.close(); } catch (SQLException ignored) { }
    }
}
