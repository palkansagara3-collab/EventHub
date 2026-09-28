-- ============================================================
-- EventHub - demo data
-- File: database/sample-data.sql   (run AFTER schema.sql)
-- Demo passwords are the SHA-256 hash of "admin123" / "user123"
-- ============================================================
USE eventhub;

-- ---------------------- USERS -------------------------------
INSERT INTO users (full_name, email, phone, password_hash, role) VALUES
('Admin Kumar',  'admin@eventhub.com', '9000000001',
 '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'ADMIN'),    -- admin123
('Riya Sharma',  'riya@example.com',   '9000000002',
 '0a041b9462caa4a31bac3567e0b6e6fd9100787db2ab433d96f6d178cabfce90', 'CUSTOMER'), -- user123
('Arjun Mehta',  'arjun@example.com',  '9000000003',
 '0a041b9462caa4a31bac3567e0b6e6fd9100787db2ab433d96f6d178cabfce90', 'CUSTOMER');

-- ---------------------- VENUES ------------------------------
INSERT INTO venues (name, address, city, state, capacity) VALUES
('Nehru Indoor Stadium', 'Park Road',        'Chennai',   'Tamil Nadu',  8000),
('Phoenix Arena',        'MG Road',          'Bengaluru', 'Karnataka',  12000),
('Heritage Open Grounds','Fort Street',      'Jaipur',    'Rajasthan',   5000);

-- ---------------------- EVENTS ------------------------------
INSERT INTO events (title, description, category, venue_id, event_date, banner_url, status, created_by) VALUES
('Midnight Echoes Live', 'An electrifying indie-rock night with the Midnight Echoes.',
 'CONCERT',  2, DATE_ADD(NOW(), INTERVAL 21 DAY), NULL, 'ACTIVE', 1),
('City Premier Cup Final', 'The season decider between the two top city clubs.',
 'SPORTS',   1, DATE_ADD(NOW(), INTERVAL 35 DAY), NULL, 'ACTIVE', 1),
('Rajasthan Folk Festival', 'Three stages of folk music, dance and craft.',
 'CULTURAL', 3, DATE_ADD(NOW(), INTERVAL 14 DAY), NULL, 'ACTIVE', 1),
('Classical Evening',      'Carnatic ensemble performance.',
 'CULTURAL', 1, DATE_ADD(NOW(), INTERVAL 48 DAY), NULL, 'ACTIVE', 1);

-- ------------------ TICKET CATEGORIES -----------------------
INSERT INTO ticket_categories (event_id, category_name, price, total_seats, available_seats) VALUES
(1, 'VIP',    4500.00,  200,  200),
(1, 'GOLD',   2500.00,  800,  780),
(1, 'SILVER', 1200.00, 2000, 1950),
(2, 'VIP',    3000.00,  300,  290),
(2, 'GENERAL', 800.00, 4000, 3850),
(3, 'GOLD',   1500.00,  500,  500),
(3, 'GENERAL', 600.00, 2500, 2400),
(4, 'GOLD',   1800.00,  250,  250),
(4, 'SILVER',  900.00, 1000,  990);

-- ------------------ DEMO BOOKINGS ---------------------------
INSERT INTO bookings (booking_number, user_id, event_id, category_id, quantity, unit_price, total_amount) VALUES
('BK-1001', 2, 1, 2, 2, 2500.00, 5000.00),
('BK-1002', 3, 2, 5, 4,  800.00, 3200.00);

INSERT INTO digital_tickets (ticket_number, booking_id, seat_label, qr_payload) VALUES
('TKT-1001', 1, 'G-12', 'BK-1001|1'),
('TKT-1002', 1, 'G-13', 'BK-1001|2'),
('TKT-1003', 2, 'GA',   'BK-1002|1'),
('TKT-1004', 2, 'GA',   'BK-1002|2'),
('TKT-1005', 2, 'GA',   'BK-1002|3'),
('TKT-1006', 2, 'GA',   'BK-1002|4');
