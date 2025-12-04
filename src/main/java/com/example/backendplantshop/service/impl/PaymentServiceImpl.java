package com.example.backendplantshop.service.impl;

import com.example.backendplantshop.convert.PaymentConvert;
import com.example.backendplantshop.dto.request.PaymentDtoRequest;
import com.example.backendplantshop.dto.request.momo.MoMoCallbackRequest;
import com.example.backendplantshop.dto.response.PaymentDtoResponse;
import com.example.backendplantshop.dto.response.PaymentMethodDtoResponse;
import com.example.backendplantshop.entity.Orders;
import com.example.backendplantshop.entity.Payment;
import com.example.backendplantshop.entity.PaymentMethod;
import com.example.backendplantshop.enums.ErrorCode;
import com.example.backendplantshop.enums.OrderSatus;
import com.example.backendplantshop.enums.PaymentStatus;
import com.example.backendplantshop.enums.ShippingStatus;
import com.example.backendplantshop.exception.AppException;
import com.example.backendplantshop.mapper.OrderMapper;
import com.example.backendplantshop.mapper.PaymentMapper;
import com.example.backendplantshop.mapper.PaymentMethodMapper;
import com.example.backendplantshop.service.intf.DepositService;
import com.example.backendplantshop.service.intf.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    
    private final PaymentMapper paymentMapper;
    private final PaymentMethodMapper paymentMethodMapper;
    private final OrderMapper orderMapper;
    private final DepositService depositService;
    
    @Override
    @Transactional
    public PaymentDtoResponse createPayment(PaymentDtoRequest request, int orderId) {
        // Validate payment method
        PaymentMethod paymentMethod = paymentMethodMapper.findById(request.getMethod_id());
        if (paymentMethod == null) {
            throw new AppException(ErrorCode.LIST_NOT_FOUND);
        }
        
        // Validate amount
        //compareTo: so sánh 2 giá trị
        //amount ko được nhỏ hơn hoặc bằng 0
        if (request.getAmount() == null || request.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD);
        }
        
        // Create payment
        LocalDateTime now = LocalDateTime.now();
        Payment payment = PaymentConvert.convertPaymentDtoRequestToPayment(request, orderId, now);
        paymentMapper.insert(payment);
        
        log.info("Đã tạo payment với ID: {} cho order ID: {}", payment.getPayment_id(), orderId);
        
        // Return response
        return PaymentConvert.convertPaymentToPaymentDtoResponse(payment, paymentMethod);
    }
    
    @Override
    public PaymentDtoResponse getPaymentById(int paymentId) {
        Payment payment = paymentMapper.findById(paymentId);
        if (payment == null) {
            throw new AppException(ErrorCode.LIST_NOT_FOUND);
        }
        
        PaymentMethod paymentMethod = paymentMethodMapper.findById(payment.getMethod_id());
        return PaymentConvert.convertPaymentToPaymentDtoResponse(payment, paymentMethod);
    }

    //lấy giao dịch bằng mã đơn để hiển thi thong tin giao dịch ở lịch su don và danh sach don hàng
    @Override
    public List<PaymentDtoResponse> getPaymentsByOrderId(int orderId) {
        List<Payment> payments = paymentMapper.findByOrderId(orderId);
        return payments.stream()
                .map(payment -> {
                    PaymentMethod paymentMethod = paymentMethodMapper.findById(payment.getMethod_id());
                    return PaymentConvert.convertPaymentToPaymentDtoResponse(payment, paymentMethod);
                })
                .collect(Collectors.toList());
    }


    //lấy tất cả giao dịch cho admin
    @Override
    public List<PaymentDtoResponse> getAllPayments() {
        List<Payment> payments = paymentMapper.getAll();
        return payments.stream()
                .map(payment -> {
                    PaymentMethod paymentMethod = paymentMethodMapper.findById(payment.getMethod_id());
                    return PaymentConvert.convertPaymentToPaymentDtoResponse(payment, paymentMethod);
                })
                .collect(Collectors.toList());
    }

    //lấy tất cả pthuc thanh toán
    @Override
    public List<PaymentMethodDtoResponse> getAllPaymentMethods() {
        List<PaymentMethod> paymentMethods = paymentMethodMapper.getAll();
        return paymentMethods.stream()
                .map(PaymentConvert::convertPaymentMethodToPaymentMethodDtoResponse)
                .collect(Collectors.toList());
    }
    


    //các trường hợp trạng thái ko hợp lệ
    private void validateOrderStatusLogic(OrderSatus orderStatus, ShippingStatus shippingStatus, PaymentStatus paymentStatus) {
        // 1. PENDING_CONFIRMATION + SHIPPING/DELIVERED → Không hợp lý
        if (orderStatus == OrderSatus.PENDING_CONFIRMATION) {
            if (shippingStatus == ShippingStatus.SHIPPING || shippingStatus == ShippingStatus.DELIVERED||shippingStatus ==  ShippingStatus.PREPARING_ORDER) {
                throw new AppException(ErrorCode.INVALID_ORDER_STATUS_COMBINATION);
            }
        }

        // 2. PENDING_CONFIRMATION + PREPARING_ORDER → Không hợp lý
//        if (orderStatus == OrderSatus.PENDING_CONFIRMATION && shippingStatus == ShippingStatus.PREPARING_ORDER) {
//            throw new AppException(ErrorCode.INVALID_ORDER_STATUS_COMBINATION);
//        }

        // 3. PENDING_CONFIRMATION + payment SUCCESS → Không hợp lý (đơn chưa xác nhận thì không thể thanh toán thành công)
        if (orderStatus == OrderSatus.PENDING_CONFIRMATION && paymentStatus == PaymentStatus.SUCCESS) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS_COMBINATION);
        }

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

        // 6. CANCELLED + payment SUCCESS → Không hợp lý (đơn đã hủy thì không thể thanh toán thành công)
        if (orderStatus == OrderSatus.CANCELLED && paymentStatus == PaymentStatus.SUCCESS) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS_COMBINATION);
        }

        // 7. CONFIRMED + CANCELLED (shipping) + SUCCESS → Không hợp lý
        if (orderStatus == OrderSatus.CONFIRMED && shippingStatus == ShippingStatus.CANCELLED && paymentStatus == PaymentStatus.SUCCESS) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS_COMBINATION);
        }
    }
    
    @Override
    @Transactional
    public PaymentDtoResponse updatePaymentStatus(int paymentId, PaymentStatus status) {
        Payment payment = paymentMapper.findById(paymentId);
        if (payment == null) {
            throw new AppException(ErrorCode.LIST_NOT_FOUND);
        }
        
        // Lấy thông tin order để validate
        Orders order = orderMapper.findById(payment.getOrder_id());
        if (order == null) {
            throw new AppException(ErrorCode.LIST_NOT_FOUND);
        }
        
        // Lấy payment status mới sau khi update (tạm thời dùng status mới)
        // Lưu ý: Cần lấy payment status từ tất cả payments của order
//        List<Payment> allPayments = paymentMapper.findByOrderId(payment.getOrder_id());
        PaymentStatus effectivePaymentStatus = status; // Status mới sẽ được set

//        if (status != PaymentStatus.SUCCESS) {
//            Payment successPayment = allPayments.stream()
//                    .filter(p -> p.getPayment_id() != paymentId && p.getStatus() == PaymentStatus.SUCCESS)
//                    .findFirst()
//                    .orElse(null);
//            if (successPayment != null) {
//                effectivePaymentStatus = PaymentStatus.SUCCESS;
//            }
//        }
        
        // Validate logic trước khi update
        ShippingStatus currentShippingStatus = order.getShipping_status() != null 
                ? order.getShipping_status() 
                : ShippingStatus.UNDELIVERED;
        
        validateOrderStatusLogic(order.getStatus(), currentShippingStatus, effectivePaymentStatus);
        
        // Cập nhật status
        payment.setStatus(status);
        payment.setPayment_date(LocalDateTime.now()); // Cập nhật lại payment_date
        paymentMapper.update(payment);
        
        log.info("Đã cập nhật payment ID: {} thành status: {}", paymentId, status);
        
        PaymentMethod paymentMethod = paymentMethodMapper.findById(payment.getMethod_id());
        return PaymentConvert.convertPaymentToPaymentDtoResponse(payment, paymentMethod);
    }


    //hàm dùng để  update status khi thanh toán ko thành công/thah công
    @Override
    //đảm bảo commit trạng thái của giao dịch của đơn hàng dù cho trạng thais đơn bị lỗi
    //requires_new nó sẽ tách ra  một transaction mới chạy riêng biệt với transaction của order
    //propagation = Propagation.REQUIRES_NEW → luôn tạo một transaction mới, bất kể có transaction hiện tại hay không.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updatePaymentsByOrderId(int orderId, PaymentStatus status) {
        List<Payment> payments = paymentMapper.findByOrderId(orderId);
        
        if (payments.isEmpty()) {
            log.info("Không tìm thấy payment nào cho order ID: {}", orderId);
            return;
        }
        
        // Cập nhật tất cả payments của order
        for (Payment payment : payments) {
            payment.setStatus(status);
            payment.setPayment_date(LocalDateTime.now());
            paymentMapper.update(payment);
            log.info("Đã cập nhật payment ID: {} của order ID: {} thành status: {}", 
                    payment.getPayment_id(), orderId, status);
        }
    }
    
    @Override
    public Integer extractOrderIdFromMoMoOrderId(String momoOrderId) {
        if (momoOrderId == null || momoOrderId.isBlank()) {
            return null;
        }
        try {
            String normalized = momoOrderId.trim();
            if (normalized.startsWith("ORDER_") || normalized.startsWith("DEPOSIT_")) {
                String[] parts = normalized.split("_");
                if (parts.length >= 2) {
                    return Integer.parseInt(parts[1]);
                }
            }
            return Integer.parseInt(normalized);
        } catch (NumberFormatException ex) {
            log.error("Không thể parse orderId từ {}: {}", momoOrderId, ex.getMessage());
            return null;
        }
    }


    //hàm đánh dấu đơn hàng đã được đặt cọc hay chưa
    @Override
    @Transactional
    public void handleDepositCallback(Integer orderId, MoMoCallbackRequest callbackRequest) {
        if (orderId == null) {
            log.warn("Không xác định được orderId cho giao dịch đặt cọc");
            return;
        }
        if (callbackRequest.getResultCode() != null && callbackRequest.getResultCode() == 0) {
            // Thanh toán thành công: cập nhật deposit record thành paid = 1
            depositService.handleDepositSuccess(orderId, callbackRequest.getAmount(), callbackRequest.getTransId());
        } else {
            // Thanh toán thất bại: deposit record đã được tạo với paid = 0 khi tạo payment request
            // Không cần làm gì thêm, deposit record đã tồn tại với paid = 0
            log.warn("Đặt cọc thất bại cho order {}: {}. Deposit record vẫn tồn tại với paid = 0", 
                    orderId, callbackRequest.getMessage());
        }
    }
}

