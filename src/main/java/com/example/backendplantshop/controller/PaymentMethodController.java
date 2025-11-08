package com.example.backendplantshop.controller;

import com.example.backendplantshop.dto.response.ApiResponse;
import com.example.backendplantshop.dto.response.PaymentMethodDtoResponse;
import com.example.backendplantshop.enums.ErrorCode;
import com.example.backendplantshop.service.intf.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/payment-methods")
@RequiredArgsConstructor
public class PaymentMethodController {
    
    private final PaymentService paymentService;
    
    @GetMapping("/get-all")
    public ApiResponse<List<PaymentMethodDtoResponse>> getAllPaymentMethods() {
        List<PaymentMethodDtoResponse> methods = paymentService.getAllPaymentMethods();
        return ApiResponse.<List<PaymentMethodDtoResponse>>builder()
                .statusCode(ErrorCode.CALL_API_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.CALL_API_SUCCESSFULL.getMessage())
                .data(methods)
                .build();
    }
}

