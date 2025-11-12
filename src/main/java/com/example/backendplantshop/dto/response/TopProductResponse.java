package com.example.backendplantshop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TopProductResponse {
    private Integer productId;
    private String productName;
    private String imgUrl;
    private Integer totalQuantitySold;    // Tổng số lượng đã bán
    private BigDecimal totalRevenue;      // Tổng doanh thu từ sản phẩm này
    private Integer orderCount;           // Số đơn hàng chứa sản phẩm này
}

