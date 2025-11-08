package com.example.backendplantshop.service.impl;

import com.example.backendplantshop.convert.OrderConvert;
import com.example.backendplantshop.dto.request.OrderDtoRequest;
import com.example.backendplantshop.dto.request.OrderDetailDtoRequest;
import com.example.backendplantshop.dto.request.UpdateOrderStatusDtoRequest;
import com.example.backendplantshop.dto.response.OrderDetailDtoResponse;
import com.example.backendplantshop.dto.response.OrderDtoResponse;
import com.example.backendplantshop.entity.Discounts;
import com.example.backendplantshop.entity.OrderDetails;
import com.example.backendplantshop.entity.Orders;
import com.example.backendplantshop.entity.Products;
import com.example.backendplantshop.enums.DiscountType;
import com.example.backendplantshop.enums.ErrorCode;
import com.example.backendplantshop.enums.OrderSatus;
import com.example.backendplantshop.exception.AppException;
import com.example.backendplantshop.mapper.*;
import com.example.backendplantshop.service.intf.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderMapper orderMapper;
    private final OrderDetailMapper orderDetailMapper;
    private final ProductMapper productMapper;
    private final DiscountMapper discountMapper;
    private final CartDetailMapper cartDetailMapper;
    private final CartMapper cartMapper;
    private final AuthServiceImpl authService;

    @Override
    @Transactional
    public OrderDtoResponse createOrder(OrderDtoRequest orderRequest) {
        int currentUserId = authService.getCurrentUserId();
        String role = authService.getCurrentRole();
        if (!authService.isUser(role)) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        // 1. Validate danh sách sản phẩm
        if (orderRequest.getItems() == null || orderRequest.getItems().isEmpty()) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD);
        }

        // 2. Validate các giá trị từ FE
        if (orderRequest.getTotal() == null || orderRequest.getTotal().compareTo(BigDecimal.ZERO) < 0) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD);
        }
        if (orderRequest.getDiscount_amount() == null) {
            orderRequest.setDiscount_amount(BigDecimal.ZERO);
        }
        if (orderRequest.getFinal_total() == null || orderRequest.getFinal_total().compareTo(BigDecimal.ZERO) < 0) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD);
        }

        // 3. Xử lý discount (chỉ để lấy discount_id)
        Integer discountId = null;
        if (orderRequest.getDiscount_id() != null) {
            Discounts discount = discountMapper.findById(orderRequest.getDiscount_id());
            if (discount == null || (discount.getIs_deleted() != null && discount.getIs_deleted())) {
                throw new AppException(ErrorCode.DISCOUNT_NOT_EXISTS);
            }
            discountId = discount.getDiscount_id();
        } else if (orderRequest.getDiscount_code() != null && !orderRequest.getDiscount_code().trim().isEmpty()) {
            Discounts discount = discountMapper.findByDiscountCode(orderRequest.getDiscount_code());
            if (discount == null || (discount.getIs_deleted() != null && discount.getIs_deleted())) {
                throw new AppException(ErrorCode.DISCOUNT_NOT_EXISTS);
            }
            discountId = discount.getDiscount_id();
        }

        // 4. Validate và tạo OrderDetails từ dữ liệu FE gửi
        List<OrderDetails> orderDetailsList = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (OrderDetailDtoRequest item : orderRequest.getItems()) {
            // Kiểm tra sản phẩm tồn tại
            Products product = productMapper.findById(item.getProduct_id());
            if (product == null) {
                throw new AppException(ErrorCode.PRODUCT_NOT_EXISTS);
            }

            // Kiểm tra số lượng
            if (item.getQuantity() <= 0) {
                throw new AppException(ErrorCode.INVALID_QUANTITY);
            }
            if (product.getQuantity() < item.getQuantity()) {
                throw new AppException(ErrorCode.QUANTITY_IS_NOT_ENOUGH);
            }

            // Validate các giá trị từ FE
            if (item.getPrice_at_order() == null || item.getPrice_at_order().compareTo(BigDecimal.ZERO) < 0) {
                throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD);
            }
            if (item.getSub_total() == null || item.getSub_total().compareTo(BigDecimal.ZERO) < 0) {
                throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD);
            }

            // Tạo OrderDetail từ dữ liệu FE gửi
            OrderDetails orderDetail = OrderConvert.convertOrderDetailDtoRequestToOrderDetails(item, now);
            orderDetailsList.add(orderDetail);
        }

        // 5. Tạo Order từ dữ liệu FE gửi
        Orders order = OrderConvert.convertOrderDtoRequestToOrders(orderRequest, currentUserId, discountId, now);

        orderMapper.insert(order);

        // 7. Tạo OrderDetails và cập nhật số lượng sản phẩm
        List<OrderDetailDtoResponse> orderDetailDtos = new ArrayList<>();
        for (OrderDetails orderDetail : orderDetailsList) {
            orderDetail.setOrder_id(order.getOrder_id());
            orderDetailMapper.insert(orderDetail);

            // Cập nhật số lượng sản phẩm trong kho
            productMapper.updateProductQuantity(orderDetail.getProduct_id(), orderDetail.getQuantity());

            // Lấy thông tin sản phẩm để trả về
            Products product = productMapper.findById(orderDetail.getProduct_id());
            OrderDetailDtoResponse orderDetailDto = OrderConvert.convertOrderDetailToOrderDetailDtoResponseWithProduct(
                    orderDetail, product);
            orderDetailDtos.add(orderDetailDto);
        }

        // 8. Xóa mềm các sản phẩm đã selected (selected = 1) trong giỏ hàng sau khi tạo đơn hàng thành công
        try {
            cartDetailMapper.deleteSelectedProductsByUserId(currentUserId);
            log.info("Đã xóa mềm các sản phẩm đã selected trong giỏ hàng của user {}", currentUserId);
        } catch (Exception e) {
            log.warn("Không thể xóa sản phẩm đã selected khỏi giỏ hàng của user {}: {}", currentUserId, e.getMessage());
        }

        // 9. Tạo response
        OrderDtoResponse response = OrderConvert.convertOrderToOrderDtoResponse(order, orderDetailsList);
        response.setOrder_details(orderDetailDtos);
        return response;
    }

    @Override
    public OrderDtoResponse getOrderById(int orderId) {
        int currentUserId = authService.getCurrentUserId();
        String role = authService.getCurrentRole();
        
        Orders order = orderMapper.findById(orderId);
        if (order == null) {
            throw new AppException(ErrorCode.LIST_NOT_FOUND);
        }

        // User chỉ có thể xem đơn hàng của mình, Admin có thể xem tất cả
        if (authService.isUser(role) && order.getUser_id() != currentUserId) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        List<OrderDetails> orderDetails = orderDetailMapper.findByOrderId(orderId);
        List<OrderDetailDtoResponse> orderDetailDtos = new ArrayList<>();
        
        for (OrderDetails orderDetail : orderDetails) {
            Products product = productMapper.findById(orderDetail.getProduct_id());
            if (product != null) {
                OrderDetailDtoResponse orderDetailDto = OrderConvert.convertOrderDetailToOrderDetailDtoResponseWithProduct(
                        orderDetail, product);
                orderDetailDtos.add(orderDetailDto);
            } else {
                // Nếu sản phẩm đã bị xóa, vẫn hiển thị order detail nhưng không có thông tin sản phẩm
                OrderDetailDtoResponse orderDetailDto = OrderConvert.convertOrderDetailToOrderDetailDtoResponse(orderDetail);
                orderDetailDtos.add(orderDetailDto);
            }
        }

        OrderDtoResponse response = OrderConvert.convertOrderToOrderDtoResponse(order, orderDetails);
        response.setOrder_details(orderDetailDtos);
        return response;
    }

    @Override
    public List<OrderDtoResponse> getOrdersByUserId(int userId) {
        int currentUserId = authService.getCurrentUserId();
        String role = authService.getCurrentRole();
        
        // User chỉ có thể xem đơn hàng của mình, Admin có thể xem tất cả
        if (authService.isUser(role) && userId != currentUserId) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        List<Orders> orders = orderMapper.findByUserId(userId);
        if (orders.isEmpty()) {
            throw new AppException(ErrorCode.LIST_NOT_FOUND);
        }

        return orders.stream()
                .map(order -> {
                    List<OrderDetails> orderDetails = orderDetailMapper.findByOrderId(order.getOrder_id());
                    List<OrderDetailDtoResponse> orderDetailDtos = new ArrayList<>();
                    
                    for (OrderDetails orderDetail : orderDetails) {
                        Products product = productMapper.findById(orderDetail.getProduct_id());
                        if (product != null) {
                            OrderDetailDtoResponse orderDetailDto = OrderConvert.convertOrderDetailToOrderDetailDtoResponseWithProduct(
                                    orderDetail, product);
                            orderDetailDtos.add(orderDetailDto);
                        } else {
                            OrderDetailDtoResponse orderDetailDto = OrderConvert.convertOrderDetailToOrderDetailDtoResponse(orderDetail);
                            orderDetailDtos.add(orderDetailDto);
                        }
                    }
                    
                    OrderDtoResponse response = OrderConvert.convertOrderToOrderDtoResponse(order, orderDetails);
                    response.setOrder_details(orderDetailDtos);
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderDtoResponse> getAllOrders() {
        String role = authService.getCurrentRole();
        if (!authService.isAdmin(role)) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        List<Orders> orders = orderMapper.getAll();
        if (orders.isEmpty()) {
            throw new AppException(ErrorCode.LIST_NOT_FOUND);
        }

        return orders.stream()
                .map(order -> {
                    List<OrderDetails> orderDetails = orderDetailMapper.findByOrderId(order.getOrder_id());
                    List<OrderDetailDtoResponse> orderDetailDtos = new ArrayList<>();
                    
                    for (OrderDetails orderDetail : orderDetails) {
                        Products product = productMapper.findById(orderDetail.getProduct_id());
                        if (product != null) {
                            OrderDetailDtoResponse orderDetailDto = OrderConvert.convertOrderDetailToOrderDetailDtoResponseWithProduct(
                                    orderDetail, product);
                            orderDetailDtos.add(orderDetailDto);
                        } else {
                            OrderDetailDtoResponse orderDetailDto = OrderConvert.convertOrderDetailToOrderDetailDtoResponse(orderDetail);
                            orderDetailDtos.add(orderDetailDto);
                        }
                    }
                    
                    OrderDtoResponse response = OrderConvert.convertOrderToOrderDtoResponse(order, orderDetails);
                    response.setOrder_details(orderDetailDtos);
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderDtoResponse updateOrderStatus(int orderId, UpdateOrderStatusDtoRequest request) {
        int currentUserId = authService.getCurrentUserId();
        String role = authService.getCurrentRole();
        
        Orders order = orderMapper.findById(orderId);
        if (order == null) {
            throw new AppException(ErrorCode.LIST_NOT_FOUND);
        }

        // Kiểm tra quyền: Admin có thể cập nhật bất kỳ đơn hàng nào, User chỉ có thể cập nhật đơn hàng của mình
        // Nhưng thường chỉ Admin mới được cập nhật status
        if (authService.isUser(role) && order.getUser_id() != currentUserId) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        // Validate status
        if (request.getStatus() == null) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD);
        }

        // Cập nhật status
        order.setStatus(request.getStatus());
        order.setUpdated_at(LocalDateTime.now());
        orderMapper.update(order);

        // Lấy order details để trả về
        List<OrderDetails> orderDetails = orderDetailMapper.findByOrderId(orderId);
        List<OrderDetailDtoResponse> orderDetailDtos = new ArrayList<>();
        
        for (OrderDetails orderDetail : orderDetails) {
            Products product = productMapper.findById(orderDetail.getProduct_id());
            if (product != null) {
                OrderDetailDtoResponse orderDetailDto = OrderConvert.convertOrderDetailToOrderDetailDtoResponseWithProduct(
                        orderDetail, product);
                orderDetailDtos.add(orderDetailDto);
            } else {
                OrderDetailDtoResponse orderDetailDto = OrderConvert.convertOrderDetailToOrderDetailDtoResponse(orderDetail);
                orderDetailDtos.add(orderDetailDto);
            }
        }

        OrderDtoResponse response = OrderConvert.convertOrderToOrderDtoResponse(order, orderDetails);
        response.setOrder_details(orderDetailDtos);
        return response;
    }
}

