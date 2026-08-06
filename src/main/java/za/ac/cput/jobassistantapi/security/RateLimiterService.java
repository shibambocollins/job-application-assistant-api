package za.ac.cput.jobassistantapi.security;

import org.springframework.stereotype.Service;
import za.ac.cput.jobassistantapi.exception.RateLimitExceededException;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Generic in-memory sliding-window rate limiter, keyed by caller-supplied string (e.g. "register:1.2.3.4"). */
@Service
public class RateLimiterService {

    private static class Window {
        int count;
        Instant windowStart;
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public synchronized void checkAllowed(String key, int maxAttempts, Duration window) {
        Window w = windows.get(key);
        if (w == null) return;

        Instant now = Instant.now();
        if (now.isAfter(w.windowStart.plus(window))) {
            windows.remove(key);
            return;
        }

        if (w.count >= maxAttempts) {
            long minutesLeft = Duration.between(now, w.windowStart.plus(window)).toMinutes() + 1;
            throw new RateLimitExceededException(
                    "Too many attempts. Try again in " + minutesLeft + " minute(s).");
        }
    }

    public synchronized void recordAttempt(String key, Duration window) {
        Instant now = Instant.now();
        Window w = windows.computeIfAbsent(key, k -> new Window());
        if (w.windowStart == null || now.isAfter(w.windowStart.plus(window))) {
            w.windowStart = now;
            w.count = 0;
        }
        w.count++;
    }
}
