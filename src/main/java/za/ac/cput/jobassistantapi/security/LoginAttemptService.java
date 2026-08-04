package za.ac.cput.jobassistantapi.security;

import org.springframework.stereotype.Service;
import za.ac.cput.jobassistantapi.exception.RateLimitExceededException;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory login throttling — locks an email out after repeated failed attempts. Resets on app restart. */
@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private static class Attempt {
        int count;
        Instant lockedUntil;
    }

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    public synchronized void checkAllowed(String email) {
        Attempt attempt = attempts.get(key(email));
        if (attempt == null || attempt.lockedUntil == null) {
            return;
        }
        if (Instant.now().isBefore(attempt.lockedUntil)) {
            long minutesLeft = Duration.between(Instant.now(), attempt.lockedUntil).toMinutes() + 1;
            throw new RateLimitExceededException(
                    "Too many failed login attempts. Try again in " + minutesLeft + " minute(s).");
        }
        attempts.remove(key(email));
    }

    public synchronized void recordFailure(String email) {
        Attempt attempt = attempts.computeIfAbsent(key(email), k -> new Attempt());
        attempt.count++;
        if (attempt.count >= MAX_ATTEMPTS) {
            attempt.lockedUntil = Instant.now().plus(LOCK_DURATION);
        }
    }

    public synchronized void recordSuccess(String email) {
        attempts.remove(key(email));
    }

    private String key(String email) {
        return email == null ? "" : email.toLowerCase();
    }
}
