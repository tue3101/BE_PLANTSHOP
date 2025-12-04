package com.example.backendplantshop.service.impl;

import com.example.backendplantshop.entity.EmailOtp;
import com.example.backendplantshop.enums.ErrorCode;
import com.example.backendplantshop.exception.AppException;
import com.example.backendplantshop.mapper.EmailOtpMapper;
import com.example.backendplantshop.service.intf.EmailService;
import com.example.backendplantshop.service.intf.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {
    private final EmailOtpMapper emailOtpMapper;
    private final EmailService emailService;
    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final SecureRandom random = new SecureRandom();


    //tạo otp cho đăng ký vì ban đầu đk userid sẽ là null
    @Override
    @Transactional
    public String generateAndSendOtp(String email) {
        return generateAndSendOtp(email, null);
    }


    //hàm tạo và gửi otp
    @Override
    @Transactional
    public String generateAndSendOtp(String email, Integer userId) {
        // Tạo mã OTP ngẫu nhiên 6 chữ số
        String otpCode = generateOtp();
        
        // Tính thời gian hết hạn
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(OTP_EXPIRY_MINUTES);
        
        // Lưu OTP vào database
        // user_id có thể NULL khi gửi OTP cho đăng ký (user chưa tồn tại)
        EmailOtp emailOtp = EmailOtp.builder()
                .email(email)
                .user_id(userId) // NULL khi đăng ký, có giá trị khi quên mật khẩu
                .otp_code(otpCode)
                .created_at(now)
                .expires_at(expiresAt)
                .is_deleted(false)
                .build();
        
        emailOtpMapper.insertOtp(emailOtp);
        
        // Gửi email OTP
        emailService.sendOtpEmail(email, otpCode);
        
        log.info("Đã tạo và gửi OTP cho email: {} với user_id: {}", email, userId);
        return otpCode;
    }

    @Override
    @Transactional
    public boolean verifyOtp(String email, String otpCode) {
        // Chỉ verify OTP, KHÔNG mark as used
        // OTP sẽ được mark as used sau khi reset password/register thành công
        // Tìm OTP với otp_code chính xác
        EmailOtp emailOtp = emailOtpMapper.findByEmailAndOtp(email, otpCode);
        
        if (emailOtp == null) {
            log.warn("OTP không hợp lệ hoặc đã hết hạn cho email: {} với OTP: {}", email, otpCode);
            return false;
        }
        
        log.info("Đã xác thực OTP thành công cho email: {} với OTP ID: {} (chưa mark as used)", email, emailOtp.getOtp_id());
        return true;
    }


    //đánh dấu otp đã được sử dụng
    @Override
    @Transactional
    public void markOtpAsUsed(String email, String otpCode) {
        // Tìm OTP chưa được mark và mark as used
//        EmailOtp emailOtp = emailOtpMapper.findByEmailAndOtpForMark(email, otpCode);
        EmailOtp emailOtp = emailOtpMapper.findByEmailAndOtp(email, otpCode);


        if (emailOtp != null) {
            emailOtpMapper.markAsUsed(emailOtp.getOtp_id());
            log.info("Đã đánh dấu OTP ID: {} đã sử dụng cho email: {} với OTP: {}", emailOtp.getOtp_id(), email, otpCode);
        } else {
            log.warn("Không tìm thấy OTP hợp lệ để mark as used cho email: {} với OTP: {} (có thể đã được mark, hết hạn, hoặc không tồn tại)", email, otpCode);
        }
    }


    //cập nhật lại userid sau khi đk thành công
    @Override
    @Transactional
    public void updateUserIdForOtp(String email, String otpCode, Integer userId) {
        emailOtpMapper.updateUserIdByEmailAndOtp(email, otpCode, userId);
        log.info("Đã cập nhật user_id = {} cho OTP của email: {}", userId, email);
    }


    //StringBuilder cho phép thêm, xóa, chèn, đảo ngược mà không tạo ra object mới mỗi lần
    private String generateOtp() {
        StringBuilder otp = new StringBuilder(OTP_LENGTH);
        for (int i = 0; i < OTP_LENGTH; i++) {
            //append -> thêm số vừa sinh vào cuối chũi
            otp.append(random.nextInt(10)); //random 6 số ngto từ 0->9
        }
        return otp.toString();
    }
}

