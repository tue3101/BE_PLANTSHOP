package com.example.backendplantshop.convert;

import com.example.backendplantshop.dto.request.OrderDetailDtoRequest;
import com.example.backendplantshop.dto.request.OrderDtoRequest;
import com.example.backendplantshop.dto.response.OrderDetailDtoResponse;
import com.example.backendplantshop.dto.response.OrderDtoResponse;
import com.example.backendplantshop.dto.response.ProductDtoResponse;
import com.example.backendplantshop.entity.OrderDetails;
import com.example.backendplantshop.entity.Orders;
import com.example.backendplantshop.entity.Products;
import com.example.backendplantshop.enums.OrderSatus;
import com.example.backendplantshop.enums.ShippingStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class OrderConvert {
    
    // Convert từ OrderDetailDtoRequest sang OrderDetails
    public static OrderDetails convertOrderDetailDtoRequestToOrderDetails(OrderDetailDtoRequest request, LocalDateTime now) {
        return OrderDetails.builder()
                .quantity(request.getQuantity())
                .price_at_order(request.getPrice_at_order())
                .sub_total(request.getSub_total())
                .note(request.getNote())
                .created_at(now)
                .updated_at(now)
                .product_id(request.getProduct_id())
                .is_deleted(false)
                .build();
    }
    
    // Convert từ OrderDtoRequest sang Orders
    public static Orders convertOrderDtoRequestToOrders(OrderDtoRequest request, int userId, Integer discountId, LocalDateTime now) {
        return Orders.builder()
                .total(request.getTotal())
                .discount_amount(request.getDiscount_amount())
                .final_total(request.getFinal_total())
                .order_date(now)
                .status(OrderSatus.PENDING_CONFIRMATION)
                .shipping_status(ShippingStatus.UNDELIVERED) 
                .created_at(now)
                .updated_at(now)
                .user_id(userId)
                .discount_id(discountId)
                .shipping_name(request.getShipping_name())
                .shipping_address(request.getShipping_address())
                .shipping_phone(request.getShipping_phone())
                .is_deleted(false)
                .build();
    }
    
    public static OrderDtoResponse convertOrderToOrderDtoResponse(Orders order, List<OrderDetails> orderDetails) {
        List<OrderDetailDtoResponse> orderDetailDtos = orderDetails.stream()
                .map(OrderConvert::convertOrderDetailToOrderDetailDtoResponse)
                .collect(Collectors.toList());
        
        return OrderDtoResponse.builder()
                .order_id(order.getOrder_id())
                .total(order.getTotal())
                .discount_amount(order.getDiscount_amount())
                .final_total(order.getFinal_total())
                .order_date(order.getOrder_date())
                .status(order.getStatus())
                .shipping_status(order.getShipping_status())
                .created_at(order.getCreated_at())
                .updated_at(order.getUpdated_at())
                .user_id(order.getUser_id())
                .discount_id(order.getDiscount_id())
                .shipping_name(order.getShipping_name())
                .shipping_address(order.getShipping_address())
                .shipping_phone(order.getShipping_phone())
                .order_details(orderDetailDtos)
                .build();
    }
    
    public static OrderDetailDtoResponse convertOrderDetailToOrderDetailDtoResponse(OrderDetails orderDetail) {
        return OrderDetailDtoResponse.builder()
                .order_detail_id(orderDetail.getOrder_detail_id())
                .quantity(orderDetail.getQuantity())
                .price_at_order(orderDetail.getPrice_at_order())
                .sub_total(orderDetail.getSub_total())
                .note(orderDetail.getNote())
                .created_at(orderDetail.getCreated_at())
                .updated_at(orderDetail.getUpdated_at())
                .product_id(orderDetail.getProduct_id())
                .build();
    }
    
    public static OrderDetailDtoResponse convertOrderDetailToOrderDetailDtoResponseWithProduct(
            OrderDetails orderDetail, Products product) {
        ProductDtoResponse productDto = ProductConvert.convertToProductDtoResponse(product);
        
        return OrderDetailDtoResponse.builder()
                .order_detail_id(orderDetail.getOrder_detail_id())
                .quantity(orderDetail.getQuantity())
                .price_at_order(orderDetail.getPrice_at_order())
                .sub_total(orderDetail.getSub_total())
                .note(orderDetail.getNote())
                .created_at(orderDetail.getCreated_at())
                .updated_at(orderDetail.getUpdated_at())
                .product_id(orderDetail.getProduct_id())
                .product(productDto)
                .build();
    }
}

