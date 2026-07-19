package com.lcs.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, length = 1024)
    private String token;

    @Column(nullable = false)
    private Instant expiryDate;

    protected RefreshToken() {
    }

    public RefreshToken(String email, String token, Instant expiryDate) {
        this.email = email;
        this.token = token;
        this.expiryDate = expiryDate;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getToken() {
        return token;
    }

    public Instant getExpiryDate() {
        return expiryDate;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiryDate);
    }
}
