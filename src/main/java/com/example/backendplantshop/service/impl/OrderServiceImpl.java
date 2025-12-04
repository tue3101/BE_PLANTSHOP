package com.example.backendplantshop.service.impl;

import com.example.backendplantshop.convert.OrderConvert;
import com.example.backendplantshop.convert.PaymentConvert;
import com.example.backendplantshop.convert.UserConvert;
import com.example.backendplantshop.dto.request.*;
import com.example.backendplantshop.dto.request.momo.MoMoCallbackRequest;
import com.example.backendplantshop.dto.response.DepositDtoResponse;
import com.example.backendplantshop.dto.response.OrderDetailDtoResponse;
import com.example.backendplantshop.dto.response.OrderDtoResponse;
import com.example.backendplantshop.dto.response.momo.CreatePaymentResponse;
import com.example.backendplantshop.dto.response.user.UserDtoResponse;
import com.example.backendplantshop.entity.*;
import com.example.backendplantshop.enums.PaymentStatus;
import com.example.backendplantshop.enums.ErrorCode;
import com.example.backendplantshop.enums.OrderSatus;
import com.example.backendplantshop.enums.ShippingStatus;
import com.example.backendplantshop.exception.AppException;
import com.example.backendplantshop.mapper.*;
import com.example.backendplantshop.service.intf.DepositService;
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
    private static final int DEPOSIT_QUANTITY_THRESHOLD = 10;
    private final OrderMapper orderMapper;
    private final OrderDetailMapper orderDetailMapper;
    private final ProductMapper productMapper;
    private final DiscountMapper discountMapper;
    private final CartDetailMapper cartDetailMapper;
    private final AuthServiceImpl authService;
    private final PaymentMapper paymentMapper;
    private final PaymentMethodMapper paymentMethodMapper;
    private final DepositMapper depositMapper;
    private final PaymentService paymentService;
    private final UserMapper userMapper;
    private final DepositService depositService;

    @Override
    @Transactional //sử dụng transactional để rollback nếu xảy ra lỗi va commit nếu thành công
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

        // 3. Xử lý discount (chỉ lấy discount_id)
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

        // 4. Validate và tạo giá OrderDetails từ dữ liệu FE gửi (chưa thêm vào DB)
        List<OrderDetails> orderDetailsList = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (OrderDetailDtoRequest item : orderRequest.getItems()) {
            // Kiểm tra sản phẩm tồn tại
            Products product = productMapper.findById(item.getProduct_id());
            if (product == null) {
                throw new AppException(ErrorCode.PRODUCT_NOT_EXISTS);
            }

            // Kiểm tra số lượng ko được <=0
            if (item.getQuantity() <= 0) {
                throw new AppException(ErrorCode.INVALID_QUANTITY);
            }
            //số lượng ko được lớn hơn trong kho
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

        // 5. Tạo Order từ dữ liệu FE gửi (tạo order trong DB)
        Orders order = OrderConvert.convertOrderDtoRequestToOrders(orderRequest, currentUserId, discountId, now);
        orderMapper.insert(order);

        // 7. Tạo OrderDetails và set orderid cho từng order detail và cập nhật số lượng sản phẩm
        List<OrderDetailDtoResponse> orderDetailDtos = new ArrayList<>();
        for (OrderDetails orderDetail : orderDetailsList) {
            //1.tạo chi tiết đơn tương ứng với order_id
            orderDetail.setOrder_id(order.getOrder_id());
            orderDetailMapper.insert(orderDetail);  //thêm chi tiết đơn vào DB

            // 2.Cập nhật số lượng sản phẩm trong kho (trừ số lượng sau khi mua hàng)
            productMapper.updateProductQuantity(orderDetail.getProduct_id(), orderDetail.getQuantity());

            // 3.Lấy thông tin sản phẩm để trả về
            Products product = productMapper.findById(orderDetail.getProduct_id());
            OrderDetailDtoResponse orderDetailDto = OrderConvert.convertOrderDetailToOrderDetailDtoResponseWithProduct(
                    orderDetail, product);
            orderDetailDtos.add(orderDetailDto);
        }

        // 8. Tạo Payment khi FE gửi thông tin thanh toán
        if (orderRequest.getPayment() != null) {
            try {
                PaymentDtoRequest paymentRequest = orderRequest.getPayment();
                
                // Validate payment method
               PaymentMethod paymentMethod =
                    paymentMethodMapper.findById(paymentRequest.getMethod_id());
                if (paymentMethod == null) {
                    throw new AppException(ErrorCode.LIST_NOT_FOUND);
                }
                
                // Tạo payment cho đơn hàng
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
        enrichOrderResponseWithDeposit(response, orderDetailsList);  //bổ sung thông tin đặt cọc (nếu có)
        attachDepositPaymentLink(order, response);
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
        enrichOrderResponseWithDeposit(response, orderDetails);
        return response;
    }


    //lấy thông tin đơn hàng của user
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
                     enrichOrderResponseWithDeposit(response, orderDetails);
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
                     enrichOrderResponseWithDeposit(response, orderDetails);
                    response.setUser(userDto); // Set thông tin user vào response
                    return response;
                })
                .collect(Collectors.toList());
    }


    private void validateOrderStatusLogic(OrderSatus orderStatus, ShippingStatus shippingStatus, PaymentStatus paymentStatus) {
        // 1. PENDING_CONFIRMATION + SHIPPING/DELIVERED → Không hợp lý
        if (orderStatus == OrderSatus.PENDING_CONFIRMATION) {
            if (shippingStatus == ShippingStatus.SHIPPING || shippingStatus == ShippingStatus.DELIVERED) {
                throw new AppException(ErrorCode.INVALID_ORDER_STATUS_COMBINATION);
            }
        }

        // 2. PENDING_CONFIRMATION + PREPARING_ORDER → Không hợp lý (chưa xác nhận thì không thể chuẩn bị)
        if (orderStatus == OrderSatus.PENDING_CONFIRMATION && shippingStatus == ShippingStatus.PREPARING_ORDER) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS_COMBINATION);
        }

        // 3. PENDING_CONFIRMATION + payment SUCCESS → Cho phép
        // Lưu ý: Có thể xảy ra khi user thanh toán thành công nhưng admin chưa xác nhận đơn
        // Trong trường hợp này, user vẫn có thể hủy đơn và được hoàn tiền
        // Không cần validate rule này nữa

        // 4. CONFIRMED + DELIVERED + FAILED → Không hợp lý (giao hàng thành công mà thanh toán thất bại)
        if (orderStatus == OrderSatus.CONFIRMED && shippingStatus == ShippingStatus.DELIVERED && paymentStatus == PaymentStatus.FAILED) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS_COMBINATION);
        }

        // 5. CANCELLED + SHIPPING/DELIVERED/PREPARING_ORDER → Không hợp lý
        if (orderStatus == OrderSatus.CANCELLED) {
            if (shippingStatus == ShippingStatus.SHIPPING || 
                shippingStatus == ShippingStatus.DELIVERED || 
                shippingStatus == ShippingStatus.PREPARING_ORDER) {
                throw new AppException(ErrorCode.INVALID_ORDER_STATUS_COMBINATION);
            }
        }

        // 6. CANCELLED + payment SUCCESS → Cho phép nếu chưa giao hàng hoặc đã hủy giao hàng (cần hoàn tiền)
        // Cho phép hủy đơn khi đã thanh toán thành công nhưng chưa giao hàng hoặc đã hủy giao hàng
        // Áp dụng cho cả PENDING_CONFIRMATION và CONFIRMED
        if (orderStatus == OrderSatus.CANCELLED && paymentStatus == PaymentStatus.SUCCESS) {
            if (shippingStatus == ShippingStatus.SHIPPING || shippingStatus == ShippingStatus.DELIVERED) {
                // Đang giao hàng hoặc đã giao hàng thành công thì không cho phép hủy
                throw new AppException(ErrorCode.INVALID_ORDER_STATUS_COMBINATION);
            }
            // Cho phép hủy nếu chưa giao hàng (UNDELIVERED) hoặc đã hủy giao hàng (CANCELLED)
            // Sẽ hoàn tiền trong logic hủy đơn
        }

        // 7. CONFIRMED + CANCELLED (shipping) + SUCCESS → Cho phép (có hoàn tiền)
        // Cho phép hủy đơn khi đã xác nhận, đã thanh toán thành công nhưng đã hủy giao hàng
        // Logic này hợp lý vì đã có cơ chế hoàn tiền tự động
        // Không cần validate rule này nữa - đã được xử lý ở rule 6
    }


    private PaymentStatus getPaymentStatusFromOrder(int orderId) {
        List<Payment> payments = paymentMapper.findByOrderId(orderId);
        if (payments == null || payments.isEmpty()) {
            return PaymentStatus.PROCESSING; // Mặc định nếu chưa có payment
        }
        
        // Tìm payment có status SUCCESS trước, nếu không có thì lấy payment mới nhất
        Payment successPayment = payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                .findFirst()
                .orElse(null);
        
        if (successPayment != null) {
            return PaymentStatus.SUCCESS;
        }
        
        // Lấy payment mới nhất
        Payment latestPayment = payments.stream()
                .max((p1, p2) -> p1.getPayment_date().compareTo(p2.getPayment_date()))
                .orElse(payments.get(0));
        
        return latestPayment.getStatus();
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

        // Validate logic trước khi update
        PaymentStatus currentPaymentStatus = getPaymentStatusFromOrder(orderId);
        ShippingStatus currentShippingStatus = order.getShipping_status() != null 
                ? order.getShipping_status() 
                : ShippingStatus.UNDELIVERED;
        
        // Lưu trạng thái đơn hàng trước khi cập nhật để kiểm tra xem có cần cộng lại số lượng không
        OrderSatus previousOrderStatus = order.getStatus();
        
        validateOrderStatusLogic(request.getStatus(), currentShippingStatus, currentPaymentStatus);

        // Cập nhật status
        order.setStatus(request.getStatus());
        order.setUpdated_at(LocalDateTime.now());
        orderMapper.update(order);

        // Nếu hủy đơn (CANCELLED), xử lý hoàn tiền nếu đã thanh toán thành công
        if (request.getStatus() == OrderSatus.CANCELLED) {
            try {
                // Nếu đơn chưa được xác nhận (PENDING_CONFIRMATION), cộng lại số lượng sản phẩm vào kho
                if (previousOrderStatus == OrderSatus.PENDING_CONFIRMATION) {
                    List<OrderDetails> orderDetails = orderDetailMapper.findByOrderId(orderId);
                    for (OrderDetails orderDetail : orderDetails) {
                        productMapper.restoreProductQuantity(orderDetail.getProduct_id(), orderDetail.getQuantity());
                        log.info("Đã cộng lại {} sản phẩm (product_id: {}) vào kho khi hủy đơn {} (đơn chưa được xác nhận)",
                                orderDetail.getQuantity(), orderDetail.getProduct_id(), orderId);
                    }
                }
                
                // Nếu đã thanh toán thành công, cần hoàn tiền trước khi hủy
                if (currentPaymentStatus == PaymentStatus.SUCCESS) {
                    log.warn("Đơn hàng {} đã thanh toán thành công, cần hoàn tiền MoMo trước khi hủy", orderId);
                    log.warn("LƯU Ý: Cần gọi MoMo Refund API với transId từ callback để hoàn tiền cho khách hàng");
                    log.warn("Sau khi hoàn tiền thành công, payment status sẽ được cập nhật thành FAILED");
                    // TODO: Implement MoMo refund API call here nếu cần tự động hoàn tiền
                }
                
                // Cập nhật payment status thành FAILED khi hủy đơn
                paymentService.updatePaymentsByOrderId(orderId, PaymentStatus.FAILED);
                log.info("Đã cập nhật tất cả payments của order {} thành FAILED khi hủy đơn", orderId);
                
                // Cập nhật shipping status thành UNDELIVERED khi hủy đơn
                if (order.getShipping_status() != ShippingStatus.UNDELIVERED) {
                    order.setShipping_status(ShippingStatus.UNDELIVERED);
                    orderMapper.update(order);
                    log.info("Đã cập nhật shipping_status của order {} thành UNDELIVERED khi hủy đơn", orderId);
                }
            } catch (Exception e) {
                log.warn("Không thể cập nhật payment/shipping khi hủy đơn {}: {}", orderId, e.getMessage());
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
        enrichOrderResponseWithDeposit(response, orderDetails);  //bổ sung thông tin đặt cọc (nếu có)
        return response;
    }

    @Override
    @Transactional
    public OrderDtoResponse updateShippingStatus(int orderId, UpdateShippingStatusDtoRequest request) {
        int currentUserId = authService.getCurrentUserId();
        String role = authService.getCurrentRole();

        Orders order = orderMapper.findById(orderId);
        if (order == null) {
            throw new AppException(ErrorCode.LIST_NOT_FOUND);
        }

        // Kiểm tra quyền: Admin có thể cập nhật bất kỳ đơn hàng nào, User chỉ có thể cập nhật đơn hàng của mình
        if (authService.isUser(role) && order.getUser_id() != currentUserId) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        // Validate shipping_status
        if (request.getShipping_status() == null) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD);
        }

        // Logic validation: Nếu đơn hàng chưa ở trạng thái CONFIRMED thì shipping_status chỉ được là UNDELIVERED
        if (order.getStatus() != OrderSatus.CONFIRMED) {
            if (request.getShipping_status() != ShippingStatus.UNDELIVERED) {
                throw new AppException(ErrorCode.INVALID_ORDER_STATUS_COMBINATION);
            }
        }

        // Validate logic với payment status trước khi update
        PaymentStatus currentPaymentStatus = getPaymentStatusFromOrder(orderId);
        validateOrderStatusLogic(order.getStatus(), request.getShipping_status(), currentPaymentStatus);

        // Cập nhật shipping_status
        order.setShipping_status(request.getShipping_status());
        order.setUpdated_at(LocalDateTime.now());
        orderMapper.update(order);

        log.info("Đã cập nhật shipping_status của order {} thành {}", orderId, request.getShipping_status());

        if (request.getShipping_status() == ShippingStatus.DELIVERED) {
            markCodPaymentsAsSuccess(orderId);
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
        enrichOrderResponseWithDeposit(response, orderDetails); //bổ sung thông tin đặt cọc (nếu có)
        return response;
    }

    private void enrichOrderResponseWithDeposit(OrderDtoResponse response, List<OrderDetails> orderDetails) {
        if (response == null) {
            return;
        }
        response.setDeposit_payment(null);
        boolean requiresDeposit = requiresDeposit(orderDetails);
        response.setDeposit_required(requiresDeposit);

        Deposit deposit = depositMapper.findLatestByOrderId(response.getOrder_id());
        if (deposit != null) {
            PaymentMethod method = paymentMethodMapper.findById(deposit.getMethod_id());
            DepositDtoResponse depositDto = DepositDtoResponse.builder()
                    .deposit_id(deposit.getDeposit_id())
                    .order_id(deposit.getOrder_id())
                    .method_id(deposit.getMethod_id())
                    .method_name(method != null ? method.getMethod_name() : null)
                    .amount(deposit.getAmount())
                    .paid(deposit.getPaid())
                    .momo_trans_id(deposit.getMomo_trans_id())
                    .created_at(deposit.getCreated_at())
                    .paid_at(deposit.getPaid_at())
                    .build();
            response.setDeposit(depositDto);
        } else {
            response.setDeposit(null);
        }
    }

    private boolean requiresDeposit(List<OrderDetails> orderDetails) {
        if (orderDetails == null || orderDetails.isEmpty()) {
            return false;
        }
        int totalQuantity = orderDetails.stream()
                .mapToInt(OrderDetails::getQuantity)
                .sum();
        return totalQuantity >= DEPOSIT_QUANTITY_THRESHOLD;
    }

    private void attachDepositPaymentLink(Orders order, OrderDtoResponse response) {
        if (order == null || response == null) {
            return;
        }

        if (!Boolean.TRUE.equals(response.getDeposit_required())) {
            return;
        }

        if (response.getDeposit() != null && Boolean.TRUE.equals(response.getDeposit().getPaid())) {
            return;
        }

        try {
            CreatePaymentResponse depositPayment = depositService.createDepositPayment(order.getOrder_id());
            response.setDeposit_payment(depositPayment);
        } catch (AppException ex) {
            log.warn("Không thể tạo thanh toán đặt cọc tự động cho order {}: {}", order.getOrder_id(), ex.getMessage());
        } catch (Exception ex) {
            log.error("Lỗi khi tạo thanh toán đặt cọc cho order {}: {}", order.getOrder_id(), ex.getMessage(), ex);
        }
    }

    private void markCodPaymentsAsSuccess(int orderId) {
        List<Payment> payments = paymentMapper.findByOrderId(orderId);
        if (payments == null || payments.isEmpty()) {
            return;
        }

        for (Payment payment : payments) {
            PaymentMethod method = paymentMethodMapper.findById(payment.getMethod_id());
            if (!isCodMethod(method != null ? method.getMethod_name() : null)) {
                continue;
            }

            if (payment.getStatus() == PaymentStatus.SUCCESS) {
                continue;
            }

            try {
                paymentService.updatePaymentStatus(payment.getPayment_id(), PaymentStatus.SUCCESS);
                log.info("Đã tự động chuyển payment {} của order {} sang SUCCESS vì đơn COD đã giao thành công",
                        payment.getPayment_id(), orderId);
            } catch (Exception ex) {
                log.warn("Không thể cập nhật payment {} của order {} sang SUCCESS: {}", payment.getPayment_id(), orderId, ex.getMessage());
            }
        }
    }

    private boolean isCodMethod(String methodName) {
        if (methodName == null) {
            return false;
        }
        String normalized = methodName.trim().toLowerCase();
        return normalized.equals("cod")
                || normalized.contains("cod")
                || normalized.contains("cash on delivery")
                || normalized.contains("thanh toán khi nhận");
    }

//    @Override
//    @Transactional
//    public OrderDtoResponse updateShippingInfo(int orderId, UpdateShippingInfoDtoRequest request) {
//        int currentUserId = authService.getCurrentUserId();
//        String role = authService.getCurrentRole();
//
//        Orders order = orderMapper.findById(orderId);
//        if (order == null) {
//            throw new AppException(ErrorCode.LIST_NOT_FOUND);
//        }
//
//        // Kiểm tra quyền: Admin có thể cập nhật bất kỳ đơn hàng nào, User chỉ có thể cập nhật đơn hàng của mình
//        if (authService.isUser(role) && order.getUser_id() != currentUserId) {
//            throw new AppException(ErrorCode.ACCESS_DENIED);
//        }
//
//        // Validate các trường shipping info
//        if (request.getShipping_name() == null || request.getShipping_name().trim().isEmpty()) {
//            throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD);
//        }
//        if (request.getShipping_address() == null || request.getShipping_address().trim().isEmpty()) {
//            throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD);
//        }
//        if (request.getShipping_phone() == null || request.getShipping_phone().trim().isEmpty()) {
//            throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD);
//        }
//
//        // Chỉ cập nhật 3 trường shipping info, giữ nguyên các trường khác
//        // Sử dụng method updateShippingInfo riêng để tối ưu (chỉ update 3 trường này)
//        orderMapper.updateShippingInfo(
//                orderId,
//                request.getShipping_name(),
//                request.getShipping_address(),
//                request.getShipping_phone()
//        );
//
//        // Cập nhật lại object order để trả về response đúng
//        order.setShipping_name(request.getShipping_name());
//        order.setShipping_address(request.getShipping_address());
//        order.setShipping_phone(request.getShipping_phone());
//
//        log.info("Đã cập nhật thông tin giao hàng của order {}: name={}, address={}, phone={}",
//                orderId, request.getShipping_name(), request.getShipping_address(), request.getShipping_phone());
//
//        // Lấy order details để trả về
//        List<OrderDetails> orderDetails = orderDetailMapper.findByOrderId(orderId);
//        List<OrderDetailDtoResponse> orderDetailDtos = new ArrayList<>();
//
//        for (OrderDetails orderDetail : orderDetails) {
//            Products product = productMapper.findById(orderDetail.getProduct_id());
//            if (product != null) {
//                OrderDetailDtoResponse orderDetailDto = OrderConvert.convertOrderDetailToOrderDetailDtoResponseWithProduct(
//                        orderDetail, product);
//                orderDetailDtos.add(orderDetailDto);
//            } else {
//                OrderDetailDtoResponse orderDetailDto = OrderConvert.convertOrderDetailToOrderDetailDtoResponse(orderDetail);
//                orderDetailDtos.add(orderDetailDto);
//            }
//        }
//
//        OrderDtoResponse response = OrderConvert.convertOrderToOrderDtoResponse(order, orderDetails);
//        response.setOrder_details(orderDetailDtos);
//        return response;
//    }

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

    //hàm set trạng thái giao dịch thành công nếu thanh toán thành công từ momo
    @Override
    @Transactional
    public void handleOrderPaymentCallback(Integer orderId, MoMoCallbackRequest callbackRequest) {
        if (callbackRequest.getResultCode() != null && callbackRequest.getResultCode() == 0) {
            try {
                if (orderId != null) {
                    paymentService.updatePaymentsByOrderId(orderId, PaymentStatus.SUCCESS);
                    UpdateOrderStatusDtoRequest statusRequest = UpdateOrderStatusDtoRequest.builder()
                                    .status(OrderSatus.CONFIRMED)
                                    .build();
                    updateOrderStatus(orderId, statusRequest);
                    log.info("Đã cập nhật trạng thái đơn hàng {} thành công sau khi thanh toán", orderId);
                }
            } catch (Exception e) {
                log.error("Lỗi khi cập nhật trạng thái đơn hàng từ callback: {}", e.getMessage(), e);
            }
        } else {
            log.warn("Thanh toán thất bại: orderId={}, message={}",
                    callbackRequest.getOrderId(), callbackRequest.getMessage());
            if (orderId != null) {
                paymentService.updatePaymentsByOrderId(orderId, PaymentStatus.FAILED);
            }
        }
    }
}

