package za.ac.cput.jobassistantapi.service.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.json.webtoken.JsonWebSignature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import za.ac.cput.jobassistantapi.dto.response.AuthResponse;
import za.ac.cput.jobassistantapi.exception.InvalidCredentialsException;
import za.ac.cput.jobassistantapi.model.User;
import za.ac.cput.jobassistantapi.repository.UserRepository;
import za.ac.cput.jobassistantapi.security.JwtService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleAuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private GoogleIdTokenVerifier verifier;

    @InjectMocks
    private GoogleAuthServiceImpl googleAuthService;

    private GoogleIdToken tokenWithPayload(String email, String name) {
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setEmail(email);
        payload.set("name", name);
        // GoogleIdToken.getPayload() is a final method, so it can't be Mockito-mocked — construct a
        // real instance instead. The signature/header bytes are irrelevant since verify() is never called.
        return new GoogleIdToken(new JsonWebSignature.Header(), payload, new byte[0], new byte[0]);
    }

    @Test
    void loginWithGoogle_invalidToken_throwsInvalidCredentials() throws Exception {
        when(verifier.verify("bad-token")).thenReturn(null);

        assertThrows(InvalidCredentialsException.class, () -> googleAuthService.loginWithGoogle("bad-token"));
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void loginWithGoogle_existingUser_returnsTokenWithoutCreatingUser() throws Exception {
        when(verifier.verify("good-token")).thenReturn(tokenWithPayload("existing@example.com", "Existing User"));

        User existing = new User.Builder().setId(1L).setEmail("existing@example.com").build();
        when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(existing));
        when(jwtService.generateToken("existing@example.com")).thenReturn("jwt-token");

        AuthResponse response = googleAuthService.loginWithGoogle("good-token");

        assertEquals("jwt-token", response.getToken());
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginWithGoogle_newUser_createsAccountWithRandomPassword() throws Exception {
        when(verifier.verify("good-token")).thenReturn(tokenWithPayload("new@example.com", "New Person"));

        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("random-hashed");
        User saved = new User.Builder().setId(2L).setEmail("new@example.com").setFullName("New Person").build();
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(jwtService.generateToken("new@example.com")).thenReturn("jwt-token-2");

        AuthResponse response = googleAuthService.loginWithGoogle("good-token");

        assertEquals("jwt-token-2", response.getToken());
        verify(userRepository).save(any(User.class));
    }
}
