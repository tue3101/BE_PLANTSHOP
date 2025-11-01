package com.example.backendplantshop.entity;

import com.example.backendplantshop.enums.DiscountType;
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
public class Discounts {
    private int discount_id;
    private String discount_code;
    private String discount_name;
    private BigDecimal value;
    private DiscountType type;
    private int quantity;
    private LocalDateTime updated_at;
    private LocalDateTime created_at;
    private Boolean is_deleted;


}
