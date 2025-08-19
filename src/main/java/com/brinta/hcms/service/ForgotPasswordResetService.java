package com.brinta.hcms.service;

import com.brinta.hcms.request.ForgotPasswordRequest;
import com.brinta.hcms.request.ResetPasswordRequest;
import jakarta.servlet.http.HttpServletRequest;

public interface ForgotPasswordResetService {

    void forgotPassword(ForgotPasswordRequest request, HttpServletRequest httpRequest);

    String validateResetToken(String token);

    void resetPassword(ResetPasswordRequest request);

}

