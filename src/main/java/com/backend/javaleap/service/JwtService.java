package com.backend.javaleap.service;

import com.backend.javaleap.model.AppProperties;
import com.backend.javaleap.model.UserPrincipal;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Creates and validates signed JWT access tokens (HS256).
 * <p>
 * Token claims: sub = username, uid = user id, role, name.
 */
@Slf4j
@Service
public class JwtService {

    public static final String ISSUER = "padashakti";
    public static final String CLAIM_UID = "uid";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_NAME = "name";

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(AppProperties appProperties) {
        byte[] bytes = Decoders.BASE64.decode(appProperties.jwt().secret());
        if (bytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 256 bits (32 bytes) Base64");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expirationMs = appProperties.jwt().expirationMs();
    }

    /** Issues a token for the given user. */
    public String generateToken(UserPrincipal user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(ISSUER)
                .subject(user.getUsername())
                .claim(CLAIM_UID, user.getId())
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_NAME, user.getFullName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(key)
                .compact();
    }

    /** Parses and verifies a token. Empty if the token is invalid, tampered or expired. */
    public Optional<Claims> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(ISSUER)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Rejected JWT: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public Optional<String> extractUsername(String token) {
        return parse(token).map(Claims::getSubject);
    }

    public boolean isValid(String token) {
        return parse(token).isPresent();
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
