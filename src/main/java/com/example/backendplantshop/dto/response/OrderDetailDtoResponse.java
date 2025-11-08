package com.example.backendplantshop.dto.response;

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
public class OrderDetailDtoResponse {
    private int order_detail_id;
    private int quantity;
    private BigDecimal price_at_order;
    private BigDecimal sub_total;
    private String note;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
    private int product_id;
    private ProductDtoResponse product; // Thông tin sản phẩm
}

