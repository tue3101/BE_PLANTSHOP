package com.example.backendplantshop.service.intf;

import com.example.backendplantshop.dto.request.OrderDtoRequest;
import com.example.backendplantshop.dto.request.UpdateOrderStatusDtoRequest;
import com.example.backendplantshop.dto.request.UpdateShippingStatusDtoRequest;
import com.example.backendplantshop.dto.request.momo.MoMoCallbackRequest;
import com.example.backendplantshop.dto.response.OrderDtoResponse;

import java.util.List;

public interface OrderService {
    OrderDtoResponse createOrder(OrderDtoRequest orderRequest);
    OrderDtoResponse getOrderById(int orderId);
    List<OrderDtoResponse> getOrdersByUserId(int userId);
    List<OrderDtoResponse> getAllOrders();
    OrderDtoResponse updateOrderStatus(int orderId, UpdateOrderStatusDtoRequest request);
    OrderDtoResponse updateShippingStatus(int orderId, UpdateShippingStatusDtoRequest request);
//    OrderDtoResponse updateShippingInfo(int orderId, UpdateShippingInfoDtoRequest request);
    void deleteOrder(int orderId);
    void handleOrderPaymentCallback(Integer orderId, MoMoCallbackRequest callbackRequest);
}

