package com.example.backendplantshop.controller;

import com.example.backendplantshop.dto.response.ApiResponse;
import com.example.backendplantshop.dto.response.DepositDtoResponse;
import com.example.backendplantshop.dto.response.momo.CreatePaymentResponse;
import com.example.backendplantshop.enums.ErrorCode;
import com.example.backendplantshop.service.intf.DepositService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/deposits")
@RequiredArgsConstructor
public class DepositController {

    private final DepositService depositService;

    @PostMapping("/{orderId}/momo")
    public ApiResponse<CreatePaymentResponse> createDepositPayment(@PathVariable("orderId") int orderId) {
        CreatePaymentResponse response = depositService.createDepositPayment(orderId);
        return ApiResponse.<CreatePaymentResponse>builder()
                .statusCode(ErrorCode.CALL_API_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.CALL_API_SUCCESSFULL.getMessage())
                .data(response)
                .build();
    }

    @GetMapping("/order/{orderId}")
    public ApiResponse<DepositDtoResponse> getDepositByOrderId(@PathVariable("orderId") int orderId) {
        DepositDtoResponse deposit = depositService.getDepositByOrderId(orderId);
        return ApiResponse.<DepositDtoResponse>builder()
                .statusCode(ErrorCode.CALL_API_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.CALL_API_SUCCESSFULL.getMessage())
                .data(deposit)
                .build();
    }
}


