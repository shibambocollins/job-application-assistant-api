package za.ac.cput.jobassistantapi.service;

import za.ac.cput.jobassistantapi.dto.request.ForgotPasswordRequest;
import za.ac.cput.jobassistantapi.dto.request.ResetPasswordRequest;

public interface PasswordResetService {

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);
}
