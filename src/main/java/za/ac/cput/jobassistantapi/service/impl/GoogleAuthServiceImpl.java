package za.ac.cput.jobassistantapi.service.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import za.ac.cput.jobassistantapi.dto.response.AuthResponse;
import za.ac.cput.jobassistantapi.exception.InvalidCredentialsException;
import za.ac.cput.jobassistantapi.model.User;
import za.ac.cput.jobassistantapi.repository.UserRepository;
import za.ac.cput.jobassistantapi.security.JwtService;
import za.ac.cput.jobassistantapi.service.GoogleAuthService;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.UUID;

@Service
public class GoogleAuthServiceImpl implements GoogleAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleIdTokenVerifier verifier;

    public GoogleAuthServiceImpl(UserRepository userRepository,
                                  PasswordEncoder passwordEncoder,
                                  JwtService jwtService,
                                  @Value("${google.oauth.client-id:}") String googleClientId) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(googleClientId))
                .build();
    }

    @Override
    public AuthResponse loginWithGoogle(String idToken) {
        GoogleIdToken.Payload payload;
        try {
            GoogleIdToken token = verifier.verify(idToken);
            if (token == null) {
                throw new InvalidCredentialsException("Invalid Google sign-in token");
            }
            payload = token.getPayload();
        } catch (GeneralSecurityException | IOException e) {
            throw new InvalidCredentialsException("Could not verify Google sign-in token");
        }

        String email = payload.getEmail();
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = new User.Builder()
                            .setEmail(email)
                            .setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                            .setFullName((String) payload.get("name"))
                            .build();
                    return userRepository.save(newUser);
                });

        String jwt = jwtService.generateToken(user.getEmail());
        return new AuthResponse("LOGIN_SUCCESSFUL", jwt);
    }
}
