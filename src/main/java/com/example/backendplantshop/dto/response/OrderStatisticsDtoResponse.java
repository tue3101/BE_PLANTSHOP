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
public class OrderStatisticsDtoResponse {
    private BigDecimal totalRevenue;      // Tổng doanh thu
    private Integer totalOrders;          // Tổng số đơn hàng
    private Integer completedOrders;      // Số đơn hàng đã hoàn thành
    private Integer cancelledOrders;      // Số đơn hàng đã hủy
    private BigDecimal averageOrderValue; // Giá trị đơn hàng trung bình
}

