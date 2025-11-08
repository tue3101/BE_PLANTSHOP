package com.example.backendplantshop.dto.response.momo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MoMoPaymentResponse {
    private Integer resultCode;
    private String message;
    private String payUrl;
    private String deeplink;
    private String qrCodeUrl;
    private String deeplinkWebInApp;
    private String requestId;
    private Long amount;
    private String orderId;
    private String partnerCode;
    private String orderInfo;
    private String extraData;
    private String signature;
}

