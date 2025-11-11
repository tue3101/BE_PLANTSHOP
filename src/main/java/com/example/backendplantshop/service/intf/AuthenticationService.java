package com.example.backendplantshop.service.intf;

import com.example.backendplantshop.dto.request.users.ChangePasswordDtoRequest;
import com.example.backendplantshop.dto.request.users.GoogleLoginDtoRequest;
import com.example.backendplantshop.dto.request.users.LoginDtoRequest;
import com.example.backendplantshop.dto.request.users.RegisterDtoRequest;
import com.example.backendplantshop.dto.request.users.SendOtpDtoRequest;
import com.example.backendplantshop.dto.request.users.SendOtpRegisterDtoRequest;
import com.example.backendplantshop.dto.request.users.VerifyOtpDtoRequest;
import com.example.backendplantshop.dto.request.users.ForgotPasswordDtoRequest;
import com.example.backendplantshop.dto.response.user.LoginDtoResponse;
import com.example.backendplantshop.dto.response.user.RegisterDtoResponse;

public interface AuthenticationService {
    RegisterDtoResponse register(RegisterDtoRequest registerDtoRequest);
    void logout(String authHeader);
    LoginDtoResponse login(LoginDtoRequest loginDtoRequest);
    LoginDtoResponse loginWithGoogle(GoogleLoginDtoRequest googleLoginDtoRequest);
    LoginDtoResponse refresh(String refreshToken);
    void changePassword(ChangePasswordDtoRequest changePasswordDtoRequest, String authHeader);
    void sendOtpForRegister(SendOtpRegisterDtoRequest request); // Gửi OTP với validation đầy đủ cho đăng ký
    boolean verifyOtp(VerifyOtpDtoRequest request);
    void sendOtpForgotPassword(SendOtpDtoRequest request);
    void resetPassword(ForgotPasswordDtoRequest request); // Reset password khi chưa đăng nhập (dùng OTP)
    void resetPassword(String authHeader, ForgotPasswordDtoRequest request); // Reset password khi đã đăng nhập (dùng token)
}
