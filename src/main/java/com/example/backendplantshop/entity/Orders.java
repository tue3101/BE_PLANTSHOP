package com.example.backendplantshop.entity;

import com.example.backendplantshop.enums.OrderSatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Orders {
    private int order_id;
    private BigDecimal total;
    private BigDecimal discount_amount;
    private BigDecimal final_total;
    private LocalDateTime order_date;
    private OrderSatus status;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;

    private int user_id;
    private Integer discount_id; // Có thể null nếu không dùng discount
    private Boolean is_deleted;
}
