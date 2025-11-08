package com.example.backendplantshop.dto.response.momo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreatePaymentResponse {
    private String payUrl;
    private String qrCodeUrl;
    private String deeplink;
    private String orderId;
    private Long amount;
    private String message;
}

