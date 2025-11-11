package com.example.backendplantshop.service.impl;

import com.example.backendplantshop.convert.OrderConvert;
import com.example.backendplantshop.convert.PaymentConvert;
import com.example.backendplantshop.convert.UserConvert;
import com.example.backendplantshop.dto.request.OrderDtoRequest;
import com.example.backendplantshop.dto.request.OrderDetailDtoRequest;
import com.example.backendplantshop.dto.request.PaymentDtoRequest;
import com.example.backendplantshop.dto.request.UpdateOrderStatusDtoRequest;
import com.example.backendplantshop.dto.response.OrderDetailDtoResponse;
import com.example.backendplantshop.dto.response.OrderDtoResponse;
import com.example.backendplantshop.dto.response.user.UserDtoResponse;
import com.example.backendplantshop.entity.*;
import com.example.backendplantshop.enums.PaymentStatus;
import com.example.backendplantshop.enums.DiscountType;
import com.example.backendplantshop.enums.ErrorCode;
import com.example.backendplantshop.enums.OrderSatus;
import com.example.backendplantshop.exception.AppException;
import com.example.backendplantshop.mapper.*;
import com.example.backendplantshop.service.intf.OrderService;
import com.example.backendplantshop.service.intf.PaymentService;
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
    private final PaymentMapper paymentMapper;
    private final PaymentMethodMapper paymentMethodMapper;
    private final PaymentService paymentService;
    private final UserMapper userMapper;

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

        // 8. Tạo Payment nếu FE gửi thông tin thanh toán
        if (orderRequest.getPayment() != null) {
            try {
                PaymentDtoRequest paymentRequest = orderRequest.getPayment();
                
                // Validate payment method
                com.example.backendplantshop.entity.PaymentMethod paymentMethod = 
                    paymentMethodMapper.findById(paymentRequest.getMethod_id());
                if (paymentMethod == null) {
                    throw new AppException(ErrorCode.LIST_NOT_FOUND);
                }
                
                // Tạo payment
                Payment payment = PaymentConvert.convertPaymentDtoRequestToPayment(
                    paymentRequest, order.getOrder_id(), now);
                paymentMapper.insert(payment);
                log.info("Đã tạo payment với ID: {} cho order ID: {}", payment.getPayment_id(), order.getOrder_id());
            } catch (Exception e) {
                log.error("Lỗi khi tạo payment cho order {}: {}", order.getOrder_id(), e.getMessage(), e);
                // Không throw exception để không rollback order, chỉ log lỗi
            }
        }

        // 9. Xóa mềm các sản phẩm đã selected (selected = 1) trong giỏ hàng sau khi tạo đơn hàng thành công
        try {
            cartDetailMapper.deleteSelectedProductsByUserId(currentUserId);
            log.info("Đã xóa mềm các sản phẩm đã selected trong giỏ hàng của user {}", currentUserId);
        } catch (Exception e) {
            log.warn("Không thể xóa sản phẩm đã selected khỏi giỏ hàng của user {}: {}", currentUserId, e.getMessage());
        }

        // 10. Tạo response
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
        // Kiểm tra quyền: chỉ admin mới được xem tất cả đơn hàng
        String role = authService.getCurrentRole();
        if (!authService.isAdmin(role)) {
            log.warn("User không có quyền xem tất cả đơn hàng. Role: {}", role);
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        // Lấy tất cả đơn hàng từ database
        List<Orders> orders = orderMapper.getAll();
        
        // Nếu danh sách rỗng, trả về list rỗng thay vì throw exception
        if (orders == null || orders.isEmpty()) {
            log.info("Không có đơn hàng nào trong hệ thống");
            return new ArrayList<>();
        }

        log.info("Lấy tất cả đơn hàng: {} đơn hàng", orders.size());

        // Convert sang DTO và thêm thông tin chi tiết
        return orders.stream()
                .map(order -> {
                    // Lấy chi tiết đơn hàng
                    List<OrderDetails> orderDetails = orderDetailMapper.findByOrderId(order.getOrder_id());
                    List<OrderDetailDtoResponse> orderDetailDtos = new ArrayList<>();
                    
                    // Convert order details sang DTO với thông tin sản phẩm
                    for (OrderDetails orderDetail : orderDetails) {
                        Products product = productMapper.findById(orderDetail.getProduct_id());
                        if (product != null) {
                            OrderDetailDtoResponse orderDetailDto = OrderConvert.convertOrderDetailToOrderDetailDtoResponseWithProduct(
                                    orderDetail, product);
                            orderDetailDtos.add(orderDetailDto);
                        } else {
                            // Nếu sản phẩm không tồn tại, vẫn trả về order detail nhưng không có thông tin sản phẩm
                            OrderDetailDtoResponse orderDetailDto = OrderConvert.convertOrderDetailToOrderDetailDtoResponse(orderDetail);
                            orderDetailDtos.add(orderDetailDto);
                        }
                    }
                    
                    // Lấy thông tin user
                    Users user = userMapper.findById(order.getUser_id());
                    UserDtoResponse userDto = null;
                    if (user != null) {
                        userDto = UserConvert.convertUsersToUserDtoResponse(user);
                    }
                    
                    // Tạo response DTO
                    OrderDtoResponse response = OrderConvert.convertOrderToOrderDtoResponse(order, orderDetails);
                    response.setOrder_details(orderDetailDtos);
                    response.setUser(userDto); // Set thông tin user vào response
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

        // Nếu hủy đơn (CANCELLED), cập nhật payment status thành FAILED
        if (request.getStatus() == OrderSatus.CANCELLED) {
            try {
                paymentService.updatePaymentsByOrderId(orderId, PaymentStatus.FAILED);
                log.info("Đã cập nhật tất cả payments của order {} thành FAILED khi hủy đơn", orderId);
            } catch (Exception e) {
                log.warn("Không thể cập nhật payment khi hủy đơn {}: {}", orderId, e.getMessage());
                // Không throw exception để không rollback order cancellation
            }
        }

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

    @Override
    @Transactional
    public void deleteOrder(int orderId) {
        int currentUserId = authService.getCurrentUserId();
        String role = authService.getCurrentRole();
        
        // Kiểm tra đơn hàng tồn tại
        Orders order = orderMapper.findById(orderId);
        if (order == null) {
            throw new AppException(ErrorCode.LIST_NOT_FOUND);
        }

        // Kiểm tra quyền: Admin có thể xóa bất kỳ đơn hàng nào, User chỉ có thể xóa đơn hàng của mình
        if (authService.isUser(role) && order.getUser_id() != currentUserId) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        // Bước 1: Kiểm tra và lấy danh sách order details của order
        List<OrderDetails> orderDetails = orderDetailMapper.findByOrderId(orderId);
        if (orderDetails != null && !orderDetails.isEmpty()) {
            log.info("Bắt đầu xóa {} order details của order ID: {}", orderDetails.size(), orderId);
            // Bước 2: Xóa tất cả order details của order trước (soft delete)
            orderDetailMapper.deleteByOrderId(orderId);
            log.info("Đã xóa thành công tất cả order details của order ID: {}", orderId);
        } else {
            log.info("Order ID: {} không có order details nào", orderId);
        }

        // Bước 3: Sau khi xóa order details thành công, mới xóa order (soft delete)
        orderMapper.delete(orderId);
        log.info("Đã xóa thành công order ID: {}", orderId);
    }
}

