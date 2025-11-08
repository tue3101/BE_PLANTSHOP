package com.example.backendplantshop.service.intf;

import com.example.backendplantshop.dto.response.OrderDetailDtoResponse;

import java.math.BigDecimal;
import java.util.List;

public interface OrderDetailService {
    // Lấy chi tiết đơn hàng theo ID
    OrderDetailDtoResponse getOrderDetailById(int orderDetailId);
    
    // Lấy danh sách chi tiết đơn hàng theo orderId
    List<OrderDetailDtoResponse> getOrderDetailsByOrderId(int orderId);
    
    // Cập nhật chi tiết đơn hàng (chỉ admin hoặc khi đơn hàng chưa được xác nhận)
    void updateOrderDetail(int orderDetailId, int quantity, BigDecimal price_at_order, BigDecimal sub_total, String note);
    
    // Xóa chi tiết đơn hàng (soft delete, chỉ admin)
    void deleteOrderDetail(int orderDetailId);
}

