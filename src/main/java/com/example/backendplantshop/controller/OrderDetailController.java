package com.example.backendplantshop.controller;

import com.example.backendplantshop.dto.response.ApiResponse;
import com.example.backendplantshop.dto.response.OrderDetailDtoResponse;
import com.example.backendplantshop.enums.ErrorCode;
import com.example.backendplantshop.service.intf.OrderDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/order-details")
@RequiredArgsConstructor
public class OrderDetailController {
    private final OrderDetailService orderDetailService;

    // Lấy chi tiết đơn hàng theo ID
    @GetMapping("/{orderDetailId}")
    public ApiResponse<OrderDetailDtoResponse> getOrderDetailById(@PathVariable("orderDetailId") int orderDetailId) {
        OrderDetailDtoResponse orderDetail = orderDetailService.getOrderDetailById(orderDetailId);
        return ApiResponse.<OrderDetailDtoResponse>builder()
                .statusCode(ErrorCode.CALL_API_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.CALL_API_SUCCESSFULL.getMessage())
                .data(orderDetail)
                .build();
    }

    // Lấy danh sách chi tiết đơn hàng theo orderId
    @GetMapping("/order/{orderId}")
    public ApiResponse<List<OrderDetailDtoResponse>> getOrderDetailsByOrderId(@PathVariable("orderId") int orderId) {
        List<OrderDetailDtoResponse> orderDetails = orderDetailService.getOrderDetailsByOrderId(orderId);
        return ApiResponse.<List<OrderDetailDtoResponse>>builder()
                .statusCode(ErrorCode.CALL_API_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.CALL_API_SUCCESSFULL.getMessage())
                .data(orderDetails)
                .build();
    }

    // Cập nhật chi tiết đơn hàng
    @PutMapping("/{orderDetailId}")
    public ApiResponse<Void> updateOrderDetail(
            @PathVariable("orderDetailId") int orderDetailId,
            @RequestParam("quantity") int quantity,
            @RequestParam("price_at_order") BigDecimal price_at_order,
            @RequestParam("sub_total") BigDecimal sub_total,
            @RequestParam(value = "note", required = false) String note) {
        orderDetailService.updateOrderDetail(orderDetailId, quantity, price_at_order, sub_total, note);
        return ApiResponse.<Void>builder()
                .statusCode(ErrorCode.UPDATE_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.UPDATE_SUCCESSFULL.getMessage())
                .build();
    }

    // Xóa chi tiết đơn hàng (chỉ admin)
    @DeleteMapping("/{orderDetailId}")
    public ApiResponse<Void> deleteOrderDetail(@PathVariable("orderDetailId") int orderDetailId) {
        orderDetailService.deleteOrderDetail(orderDetailId);
        return ApiResponse.<Void>builder()
                .statusCode(ErrorCode.DELETE_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.DELETE_SUCCESSFULL.getMessage())
                .build();
    }
}

