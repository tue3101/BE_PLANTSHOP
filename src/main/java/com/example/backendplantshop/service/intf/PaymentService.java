package com.example.backendplantshop.service.intf;

import com.example.backendplantshop.dto.request.PaymentDtoRequest;
import com.example.backendplantshop.dto.request.momo.MoMoCallbackRequest;
import com.example.backendplantshop.dto.response.PaymentDtoResponse;
import com.example.backendplantshop.dto.response.PaymentMethodDtoResponse;

import java.util.List;

public interface PaymentService {
    PaymentDtoResponse createPayment(PaymentDtoRequest request, int orderId);
    PaymentDtoResponse getPaymentById(int paymentId);
    List<PaymentDtoResponse> getPaymentsByOrderId(int orderId);
    List<PaymentDtoResponse> getAllPayments();
    List<PaymentMethodDtoResponse> getAllPaymentMethods();
    PaymentDtoResponse updatePaymentStatus(int paymentId, com.example.backendplantshop.enums.PaymentStatus status);
    void updatePaymentsByOrderId(int orderId, com.example.backendplantshop.enums.PaymentStatus status);
    
    // MoMo callback handling methods
    Integer extractOrderIdFromMoMoOrderId(String momoOrderId);
    void handleDepositCallback(Integer orderId, MoMoCallbackRequest callbackRequest);
}

