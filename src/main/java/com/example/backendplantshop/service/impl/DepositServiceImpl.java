package com.example.backendplantshop.service.impl;

import com.example.backendplantshop.dto.request.momo.CreatePaymentRequest;
import com.example.backendplantshop.dto.response.DepositDtoResponse;
import com.example.backendplantshop.dto.response.momo.CreatePaymentResponse;
import com.example.backendplantshop.entity.Deposit;
import com.example.backendplantshop.entity.OrderDetails;
import com.example.backendplantshop.entity.Orders;
import com.example.backendplantshop.entity.PaymentMethod;
import com.example.backendplantshop.enums.ErrorCode;
import com.example.backendplantshop.enums.MoMoPaymentPurpose;
import com.example.backendplantshop.exception.AppException;
import com.example.backendplantshop.mapper.DepositMapper;
import com.example.backendplantshop.mapper.OrderDetailMapper;
import com.example.backendplantshop.mapper.OrderMapper;
import com.example.backendplantshop.mapper.PaymentMethodMapper;
import com.example.backendplantshop.service.intf.DepositService;
import com.example.backendplantshop.service.intf.MoMoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepositServiceImpl implements DepositService {

    private static final int DEPOSIT_QUANTITY_THRESHOLD = 10;
    private static final BigDecimal DEPOSIT_RATIO = new BigDecimal("0.5");
    private static final String MOMO_METHOD_KEYWORD = "momo";

    private final DepositMapper depositMapper;
    private final OrderMapper orderMapper;
    private final OrderDetailMapper orderDetailMapper;
    private final PaymentMethodMapper paymentMethodMapper;
    private final MoMoService moMoService;
    private final AuthServiceImpl authService;

    @Override
    @Transactional
    public CreatePaymentResponse createDepositPayment(int orderId) {
        Orders order = validateOrderOwnership(orderId);
        if (!requiresDeposit(orderId)) {
            throw new AppException(ErrorCode.DEPOSIT_NOT_REQUIRED);
        }

        Deposit existing = depositMapper.findLatestByOrderId(orderId);
        if (existing != null && Boolean.TRUE.equals(existing.getPaid())) {
            throw new AppException(ErrorCode.DEPOSIT_ALREADY_PAID);
        }

        BigDecimal depositAmount = calculateDepositAmount(order);
        PaymentMethod momoMethod = resolveMoMoPaymentMethod();

        // Tạo deposit record với paid = 0 (chưa đặt cọc) khi tạo payment request
        // Chỉ tạo mới nếu chưa có deposit record nào
        if (existing == null) {
            Deposit deposit = Deposit.builder()
                    .order_id(orderId)
                    .method_id(momoMethod.getMethod_id())
                    .amount(depositAmount)
                    .paid(Boolean.FALSE) // Chưa đặt cọc
                    .momo_trans_id(null) // Chưa có transId
                    .created_at(LocalDateTime.now())
                    .paid_at(null) // Chưa thanh toán
                    .build();

            depositMapper.insert(deposit);
            log.info("Đã tạo deposit record cho order {} với paid = false", orderId);
        } else {
            // Đã có deposit record với paid = 0, không cần tạo mới
            log.info("Đã tồn tại deposit record chưa thanh toán cho order {}, không tạo mới", orderId);
        }

        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .orderId(orderId)
                .amount(depositAmount)
                .orderInfo("Đặt cọc đơn hàng #" + orderId)
                .purpose(MoMoPaymentPurpose.DEPOSIT)
                .build();

        return moMoService.createPayment(request);
    }

    @Override
    public DepositDtoResponse getDepositByOrderId(int orderId) {
        Orders order = orderMapper.findById(orderId);
        if (order == null) {
            throw new AppException(ErrorCode.LIST_NOT_FOUND);
        }
        enforceOwnership(order.getUser_id());

        Deposit deposit = depositMapper.findLatestByOrderId(orderId);
        if (deposit == null) {
            return null;
        }

        PaymentMethod method = paymentMethodMapper.findById(deposit.getMethod_id());
        return convertDepositToDto(deposit, method);
    }

    @Override
    @Transactional
    public void handleDepositSuccess(int orderId, Long amount, Long transId) {
        if (orderId <= 0) {
            return;
        }

        if (transId == null) {
            log.warn("Không nhận được transId từ MoMo cho giao dịch đặt cọc order {}", orderId);
            return;
        }

        String momoTransId = String.valueOf(transId);

        // Kiểm tra xem giao dịch này đã được xử lý chưa
        Deposit duplicateCheck = depositMapper.findByMomoTransId(momoTransId);
        if (duplicateCheck != null) {
            log.info("Đặt cọc cho giao dịch {} đã được ghi nhận trước đó", transId);
            return;
        }

        // Tìm deposit record chưa thanh toán (paid = 0) cho order này
        Deposit existingDeposit = depositMapper.findLatestByOrderId(orderId);
        
        if (existingDeposit != null && Boolean.FALSE.equals(existingDeposit.getPaid())) {
            // Cập nhật deposit record đã tồn tại
            existingDeposit.setPaid(Boolean.TRUE);
            existingDeposit.setMomo_trans_id(momoTransId);
            existingDeposit.setPaid_at(LocalDateTime.now());
            if (amount != null) {
                existingDeposit.setAmount(BigDecimal.valueOf(amount));
            }
            depositMapper.update(existingDeposit);
            log.info("Đã cập nhật deposit record cho order {} thành công với giao dịch {}", orderId, transId);
        } else {
            // Nếu không có deposit record, tạo mới (trường hợp hiếm)
            PaymentMethod momoMethod = resolveMoMoPaymentMethod();
            Deposit deposit = Deposit.builder()
                    .order_id(orderId)
                    .method_id(momoMethod.getMethod_id())
                    .amount(amount != null ? BigDecimal.valueOf(amount) : BigDecimal.ZERO)
                    .paid(Boolean.TRUE)
                    .momo_trans_id(momoTransId)
                    .created_at(LocalDateTime.now())
                    .paid_at(LocalDateTime.now())
                    .build();
            depositMapper.insert(deposit);
            log.info("Đã tạo mới deposit record cho order {} với giao dịch {}", orderId, transId);
        }
    }

    @Override
    public boolean requiresDeposit(int orderId) {
        List<OrderDetails> orderDetails = orderDetailMapper.findByOrderId(orderId);
        if (orderDetails == null || orderDetails.isEmpty()) {
            return false;
        }
        int totalQuantity = orderDetails.stream()
                .mapToInt(OrderDetails::getQuantity)
                .sum();
        return totalQuantity >= DEPOSIT_QUANTITY_THRESHOLD;
    }

    private Orders validateOrderOwnership(int orderId) {
        Orders order = orderMapper.findById(orderId);
        if (order == null) {
            throw new AppException(ErrorCode.LIST_NOT_FOUND);
        }
        enforceOwnership(order.getUser_id());
        return order;
    }

    private void enforceOwnership(int ownerId) {
        String role = authService.getCurrentRole();
        int currentUserId = authService.getCurrentUserId();
        if (!authService.isAdmin(role) && currentUserId != ownerId) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
    }

    private BigDecimal calculateDepositAmount(Orders order) {
        BigDecimal discountAmount = order.getDiscount_amount() != null ? order.getDiscount_amount() : BigDecimal.ZERO;
        return order.getTotal()
                .subtract(discountAmount)
                .multiply(DEPOSIT_RATIO)
                .setScale(0, RoundingMode.HALF_UP);
    }

    private PaymentMethod resolveMoMoPaymentMethod() {
        PaymentMethod method = paymentMethodMapper.findByName(MOMO_METHOD_KEYWORD);
        if (method == null) {
            throw new AppException(ErrorCode.DEPOSIT_METHOD_NOT_FOUND);
        }
        return method;
    }

    private DepositDtoResponse convertDepositToDto(Deposit deposit, PaymentMethod method) {
        return DepositDtoResponse.builder()
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
    }
}


