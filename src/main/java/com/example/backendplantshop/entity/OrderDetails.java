package com.example.backendplantshop.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDetails {
    private int order_detail_id;
    private int quantity;
    private BigDecimal price_at_order;
    private BigDecimal sub_total;
    private String note;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
    private int product_id;
    private int order_id;
    private Boolean is_deleted;
}
