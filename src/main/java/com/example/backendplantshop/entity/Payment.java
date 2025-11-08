package com.example.backendplantshop.entity;

import com.example.backendplantshop.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    private int payment_id;
    private int method_id;
    private BigDecimal amount;
    private LocalDateTime payment_date;
    private PaymentStatus status;
    private int order_id;
    private Boolean is_deleted;
}
