package com.example.backendplantshop.service.intf;

import com.example.backendplantshop.dto.request.users.ChangePasswordDtoRequest;
import com.example.backendplantshop.dto.request.users.GoogleLoginDtoRequest;
import com.example.backendplantshop.dto.request.users.LoginDtoRequest;
import com.example.backendplantshop.dto.request.users.RegisterDtoRequest;
import com.example.backendplantshop.dto.response.user.LoginDtoResponse;
import com.example.backendplantshop.dto.response.user.RegisterDtoResponse;

public interface AuthenticationService {
    RegisterDtoResponse register(RegisterDtoRequest registerDtoRequest);
    void logout(String authHeader);
    LoginDtoResponse login(LoginDtoRequest loginDtoRequest);
    LoginDtoResponse loginWithGoogle(GoogleLoginDtoRequest googleLoginDtoRequest);
    LoginDtoResponse refresh(String refreshToken);
    void changePassword(ChangePasswordDtoRequest changePasswordDtoRequest, String authHeader);
}
