package com.eventhub;

import com.eventhub.dao.EventDao;
import com.eventhub.dao.ReportDao;
import com.eventhub.model.Event;

import java.math.BigDecimal;
import java.util.List;

/**
 * Console entry point.
 *   mvn compile exec:java      (or)      java -jar target/eventhub.jar
 *
 * 1. verifies the MySQL connection
 * 2. prints the admin sales report
 * 3. lists every active upcoming event
 */
public class EventHubApp {

    public static void main(String[] args) {
        banner("EventHub - Online Event Ticket Booking System");

        // 1. connectivity -------------------------------------------------
        System.out.println("Checking database connection ...");
        if (!DatabaseConnection.testConnection()) {
            System.err.println("Could not reach the `eventhub` database.");
            System.err.println("Run database/schema.sql and database/sample-data.sql, then retry.");
            return;
        }
        System.out.println("Connected to MySQL `eventhub`.\n");

        try {
            // 2. admin sales report ---------------------------------------
            ReportDao reportDao = new ReportDao();
            banner("ADMIN SALES REPORT");
            System.out.printf("%-30s %-10s %9s %9s %14s%n",
                    "EVENT", "CATEGORY", "BOOKINGS", "TICKETS", "REVENUE");
            System.out.println("-".repeat(78));

            for (ReportDao.SalesRow r : reportDao.getSalesSummary()) {
                System.out.printf("%-30s %-10s %9d %9d %14s%n",
                        trim(r.eventTitle, 30), r.category, r.bookings, r.ticketsSold, r.revenue);
            }

            BigDecimal gross = reportDao.getGrossRevenue();
            System.out.println("-".repeat(78));
            System.out.printf("Customers: %d | Tickets sold: %d | GROSS REVENUE: Rs. %s%n%n",
                    reportDao.getCustomerCount(), reportDao.getTotalTicketsSold(), gross);

            // 3. active events --------------------------------------------
            banner("ACTIVE UPCOMING EVENTS");
            List<Event> events = new EventDao().findActiveUpcomingEvents();
            if (events.isEmpty()) {
                System.out.println("No upcoming events found.");
            } else {
                events.forEach(e -> System.out.println("  " + e));
            }
            System.out.println("\nDone.");

        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void banner(String text) {
        System.out.println("\n=== " + text + " ===");
    }

    private static String trim(String s, int max) {
        return s == null ? "" : (s.length() <= max ? s : s.substring(0, max - 1) + "…");
    }
}
