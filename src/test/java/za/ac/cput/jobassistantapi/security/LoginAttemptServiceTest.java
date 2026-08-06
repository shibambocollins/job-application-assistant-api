package za.ac.cput.jobassistantapi.security;

import org.junit.jupiter.api.Test;
import za.ac.cput.jobassistantapi.exception.RateLimitExceededException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginAttemptServiceTest {

    private final LoginAttemptService loginAttemptService = new LoginAttemptService();

    @Test
    void checkAllowed_noFailures_doesNotThrow() {
        assertDoesNotThrow(() -> loginAttemptService.checkAllowed("fresh@example.com"));
    }

    @Test
    void checkAllowed_belowThreshold_doesNotLockOut() {
        String email = "below@example.com";
        for (int i = 0; i < 4; i++) {
            loginAttemptService.recordFailure(email);
        }

        assertDoesNotThrow(() -> loginAttemptService.checkAllowed(email));
    }

    @Test
    void checkAllowed_atThreshold_locksOut() {
        String email = "locked@example.com";
        for (int i = 0; i < 5; i++) {
            loginAttemptService.recordFailure(email);
        }

        assertThrows(RateLimitExceededException.class, () -> loginAttemptService.checkAllowed(email));
    }

    @Test
    void recordSuccess_clearsFailureCount() {
        String email = "recovered@example.com";
        for (int i = 0; i < 4; i++) {
            loginAttemptService.recordFailure(email);
        }
        loginAttemptService.recordSuccess(email);

        for (int i = 0; i < 4; i++) {
            loginAttemptService.recordFailure(email);
        }

        assertDoesNotThrow(() -> loginAttemptService.checkAllowed(email));
    }

    @Test
    void checkAllowed_isCaseInsensitiveOnEmail() {
        String email = "MixedCase@example.com";
        for (int i = 0; i < 5; i++) {
            loginAttemptService.recordFailure(email.toLowerCase());
        }

        assertThrows(RateLimitExceededException.class, () -> loginAttemptService.checkAllowed(email));
    }
}
