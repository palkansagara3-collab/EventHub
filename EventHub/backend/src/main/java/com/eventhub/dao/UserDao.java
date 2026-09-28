package com.eventhub.dao;

import com.eventhub.DatabaseConnection;
import com.eventhub.model.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;

/** Registration and login validation against the `users` table. */
public class UserDao {

    /** SHA-256 hex hash. Swap for BCrypt in a real deployment. */
    public static String hash(String plain) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(plain.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    /** True when the e-mail is already taken. */
    public boolean emailExists(String email) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE email = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Registers a new customer/admin.
     * @return the populated User with its generated id, or null if the e-mail exists.
     */
    public User register(String fullName, String email, String phone,
                         String plainPassword, String role) throws SQLException {

        if (emailExists(email)) return null;

        String sql = "INSERT INTO users (full_name, email, phone, password_hash, role) "
                   + "VALUES (?, ?, ?, ?, ?)";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, fullName);
            ps.setString(2, email);
            ps.setString(3, phone);
            ps.setString(4, hash(plainPassword));
            ps.setString(5, (role == null || role.isBlank()) ? "CUSTOMER" : role.toUpperCase());

            if (ps.executeUpdate() == 0) return null;

            try (ResultSet keys = ps.getGeneratedKeys()) {
                int id = keys.next() ? keys.getInt(1) : 0;
                return new User(id, fullName, email, phone,
                                role == null ? "CUSTOMER" : role.toUpperCase());
            }
        }
    }

    /** Validates credentials. Returns the User on success, null on failure. */
    public User login(String email, String plainPassword) throws SQLException {
        String sql = "SELECT user_id, full_name, email, phone, role, is_active, created_at "
                   + "FROM users WHERE email = ? AND password_hash = ? AND is_active = 1";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, email);
            ps.setString(2, hash(plainPassword));

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /** Loads a single user by primary key. */
    public User findById(int userId) throws SQLException {
        String sql = "SELECT user_id, full_name, email, phone, role, is_active, created_at "
                   + "FROM users WHERE user_id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("user_id"));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setPhone(rs.getString("phone"));
        u.setRole(rs.getString("role"));
        u.setActive(rs.getBoolean("is_active"));
        u.setCreatedAt(rs.getTimestamp("created_at"));
        return u;
    }
}
