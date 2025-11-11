package com.example.backendplantshop.service.intf;

public interface EmailService {
    void sendOtpEmail(String toEmail, String otpCode);
}

