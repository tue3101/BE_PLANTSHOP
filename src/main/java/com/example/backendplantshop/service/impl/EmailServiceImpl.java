package com.example.backendplantshop.service.impl;

import com.example.backendplantshop.service.intf.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {
    //kết hợp với JavaMailSender để gửi email.
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendOtpEmail(String toEmail, String otpCode) {
        try {
            //khởi tạo một đối tượng SimpleMailMessage trong Spring, dùng để gửi email dạng text đơn giản.
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Mã OTP xác thực email của Shop CỎ BA LÁ");
            message.setText("Xin chào,\n\n" +
                    "Mã OTP của bạn là: " + otpCode + "\n\n" +
                    "Mã OTP này có hiệu lực trong 5 phút.\n" +
                    "Vui lòng không chia sẻ mã này với bất kỳ ai.\n\n" +
                    "Trân trọng,\n" +
                    "Shop CỎ BA LÁ");
            
            mailSender.send(message);
            log.info("Đã gửi email OTP đến: {}", toEmail);
        } catch (Exception e) {
            log.error("Lỗi khi gửi email OTP đến {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Không thể gửi email OTP", e);
        }
    }
}

