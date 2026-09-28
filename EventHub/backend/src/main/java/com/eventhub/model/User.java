package com.eventhub.model;

import java.sql.Timestamp;

/** Plain data holder mapping one row of the `users` table. */
public class User {

    private int       userId;
    private String    fullName;
    private String    email;
    private String    phone;
    private String    role;        // ADMIN | CUSTOMER
    private boolean   active;
    private Timestamp createdAt;

    public User() { }

    public User(int userId, String fullName, String email, String phone, String role) {
        this.userId   = userId;
        this.fullName = fullName;
        this.email    = email;
        this.phone    = phone;
        this.role     = role;
        this.active   = true;
    }

    public int       getUserId()    { return userId; }
    public String    getFullName()  { return fullName; }
    public String    getEmail()     { return email; }
    public String    getPhone()     { return phone; }
    public String    getRole()      { return role; }
    public boolean   isActive()     { return active; }
    public Timestamp getCreatedAt() { return createdAt; }

    public void setUserId(int userId)          { this.userId = userId; }
    public void setFullName(String fullName)   { this.fullName = fullName; }
    public void setEmail(String email)         { this.email = email; }
    public void setPhone(String phone)         { this.phone = phone; }
    public void setRole(String role)           { this.role = role; }
    public void setActive(boolean active)      { this.active = active; }
    public void setCreatedAt(Timestamp t)      { this.createdAt = t; }

    public boolean isAdmin() { return "ADMIN".equalsIgnoreCase(role); }

    @Override
    public String toString() {
        return "User{" + userId + ", " + fullName + " <" + email + ">, " + role + "}";
    }
}
