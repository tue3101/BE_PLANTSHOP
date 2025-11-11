package com.example.backendplantshop.service.intf;

public interface OtpService {
    String generateAndSendOtp(String email);
    String generateAndSendOtp(String email, Integer userId); // Overload với user_id (cho quên mật khẩu)
    boolean verifyOtp(String email, String otpCode); // Chỉ verify, không mark as used
    void markOtpAsUsed(String email, String otpCode); // Đánh dấu OTP đã sử dụng sau khi thành công
    void updateUserIdForOtp(String email, String otpCode, Integer userId); // Cập nhật user_id cho OTP sau khi đăng ký thành công
}

