package com.example.backendplantshop.controller;

import com.example.backendplantshop.dto.request.OrderDtoRequest;
import com.example.backendplantshop.dto.request.UpdateOrderStatusDtoRequest;
import com.example.backendplantshop.dto.request.UpdateShippingStatusDtoRequest;
import com.example.backendplantshop.dto.response.ApiResponse;
import com.example.backendplantshop.dto.response.OrderDtoResponse;
import com.example.backendplantshop.enums.ErrorCode;
import com.example.backendplantshop.service.intf.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping("/add")
    public ApiResponse<OrderDtoResponse> createOrder(@Valid @RequestBody OrderDtoRequest orderRequest) {
        OrderDtoResponse order = orderService.createOrder(orderRequest);
        return ApiResponse.<OrderDtoResponse>builder()
                .statusCode(ErrorCode.ADD_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.ADD_SUCCESSFULL.getMessage())
                .data(order)
                .build();
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderDtoResponse> getOrderById(@PathVariable("orderId") int orderId) {
        OrderDtoResponse order = orderService.getOrderById(orderId);
        return ApiResponse.<OrderDtoResponse>builder()
                .statusCode(ErrorCode.CALL_API_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.CALL_API_SUCCESSFULL.getMessage())
                .data(order)
                .build();
    }

    @GetMapping("/user/{userId}")
    public ApiResponse<List<OrderDtoResponse>> getOrdersByUserId(@PathVariable("userId") int userId) {
        List<OrderDtoResponse> orders = orderService.getOrdersByUserId(userId);
        return ApiResponse.<List<OrderDtoResponse>>builder()
                .statusCode(ErrorCode.CALL_API_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.CALL_API_SUCCESSFULL.getMessage())
                .data(orders)
                .build();
    }


    @GetMapping("/get-all")
    public ApiResponse<List<OrderDtoResponse>> getAllOrders() {
        List<OrderDtoResponse> orders = orderService.getAllOrders();
        return ApiResponse.<List<OrderDtoResponse>>builder()
                .statusCode(ErrorCode.CALL_API_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.CALL_API_SUCCESSFULL.getMessage())
                .data(orders)
                .build();
    }

    @PutMapping("/{orderId}")
    public ApiResponse<OrderDtoResponse> updateOrderStatus(
            @PathVariable("orderId") int orderId,
            @Valid @RequestBody UpdateOrderStatusDtoRequest request) {
        OrderDtoResponse order = orderService.updateOrderStatus(orderId, request);
        return ApiResponse.<OrderDtoResponse>builder()
                .statusCode(ErrorCode.UPDATE_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.UPDATE_SUCCESSFULL.getMessage())
                .data(order)
                .build();
    }

    @PutMapping("/{orderId}/shipping-status")
    public ApiResponse<OrderDtoResponse> updateShippingStatus(
            @PathVariable("orderId") int orderId,
            @Valid @RequestBody UpdateShippingStatusDtoRequest request) {
        OrderDtoResponse order = orderService.updateShippingStatus(orderId, request);
        return ApiResponse.<OrderDtoResponse>builder()
                .statusCode(ErrorCode.UPDATE_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.UPDATE_SUCCESSFULL.getMessage())
                .data(order)
                .build();
    }

//    @PutMapping("/{orderId}/shipping-info")
//    public ApiResponse<OrderDtoResponse> updateShippingInfo(
//            @PathVariable("orderId") int orderId,
//            @Valid @RequestBody UpdateShippingInfoDtoRequest request) {
//        OrderDtoResponse order = orderService.updateShippingInfo(orderId, request);
//        return ApiResponse.<OrderDtoResponse>builder()
//                .statusCode(ErrorCode.UPDATE_SUCCESSFULL.getCode())
//                .success(Boolean.TRUE)
//                .message(ErrorCode.UPDATE_SUCCESSFULL.getMessage())
//                .data(order)
//                .build();
//    }

    @DeleteMapping("/{orderId}")
    public ApiResponse<Void> deleteOrder(@PathVariable("orderId") int orderId) {
        orderService.deleteOrder(orderId);
        return ApiResponse.<Void>builder()
                .statusCode(ErrorCode.DELETE_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.DELETE_SUCCESSFULL.getMessage())
                .build();
    }
}

