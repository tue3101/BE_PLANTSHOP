package com.example.backendplantshop.service.intf;

import com.example.backendplantshop.dto.request.momo.CreatePaymentRequest;
import com.example.backendplantshop.dto.response.momo.CreatePaymentResponse;

public interface MoMoService {
    CreatePaymentResponse createPayment(CreatePaymentRequest request);
    boolean verifyCallback(String signature, String rawHash);
}

