package za.ac.cput.jobassistantapi.service;

import za.ac.cput.jobassistantapi.dto.response.AuthResponse;

public interface GoogleAuthService {

    AuthResponse loginWithGoogle(String idToken);
}
