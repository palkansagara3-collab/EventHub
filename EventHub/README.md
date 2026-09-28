# EventHub — Online Event Ticket Booking System

Java (JDBC + DAO + Servlet) · MySQL · HTML/CSS/Vanilla JS

## 1. Database

```bash
mysql -u root -p < database/schema.sql
mysql -u root -p < database/sample-data.sql
```

Demo logins: `admin@eventhub.com / admin123` (admin), `riya@example.com / user123` (customer).

## 2. Backend

Credentials default to `root/root` on `localhost:3306/eventhub`; override with
`EVENTHUB_DB_URL`, `EVENTHUB_DB_USER`, `EVENTHUB_DB_PASSWORD`.

```bash
cd backend
mvn clean package
java -jar target/eventhub.jar     # connectivity check + sales report + event list
```

For the JSON API deploy the same project as a WAR on Tomcat 10+ (`/eventhub`),
exposing `/api/events`, `/api/login`, `/api/register`, `/api/book`, `/api/report`.

## 3. Frontend

Open `frontend/index.html` directly, or serve it:

```bash
cd frontend && python3 -m http.server 5500
```

`js/app.js` calls `API_BASE` (`http://localhost:8080/eventhub/api`). If the backend
is not running it falls back to a built-in demo dataset so the UI stays fully usable.

## Booking transaction

`BookingDao.bookTickets()` runs one transaction: `setAutoCommit(false)` → lock the
ticket tier `FOR UPDATE` → validate stock → decrement `available_seats` → insert
`bookings` (`BK-XXXX`) → insert one `digital_tickets` row per seat (`TKT-XXXX`) →
`commit()`; any failure triggers `rollback()`.
