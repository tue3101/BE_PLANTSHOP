package com.example.backendplantshop.service.intf;

import com.example.backendplantshop.dto.response.DepositDtoResponse;
import com.example.backendplantshop.dto.response.momo.CreatePaymentResponse;

public interface DepositService {
    CreatePaymentResponse createDepositPayment(int orderId);
    DepositDtoResponse getDepositByOrderId(int orderId);
    void handleDepositSuccess(int orderId, Long amount, Long transId);
    boolean requiresDeposit(int orderId);
}


