package com.example.backendplantshop.dto.request.momo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MoMoPaymentRequest {
    private String partnerCode;
    private String partnerName;
    private String storeId;
    private String requestId;
    private Long amount;
    private String orderId;
    private String orderInfo;
    private String redirectUrl;
    private String ipnUrl;
    private String requestType;
    private String extraData;
    private Boolean autoCapture;
    private String lang;
    private String signature;
}

