package za.ac.cput.jobassistantapi.service;

import za.ac.cput.jobassistantapi.dto.request.ChangePasswordRequest;

public interface AccountService {

    void changePassword(String email, ChangePasswordRequest request);

    void deleteAccount(String email);
}
