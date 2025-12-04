package com.example.backendplantshop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepositDtoResponse {
    private int deposit_id;
    private int order_id;
    private int method_id;
    private String method_name;
    private BigDecimal amount;
    private Boolean paid;
    private String momo_trans_id;
    private LocalDateTime created_at;
    private LocalDateTime paid_at;
}


