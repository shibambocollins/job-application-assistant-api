package za.ac.cput.jobassistantapi.security;

import org.junit.jupiter.api.Test;
import za.ac.cput.jobassistantapi.exception.RateLimitExceededException;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RateLimiterServiceTest {

    private final RateLimiterService rateLimiter = new RateLimiterService();

    @Test
    void checkAllowed_underLimit_doesNotThrow() {
        String key = "test:" + System.nanoTime();
        for (int i = 0; i < 4; i++) {
            rateLimiter.checkAllowed(key, 5, Duration.ofHours(1));
            rateLimiter.recordAttempt(key, Duration.ofHours(1));
        }

        assertDoesNotThrow(() -> rateLimiter.checkAllowed(key, 5, Duration.ofHours(1)));
    }

    @Test
    void checkAllowed_atLimit_throwsRateLimitExceeded() {
        String key = "test:" + System.nanoTime();
        for (int i = 0; i < 5; i++) {
            rateLimiter.checkAllowed(key, 5, Duration.ofHours(1));
            rateLimiter.recordAttempt(key, Duration.ofHours(1));
        }

        assertThrows(RateLimitExceededException.class, () -> rateLimiter.checkAllowed(key, 5, Duration.ofHours(1)));
    }

    @Test
    void checkAllowed_windowExpired_resetsCount() {
        String key = "test:" + System.nanoTime();
        for (int i = 0; i < 5; i++) {
            rateLimiter.checkAllowed(key, 5, Duration.ofMillis(50));
            rateLimiter.recordAttempt(key, Duration.ofMillis(50));
        }

        assertThrows(RateLimitExceededException.class, () -> rateLimiter.checkAllowed(key, 5, Duration.ofMillis(50)));

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertDoesNotThrow(() -> rateLimiter.checkAllowed(key, 5, Duration.ofMillis(50)));
    }

    @Test
    void checkAllowed_differentKeys_areIndependent() {
        String keyA = "a:" + System.nanoTime();
        String keyB = "b:" + System.nanoTime();

        for (int i = 0; i < 5; i++) {
            rateLimiter.checkAllowed(keyA, 5, Duration.ofHours(1));
            rateLimiter.recordAttempt(keyA, Duration.ofHours(1));
        }

        assertDoesNotThrow(() -> rateLimiter.checkAllowed(keyB, 5, Duration.ofHours(1)));
    }
}
