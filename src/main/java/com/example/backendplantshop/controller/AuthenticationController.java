package com.example.backendplantshop.controller;

import com.example.backendplantshop.dto.request.users.ChangePasswordDtoRequest;
import com.example.backendplantshop.dto.request.users.GoogleLoginDtoRequest;
import com.example.backendplantshop.dto.request.users.LoginDtoRequest;
import com.example.backendplantshop.dto.request.users.RegisterDtoRequest;
import com.example.backendplantshop.dto.request.users.SendOtpDtoRequest;
import com.example.backendplantshop.dto.request.users.SendOtpRegisterDtoRequest;
import com.example.backendplantshop.dto.request.users.VerifyOtpDtoRequest;
import com.example.backendplantshop.dto.request.users.ForgotPasswordDtoRequest;
import com.example.backendplantshop.dto.response.ApiResponse;
import com.example.backendplantshop.dto.response.user.LoginDtoResponse;
import com.example.backendplantshop.enums.ErrorCode;
import com.example.backendplantshop.service.intf.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthenticationService authenticationService;
    @PostMapping("/register")
    ApiResponse<Void> registerUser(@Valid @RequestBody RegisterDtoRequest registerDtoRequest) {
        authenticationService.register(registerDtoRequest);
        return ApiResponse.<Void>builder()
                .statusCode(ErrorCode.REGISTER_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.REGISTER_SUCCESSFULL.getMessage())
                .build();
    }

    @PostMapping("/login")
    ApiResponse<LoginDtoResponse> loginUser(@Valid @RequestBody LoginDtoRequest loginDtoRequest) {
        return ApiResponse.<LoginDtoResponse>builder()
                .statusCode(ErrorCode.LOGIN_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.LOGIN_SUCCESSFULL.getMessage())
                .data(authenticationService.login(loginDtoRequest))
                .build();
    }

    @PutMapping("/change-password")
    ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordDtoRequest changePasswordDtoRequest,@RequestHeader("Authorization") String authHeader) {
       authenticationService.changePassword(changePasswordDtoRequest, authHeader);
        return ApiResponse.<Void>builder()
                .statusCode(ErrorCode.CHANGEPASSWORD_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.CHANGEPASSWORD_SUCCESSFULL.getMessage())
                .build();
    }

    // Endpoint làm mới access token bằng refresh token
    @PostMapping("/refresh")
    ApiResponse<LoginDtoResponse> refreshToken(@RequestHeader("Authorization") String authHeader) {
        LoginDtoResponse response = authenticationService.refresh(authHeader);
        return ApiResponse.<LoginDtoResponse>builder()
                .statusCode(ErrorCode.LOGIN_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message("Làm mới access token thành công")
                .data(response)
                .build();
    }

    @PostMapping("/logout")
    ApiResponse<Void> logout(@RequestHeader("Authorization") String authHeader) {
        authenticationService.logout(authHeader);
        return ApiResponse.<Void>builder()
                .statusCode(ErrorCode.LOGIN_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message("đăng xuất thành công")
                .build();
    }

    @PostMapping("/google")
    ApiResponse<LoginDtoResponse> loginWithGoogle(@Valid @RequestBody GoogleLoginDtoRequest googleLoginDtoRequest) {
        return ApiResponse.<LoginDtoResponse>builder()
                .statusCode(ErrorCode.LOGIN_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.LOGIN_SUCCESSFULL.getMessage())
                .data(authenticationService.loginWithGoogle(googleLoginDtoRequest))
                .build();
    }

    @PostMapping("/send-otp-register")
    ApiResponse<Void> sendOtpForRegister(@Valid @RequestBody SendOtpRegisterDtoRequest request) {
        authenticationService.sendOtpForRegister(request);
        return ApiResponse.<Void>builder()
                .statusCode(ErrorCode.CALL_API_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message("Đã gửi mã OTP đến email của bạn")
                .build();
    }

    @PostMapping("/verify-otp")
    ApiResponse<Boolean> verifyOtp(@Valid @RequestBody VerifyOtpDtoRequest request) {
        boolean isValid = authenticationService.verifyOtp(request);
        return ApiResponse.<Boolean>builder()
                .statusCode(ErrorCode.CALL_API_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(isValid ? "Xác thực OTP thành công" : "Mã OTP không hợp lệ hoặc đã hết hạn")
                .data(isValid)
                .build();
    }

    @PostMapping("/forgot-password/send-otp")
    ApiResponse<Void> sendOtpForgotPassword(@Valid @RequestBody SendOtpDtoRequest request) {
        authenticationService.sendOtpForgotPassword(request);
        return ApiResponse.<Void>builder()
                .statusCode(ErrorCode.CALL_API_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message("Đã gửi mã OTP đến email của bạn để đặt lại mật khẩu")
                .build();
    }

    @PostMapping("/forgot-password/reset")
    ApiResponse<Void> resetPassword(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody ForgotPasswordDtoRequest request) {
        // Nếu có token → reset password bằng token (đã đăng nhập)
        // Nếu không có token → reset password bằng OTP (chưa đăng nhập)
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authenticationService.resetPassword(authHeader, request);
        } else {
            authenticationService.resetPassword(request);
        }
        return ApiResponse.<Void>builder()
                .statusCode(ErrorCode.CHANGEPASSWORD_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message("Đặt lại mật khẩu thành công")
                .build();
    }
}
