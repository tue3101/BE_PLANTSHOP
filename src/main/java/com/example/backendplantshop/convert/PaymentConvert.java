package com.example.backendplantshop.convert;

import com.example.backendplantshop.dto.request.PaymentDtoRequest;
import com.example.backendplantshop.dto.response.PaymentDtoResponse;
import com.example.backendplantshop.dto.response.PaymentMethodDtoResponse;
import com.example.backendplantshop.entity.Payment;
import com.example.backendplantshop.entity.PaymentMethod;

import java.time.LocalDateTime;

public class PaymentConvert {
    
    public static Payment convertPaymentDtoRequestToPayment(PaymentDtoRequest request, int orderId, LocalDateTime now) {
        return Payment.builder()
                .method_id(request.getMethod_id())
                .amount(request.getAmount())
                .payment_date(now)
                .status(request.getStatus())
                .order_id(orderId)
                .is_deleted(false)
                .build();
    }
    
    public static PaymentDtoResponse convertPaymentToPaymentDtoResponse(Payment payment, PaymentMethod paymentMethod) {
        return PaymentDtoResponse.builder()
                .payment_id(payment.getPayment_id())
                .method_id(payment.getMethod_id())
                .method_name(paymentMethod != null ? paymentMethod.getMethod_name() : null)
                .amount(payment.getAmount())
                .payment_date(payment.getPayment_date())
                .status(payment.getStatus())
                .order_id(payment.getOrder_id())
                .build();
    }
    
    public static PaymentMethodDtoResponse convertPaymentMethodToPaymentMethodDtoResponse(PaymentMethod paymentMethod) {
        return PaymentMethodDtoResponse.builder()
                .method_id(paymentMethod.getMethod_id())
                .method_name(paymentMethod.getMethod_name())
                .build();
    }
}

