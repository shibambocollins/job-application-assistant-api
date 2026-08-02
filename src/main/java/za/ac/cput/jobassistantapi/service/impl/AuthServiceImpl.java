package za.ac.cput.jobassistantapi.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import za.ac.cput.jobassistantapi.dto.request.LoginRequest;
import za.ac.cput.jobassistantapi.dto.request.RegisterRequest;
import za.ac.cput.jobassistantapi.dto.response.AuthResponse;
import za.ac.cput.jobassistantapi.exception.DuplicateResourceException;
import za.ac.cput.jobassistantapi.exception.InvalidCredentialsException;
import za.ac.cput.jobassistantapi.exception.ResourceNotFoundException;
import za.ac.cput.jobassistantapi.model.User;
import za.ac.cput.jobassistantapi.repository.UserRepository;
import za.ac.cput.jobassistantapi.security.JwtService;
import za.ac.cput.jobassistantapi.service.AuthService;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }

        User user = new User.Builder()
                .setEmail(request.getEmail())
                .setPasswordHash(passwordEncoder.encode(request.getPassword()))
                .setFullName(request.getFullName())
                .build();

        userRepository.save(user);

        String token = jwtService.generateToken(user.getEmail());

        return new AuthResponse("REGISTERED_SUCCESSFULLY", token);
    }

    @Override
    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid password");
        }

        String token = jwtService.generateToken(user.getEmail());

        return new AuthResponse("LOGIN_SUCCESSFUL", token);
    }
}