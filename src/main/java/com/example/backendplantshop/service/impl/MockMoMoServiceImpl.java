package com.example.backendplantshop.service.impl;

import com.example.backendplantshop.dto.request.momo.CreatePaymentRequest;
import com.example.backendplantshop.dto.response.momo.CreatePaymentResponse;
import com.example.backendplantshop.service.intf.MoMoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Mock MoMo Service cho đồ án tốt nghiệp
 * Service này tạo QR code giả để demo mà không cần credentials thật
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "momo.mock.enabled", havingValue = "true", matchIfMissing = false)
public class MockMoMoServiceImpl implements MoMoService {
    
    @Override
    public CreatePaymentResponse createPayment(CreatePaymentRequest request) {
        log.info("=== MOCK PAYMENT MODE - Dùng cho đồ án tốt nghiệp ===");
        log.info("Tạo payment giả cho orderId: {}, amount: {}", request.getOrderId(), request.getAmount());
        
        // Tạo QR code URL giả (có thể dùng QR code generator online)
        String mockQrCodeUrl = String.format(
            "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=MOCK_PAYMENT_ORDER_%s_AMOUNT_%s",
            request.getOrderId(),
            request.getAmount()
        );
        
        // Tạo payUrl giả (redirect về return URL)
        String mockPayUrl = String.format(
            "http://localhost:3000/payment/return?orderId=%s&resultCode=0&message=Thanh toán thành công (MOCK)",
            request.getOrderId()
        );
        
        return CreatePaymentResponse.builder()
                .payUrl(mockPayUrl)
                .qrCodeUrl(mockQrCodeUrl)
                .deeplink("momo://mock?orderId=" + request.getOrderId())
                .orderId(String.valueOf(request.getOrderId()))
                .amount(request.getAmount().longValue())
                .message("Tạo thanh toán thành công (MOCK MODE - Dùng cho demo đồ án)")
                .build();
    }
    
    @Override
    public boolean verifyCallback(String signature, String rawHash) {
        // Trong mock mode, luôn return true
        log.info("Mock verify callback - luôn trả về true");
        return true;
    }
}

