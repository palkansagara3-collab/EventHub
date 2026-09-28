-- ============================================================
-- EventHub - Online Event Ticket Booking System
-- File: database/schema.sql
-- Engine: MySQL 8.x  |  Database: eventhub
-- ============================================================

DROP DATABASE IF EXISTS eventhub;
CREATE DATABASE eventhub CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE eventhub;

-- ------------------------------------------------------------
-- 1. users : admins and customers
-- ------------------------------------------------------------
CREATE TABLE users (
    user_id       INT AUTO_INCREMENT PRIMARY KEY,
    full_name     VARCHAR(120)  NOT NULL,
    email         VARCHAR(160)  NOT NULL UNIQUE,
    phone         VARCHAR(20),
    password_hash VARCHAR(255)  NOT NULL,           -- store a hash, never plain text
    role          ENUM('ADMIN','CUSTOMER') NOT NULL DEFAULT 'CUSTOMER',
    is_active     TINYINT(1)    NOT NULL DEFAULT 1,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role  ON users(role);

-- ------------------------------------------------------------
-- 2. venues : physical locations hosting events
-- ------------------------------------------------------------
CREATE TABLE venues (
    venue_id   INT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(150) NOT NULL,
    address    VARCHAR(255),
    city       VARCHAR(80)  NOT NULL,
    state      VARCHAR(80),
    capacity   INT          NOT NULL CHECK (capacity > 0),
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE INDEX idx_venues_city ON venues(city);

-- ------------------------------------------------------------
-- 3. events : concerts, sports, cultural shows ...
-- ------------------------------------------------------------
CREATE TABLE events (
    event_id    INT AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(180) NOT NULL,
    description TEXT,
    category    ENUM('CONCERT','SPORTS','CULTURAL','THEATRE','CONFERENCE') NOT NULL,
    venue_id    INT          NOT NULL,
    event_date  DATETIME     NOT NULL,
    banner_url  VARCHAR(400),
    status      ENUM('ACTIVE','CANCELLED','COMPLETED') NOT NULL DEFAULT 'ACTIVE',
    created_by  INT,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_events_venue
        FOREIGN KEY (venue_id)   REFERENCES venues(venue_id) ON DELETE CASCADE,
    CONSTRAINT fk_events_creator
        FOREIGN KEY (created_by) REFERENCES users(user_id)   ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE INDEX idx_events_date     ON events(event_date);
CREATE INDEX idx_events_status   ON events(status);
CREATE INDEX idx_events_category ON events(category);

-- ------------------------------------------------------------
-- 4. ticket_categories : price tiers + live stock per event
-- ------------------------------------------------------------
CREATE TABLE ticket_categories (
    category_id     INT AUTO_INCREMENT PRIMARY KEY,
    event_id        INT            NOT NULL,
    category_name   VARCHAR(60)    NOT NULL,        -- VIP / GOLD / SILVER ...
    price           DECIMAL(10,2)  NOT NULL CHECK (price >= 0),
    total_seats     INT            NOT NULL CHECK (total_seats >= 0),
    available_seats INT            NOT NULL CHECK (available_seats >= 0),
    CONSTRAINT fk_tc_event FOREIGN KEY (event_id) REFERENCES events(event_id) ON DELETE CASCADE,
    CONSTRAINT uq_tc_event_name UNIQUE (event_id, category_name)
) ENGINE=InnoDB;

CREATE INDEX idx_tc_event ON ticket_categories(event_id);

-- ------------------------------------------------------------
-- 5. bookings : one row per purchase transaction
-- ------------------------------------------------------------
CREATE TABLE bookings (
    booking_id     INT AUTO_INCREMENT PRIMARY KEY,
    booking_number VARCHAR(20)   NOT NULL UNIQUE,   -- BK-XXXX
    user_id        INT           NOT NULL,
    event_id       INT           NOT NULL,
    category_id    INT           NOT NULL,
    quantity       INT           NOT NULL CHECK (quantity > 0),
    unit_price     DECIMAL(10,2) NOT NULL,
    total_amount   DECIMAL(12,2) NOT NULL,
    status         ENUM('CONFIRMED','CANCELLED') NOT NULL DEFAULT 'CONFIRMED',
    booked_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_book_user  FOREIGN KEY (user_id)     REFERENCES users(user_id)                ON DELETE CASCADE,
    CONSTRAINT fk_book_event FOREIGN KEY (event_id)    REFERENCES events(event_id)              ON DELETE CASCADE,
    CONSTRAINT fk_book_cat   FOREIGN KEY (category_id) REFERENCES ticket_categories(category_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_bookings_user   ON bookings(user_id);
CREATE INDEX idx_bookings_event  ON bookings(event_id);
CREATE INDEX idx_bookings_status ON bookings(status);

-- ------------------------------------------------------------
-- 6. digital_tickets : one scannable ticket per seat
-- ------------------------------------------------------------
CREATE TABLE digital_tickets (
    ticket_id     INT AUTO_INCREMENT PRIMARY KEY,
    ticket_number VARCHAR(24) NOT NULL UNIQUE,      -- TKT-XXXX
    booking_id    INT         NOT NULL,
    seat_label    VARCHAR(20),
    qr_payload    VARCHAR(255),
    is_used       TINYINT(1)  NOT NULL DEFAULT 0,
    issued_at     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ticket_booking FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_tickets_booking ON digital_tickets(booking_id);
