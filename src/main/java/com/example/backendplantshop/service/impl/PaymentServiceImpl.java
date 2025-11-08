package com.example.backendplantshop.service.impl;

import com.example.backendplantshop.convert.PaymentConvert;
import com.example.backendplantshop.dto.request.PaymentDtoRequest;
import com.example.backendplantshop.dto.response.PaymentDtoResponse;
import com.example.backendplantshop.dto.response.PaymentMethodDtoResponse;
import com.example.backendplantshop.entity.Payment;
import com.example.backendplantshop.entity.PaymentMethod;
import com.example.backendplantshop.enums.ErrorCode;
import com.example.backendplantshop.enums.PaymentStatus;
import com.example.backendplantshop.exception.AppException;
import com.example.backendplantshop.mapper.PaymentMapper;
import com.example.backendplantshop.mapper.PaymentMethodMapper;
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
    
    @Override
    @Transactional
    public PaymentDtoResponse createPayment(PaymentDtoRequest request, int orderId) {
        // Validate payment method
        PaymentMethod paymentMethod = paymentMethodMapper.findById(request.getMethod_id());
        if (paymentMethod == null) {
            throw new AppException(ErrorCode.LIST_NOT_FOUND);
        }
        
        // Validate amount
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
    
    @Override
    public List<PaymentMethodDtoResponse> getAllPaymentMethods() {
        List<PaymentMethod> paymentMethods = paymentMethodMapper.getAll();
        return paymentMethods.stream()
                .map(PaymentConvert::convertPaymentMethodToPaymentMethodDtoResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public PaymentDtoResponse updatePaymentStatus(int paymentId, PaymentStatus status) {
        Payment payment = paymentMapper.findById(paymentId);
        if (payment == null) {
            throw new AppException(ErrorCode.LIST_NOT_FOUND);
        }
        
        // Cập nhật status
        payment.setStatus(status);
        payment.setPayment_date(LocalDateTime.now()); // Cập nhật lại payment_date
        paymentMapper.update(payment);
        
        log.info("Đã cập nhật payment ID: {} thành status: {}", paymentId, status);
        
        PaymentMethod paymentMethod = paymentMethodMapper.findById(payment.getMethod_id());
        return PaymentConvert.convertPaymentToPaymentDtoResponse(payment, paymentMethod);
    }
    
    @Override
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
}

