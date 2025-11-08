package com.example.backendplantshop.dto.response;

import com.example.backendplantshop.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentDtoResponse {
    private int payment_id;
    private int method_id;
    private String method_name;
    private BigDecimal amount;
    private LocalDateTime payment_date;
    private PaymentStatus status;
    private int order_id;
}

