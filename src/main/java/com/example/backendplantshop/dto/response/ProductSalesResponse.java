package com.example.backendplantshop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductSalesResponse {
    private Integer totalQuantitySold;  // Tổng số sản phẩm bán được
    private Integer year;               // Năm
    private Integer month;              // Tháng (null nếu là thống kê theo năm)
}

