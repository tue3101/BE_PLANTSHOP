package com.example.backendplantshop.service.impl;

import com.example.backendplantshop.convert.OrderConvert;
import com.example.backendplantshop.dto.response.OrderDetailDtoResponse;
import com.example.backendplantshop.entity.OrderDetails;
import com.example.backendplantshop.entity.Orders;
import com.example.backendplantshop.entity.Products;
import com.example.backendplantshop.enums.ErrorCode;
import com.example.backendplantshop.enums.OrderSatus;
import com.example.backendplantshop.exception.AppException;
import com.example.backendplantshop.mapper.OrderDetailMapper;
import com.example.backendplantshop.mapper.OrderMapper;
import com.example.backendplantshop.mapper.ProductMapper;
import com.example.backendplantshop.service.intf.OrderDetailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderDetailServiceImpl implements OrderDetailService {
    private final OrderDetailMapper orderDetailMapper;
    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;
    private final AuthServiceImpl authService;

    @Override
    public OrderDetailDtoResponse getOrderDetailById(int orderDetailId) {
        OrderDetails orderDetail = orderDetailMapper.findById(orderDetailId);
        if (orderDetail == null) {
            throw new AppException(ErrorCode.LIST_NOT_FOUND);
        }

        // Kiểm tra quyền truy cập
        Orders order = orderMapper.findById(orderDetail.getOrder_id());
        if (order == null) {
            throw new AppException(ErrorCode.LIST_NOT_FOUND);
        }

        int currentUserId = authService.getCurrentUserId();
        String role = authService.getCurrentRole();
        
        // User chỉ có thể xem chi tiết đơn hàng của mình, Admin có thể xem tất cả
        if (authService.isUser(role) && order.getUser_id() != currentUserId) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        Products product = productMapper.findById(orderDetail.getProduct_id());
        if (product != null) {
            return OrderConvert.convertOrderDetailToOrderDetailDtoResponseWithProduct(orderDetail, product);
        } else {
            return OrderConvert.convertOrderDetailToOrderDetailDtoResponse(orderDetail);
        }
    }

    @Override
    public List<OrderDetailDtoResponse> getOrderDetailsByOrderId(int orderId) {
        // Kiểm tra quyền truy cập
        Orders order = orderMapper.findById(orderId);
        if (order == null) {
            throw new AppException(ErrorCode.LIST_NOT_FOUND);
        }

        int currentUserId = authService.getCurrentUserId();
        String role = authService.getCurrentRole();
        
        // User chỉ có thể xem chi tiết đơn hàng của mình, Admin có thể xem tất cả
        if (authService.isUser(role) && order.getUser_id() != currentUserId) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        List<OrderDetails> orderDetails = orderDetailMapper.findByOrderId(orderId);
        if (orderDetails.isEmpty()) {
            throw new AppException(ErrorCode.LIST_NOT_FOUND);
        }

        return orderDetails.stream()
                .map(orderDetail -> {
                    Products product = productMapper.findById(orderDetail.getProduct_id());
                    if (product != null) {
                        return OrderConvert.convertOrderDetailToOrderDetailDtoResponseWithProduct(orderDetail, product);
                    } else {
                        return OrderConvert.convertOrderDetailToOrderDetailDtoResponse(orderDetail);
                    }
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateOrderDetail(int orderDetailId, int quantity, BigDecimal price_at_order, BigDecimal sub_total, String note) {
        OrderDetails orderDetail = orderDetailMapper.findById(orderDetailId);
        if (orderDetail == null) {
            throw new AppException(ErrorCode.LIST_NOT_FOUND);
        }

        // Kiểm tra quyền truy cập
        Orders order = orderMapper.findById(orderDetail.getOrder_id());
        if (order == null) {
            throw new AppException(ErrorCode.LIST_NOT_FOUND);
        }

        int currentUserId = authService.getCurrentUserId();
        String role = authService.getCurrentRole();
        
        // Chỉ admin hoặc user sở hữu đơn hàng mới được cập nhật
        // Và chỉ khi đơn hàng chưa được xác nhận (PENDING_CONFIRMATION)
        if (authService.isUser(role) && order.getUser_id() != currentUserId) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        // Chỉ cho phép cập nhật khi đơn hàng chưa được xác nhận
        if (order.getStatus() != OrderSatus.PENDING_CONFIRMATION) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        // Validate số lượng
        if (quantity <= 0) {
            throw new AppException(ErrorCode.INVALID_QUANTITY);
        }

        // Validate các giá trị từ FE
        if (price_at_order == null || price_at_order.compareTo(BigDecimal.ZERO) < 0) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD);
        }
        if (sub_total == null || sub_total.compareTo(BigDecimal.ZERO) < 0) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD);
        }

        Products product = productMapper.findById(orderDetail.getProduct_id());
        if (product == null) {
            throw new AppException(ErrorCode.PRODUCT_NOT_EXISTS);
        }

        // Cập nhật order detail từ dữ liệu FE gửi
        orderDetail.setQuantity(quantity);
        orderDetail.setPrice_at_order(price_at_order);
        orderDetail.setSub_total(sub_total);
        orderDetail.setNote(note);
        orderDetail.setUpdated_at(LocalDateTime.now());
        orderDetailMapper.update(orderDetail);

        // Cập nhật lại tổng tiền của đơn hàng
        List<OrderDetails> allOrderDetails = orderDetailMapper.findByOrderId(order.getOrder_id());
        BigDecimal newTotal = allOrderDetails.stream()
                .map(OrderDetails::getSub_total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Tính lại discount amount và final total
        BigDecimal discountAmount = order.getDiscount_amount();
        BigDecimal finalTotal = newTotal.subtract(discountAmount);
        if (finalTotal.compareTo(BigDecimal.ZERO) < 0) {
            finalTotal = BigDecimal.ZERO;
        }

        order.setTotal(newTotal);
        order.setFinal_total(finalTotal);
        order.setUpdated_at(LocalDateTime.now());
        orderMapper.update(order);
    }

    @Override
    @Transactional
    public void deleteOrderDetail(int orderDetailId) {
        String role = authService.getCurrentRole();
        if (!authService.isAdmin(role)) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        OrderDetails orderDetail = orderDetailMapper.findById(orderDetailId);
        if (orderDetail == null) {
            throw new AppException(ErrorCode.LIST_NOT_FOUND);
        }

        // Kiểm tra đơn hàng
        Orders order = orderMapper.findById(orderDetail.getOrder_id());
        if (order == null) {
            throw new AppException(ErrorCode.LIST_NOT_FOUND);
        }

        // Chỉ cho phép xóa khi đơn hàng chưa được xác nhận
        if (order.getStatus() != OrderSatus.PENDING_CONFIRMATION) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        // Tính lại tổng tiền trước khi xóa (trừ đi subtotal của order detail này)
        BigDecimal newTotal = order.getTotal().subtract(orderDetail.getSub_total());
        if (newTotal.compareTo(BigDecimal.ZERO) < 0) {
            newTotal = BigDecimal.ZERO;
        }

        BigDecimal discountAmount = order.getDiscount_amount();
        BigDecimal finalTotal = newTotal.subtract(discountAmount);
        if (finalTotal.compareTo(BigDecimal.ZERO) < 0) {
            finalTotal = BigDecimal.ZERO;
        }

        // Xóa order detail (soft delete)
        orderDetailMapper.delete(orderDetailId);

        // Cập nhật lại tổng tiền của đơn hàng
        order.setTotal(newTotal);
        order.setFinal_total(finalTotal);
        order.setUpdated_at(LocalDateTime.now());
        orderMapper.update(order);
    }
}

