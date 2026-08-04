package za.ac.cput.jobassistantapi.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used;

    private LocalDateTime createdAt;

    protected PasswordResetToken() {}

    private PasswordResetToken(Builder builder) {
        this.id = builder.id;
        this.userId = builder.userId;
        this.token = builder.token;
        this.expiresAt = builder.expiresAt;
        this.used = builder.used;
        this.createdAt = builder.createdAt;
    }

    @PrePersist
    public void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getToken() {
        return token;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public boolean isUsed() {
        return used;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public static class Builder {
        private Long id;
        private Long userId;
        private String token;
        private LocalDateTime expiresAt;
        private boolean used;
        private LocalDateTime createdAt;

        public Builder setId(Long id) {
            this.id = id;
            return this;
        }

        public Builder setUserId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder setToken(String token) {
            this.token = token;
            return this;
        }

        public Builder setExpiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public Builder setUsed(boolean used) {
            this.used = used;
            return this;
        }

        public Builder setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder copy(PasswordResetToken source) {
            this.id = source.id;
            this.userId = source.userId;
            this.token = source.token;
            this.expiresAt = source.expiresAt;
            this.used = source.used;
            this.createdAt = source.createdAt;
            return this;
        }

        public PasswordResetToken build() {
            return new PasswordResetToken(this);
        }
    }
}
