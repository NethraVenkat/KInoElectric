package com.backend.javaleap.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Brute-force protection: after {@value #MAX_ATTEMPTS} failed logins a username is
 * locked for {@link #LOCK_DURATION}. In-memory (resets on restart) - enough for one instance.
 */
@Service
public class LoginAttemptService {

    public static final int MAX_ATTEMPTS = 5;
    public static final Duration LOCK_DURATION = Duration.ofMinutes(5);

    private record Attempts(int count, Instant lockedUntil) { }

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();

    public boolean isLocked(String username) {
        Attempts a = attempts.get(key(username));
        if (a == null || a.lockedUntil() == null) {
            return false;
        }
        if (Instant.now().isAfter(a.lockedUntil())) {
            attempts.remove(key(username));
            return false;
        }
        return true;
    }

    public void loginFailed(String username) {
        attempts.compute(key(username), (k, a) -> {
            int count = (a == null ? 0 : a.count()) + 1;
            Instant lock = count >= MAX_ATTEMPTS ? Instant.now().plus(LOCK_DURATION) : null;
            return new Attempts(count, lock);
        });
    }

    public void loginSucceeded(String username) {
        attempts.remove(key(username));
    }

    public int remainingAttempts(String username) {
        Attempts a = attempts.get(key(username));
        return Math.max(0, MAX_ATTEMPTS - (a == null ? 0 : a.count()));
    }

    private static String key(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
