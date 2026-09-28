package com.eventhub.controller;

import com.eventhub.dao.BookingDao;
import com.eventhub.dao.EventDao;
import com.eventhub.dao.ReportDao;
import com.eventhub.dao.UserDao;
import com.eventhub.model.Event;
import com.eventhub.model.User;
import com.google.gson.Gson;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Thin JSON API in front of the DAO layer.
 *
 * Endpoints (deploy on Tomcat 10+, context path /eventhub):
 *   POST /api/register?name=&email=&phone=&password=
 *   POST /api/login?email=&password=
 *   GET  /api/events[?category=CONCERT]
 *   POST /api/book?userId=&eventId=&categoryId=&quantity=
 *   GET  /api/report
 */
@jakarta.servlet.annotation.WebServlet("/api/*")
public class EventHubController extends HttpServlet {

    private final UserDao    userDao    = new UserDao();
    private final EventDao   eventDao   = new EventDao();
    private final BookingDao bookingDao = new BookingDao();
    private final ReportDao  reportDao  = new ReportDao();
    private final Gson       gson       = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String path = req.getPathInfo() == null ? "/" : req.getPathInfo();
        try {
            switch (path) {
                case "/events" -> {
                    String cat = req.getParameter("category");
                    List<Event> events = (cat == null || cat.isBlank() || "ALL".equalsIgnoreCase(cat))
                            ? eventDao.findActiveUpcomingEvents()
                            : eventDao.findByCategory(cat);
                    write(res, 200, events);
                }
                case "/report" -> {
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("grossRevenue", reportDao.getGrossRevenue());
                    body.put("ticketsSold",  reportDao.getTotalTicketsSold());
                    body.put("customers",    reportDao.getCustomerCount());
                    body.put("rows",         reportDao.getSalesSummary());
                    write(res, 200, body);
                }
                default -> write(res, 404, Map.of("error", "Unknown endpoint " + path));
            }
        } catch (Exception e) {
            write(res, 500, Map.of("error", e.getMessage()));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String path = req.getPathInfo() == null ? "/" : req.getPathInfo();
        try {
            switch (path) {
                case "/register" -> {
                    User u = userDao.register(
                            req.getParameter("name"), req.getParameter("email"),
                            req.getParameter("phone"), req.getParameter("password"), "CUSTOMER");
                    if (u == null) write(res, 409, Map.of("error", "E-mail already registered"));
                    else           write(res, 201, u);
                }
                case "/login" -> {
                    User u = userDao.login(req.getParameter("email"), req.getParameter("password"));
                    if (u == null) write(res, 401, Map.of("error", "Invalid e-mail or password"));
                    else           write(res, 200, u);
                }
                case "/book" -> {
                    BookingDao.BookingResult r = bookingDao.bookTickets(
                            Integer.parseInt(req.getParameter("userId")),
                            Integer.parseInt(req.getParameter("eventId")),
                            Integer.parseInt(req.getParameter("categoryId")),
                            Integer.parseInt(req.getParameter("quantity")));
                    write(res, r.success ? 201 : 400, r);
                }
                default -> write(res, 404, Map.of("error", "Unknown endpoint " + path));
            }
        } catch (Exception e) {
            write(res, 500, Map.of("error", e.getMessage()));
        }
    }

    private void write(HttpServletResponse res, int status, Object body) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json;charset=UTF-8");
        res.setHeader("Access-Control-Allow-Origin", "*");
        try (PrintWriter out = res.getWriter()) {
            out.print(gson.toJson(body));
        }
    }
}
