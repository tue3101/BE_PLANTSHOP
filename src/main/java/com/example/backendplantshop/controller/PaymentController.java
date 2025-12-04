package com.example.backendplantshop.controller;

import com.example.backendplantshop.dto.request.PaymentDtoRequest;
import com.example.backendplantshop.dto.request.momo.CreatePaymentRequest;
import com.example.backendplantshop.dto.request.momo.MoMoCallbackRequest;
import com.example.backendplantshop.dto.response.ApiResponse;
import com.example.backendplantshop.dto.response.PaymentDtoResponse;
import com.example.backendplantshop.dto.response.momo.CreatePaymentResponse;
import com.example.backendplantshop.enums.ErrorCode;
import com.example.backendplantshop.enums.MoMoPaymentPurpose;
import com.example.backendplantshop.config.MoMoConfig;
import com.example.backendplantshop.service.intf.MoMoService;
import com.example.backendplantshop.service.intf.OrderService;
import com.example.backendplantshop.service.intf.PaymentService;
import com.example.backendplantshop.service.impl.AuthServiceImpl;
import com.example.backendplantshop.util.MoMoUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    
    private final PaymentService paymentService;
    private final MoMoService momoService;
    private final OrderService orderService;
    private final AuthServiceImpl authService;
    private final MoMoConfig momoConfig;
    
    @PostMapping("/create/{orderId}")
    public ApiResponse<PaymentDtoResponse> createPayment(
            @PathVariable("orderId") int orderId,
            @Valid @RequestBody PaymentDtoRequest request) {
        // Kiểm tra quyền truy cập
        String role = authService.getCurrentRole();
        if (!authService.isUser(role) && !authService.isAdmin(role)) {
            throw new com.example.backendplantshop.exception.AppException(ErrorCode.ACCESS_DENIED);
        }

        return ApiResponse.<PaymentDtoResponse>builder()
                .statusCode(ErrorCode.ADD_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.ADD_SUCCESSFULL.getMessage())
                .data(paymentService.createPayment(request, orderId))
                .build();
    }
    
    @GetMapping("/{paymentId}")
    public ApiResponse<PaymentDtoResponse> getPaymentById(@PathVariable("paymentId") int paymentId) {
        PaymentDtoResponse payment = paymentService.getPaymentById(paymentId);
        return ApiResponse.<PaymentDtoResponse>builder()
                .statusCode(ErrorCode.CALL_API_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.CALL_API_SUCCESSFULL.getMessage())
                .data(payment)
                .build();
    }
    
    @GetMapping("/order/{orderId}")
    public ApiResponse<List<PaymentDtoResponse>> getPaymentsByOrderId(@PathVariable("orderId") int orderId) {
        List<PaymentDtoResponse> payments = paymentService.getPaymentsByOrderId(orderId);
        return ApiResponse.<List<PaymentDtoResponse>>builder()
                .statusCode(ErrorCode.CALL_API_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.CALL_API_SUCCESSFULL.getMessage())
                .data(payments)
                .build();
    }
    
    @GetMapping("/get-all")
    public ApiResponse<List<PaymentDtoResponse>> getAllPayments() {
        // Chỉ admin mới được xem tất cả payments
        String role = authService.getCurrentRole();
        if (!authService.isAdmin(role)) {
            throw new com.example.backendplantshop.exception.AppException(ErrorCode.ACCESS_DENIED);
        }
        
        List<PaymentDtoResponse> payments = paymentService.getAllPayments();
        return ApiResponse.<List<PaymentDtoResponse>>builder()
                .statusCode(ErrorCode.CALL_API_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.CALL_API_SUCCESSFULL.getMessage())
                .data(payments)
                .build();
    }
    
    @PutMapping("/{paymentId}/status")
    public ApiResponse<PaymentDtoResponse> updatePaymentStatus(
            @PathVariable("paymentId") int paymentId,
            @RequestParam("status") com.example.backendplantshop.enums.PaymentStatus status) {
        // Kiểm tra quyền truy cập
        String role = authService.getCurrentRole();
        if (!authService.isAdmin(role)) {
            throw new com.example.backendplantshop.exception.AppException(ErrorCode.ACCESS_DENIED);
        }
        
        PaymentDtoResponse payment = paymentService.updatePaymentStatus(paymentId, status);
        return ApiResponse.<PaymentDtoResponse>builder()
                .statusCode(ErrorCode.UPDATE_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.UPDATE_SUCCESSFULL.getMessage())
                .data(payment)
                .build();
    }
    
    /**
     * Tạo payment request với MoMo
     */
    @PostMapping("/momo/create")
    public ApiResponse<CreatePaymentResponse> createMoMoPayment(@Valid @RequestBody CreatePaymentRequest request) {
        // Kiểm tra quyền truy cập
        String role = authService.getCurrentRole();
        if (!authService.isUser(role) && !authService.isAdmin(role)) {
            throw new com.example.backendplantshop.exception.AppException(ErrorCode.ACCESS_DENIED);
        }
        
        CreatePaymentResponse response = momoService.createPayment(request);
        return ApiResponse.<CreatePaymentResponse>builder()
                .statusCode(ErrorCode.CALL_API_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.CALL_API_SUCCESSFULL.getMessage())
                .data(response)
                .build();
    }
    
    /**
     * Callback từ MoMo sau khi thanh toán
     */
    @PostMapping("/momo/callback")
    public ResponseEntity<?> momoCallback(@RequestBody MoMoCallbackRequest callbackRequest) {
        try {
            log.info("Nhận callback từ MoMo: orderId={}, resultCode={}, amount={}", 
                    callbackRequest.getOrderId(), callbackRequest.getResultCode(), callbackRequest.getAmount());
            
            // Tạo raw hash để verify signature
            String rawHash = MoMoUtil.createCallbackRawHash(
                    momoConfig.getAccessKey(),
                    String.valueOf(callbackRequest.getAmount()),
                    callbackRequest.getExtraData() != null ? callbackRequest.getExtraData() : "",
                    callbackRequest.getMessage() != null ? callbackRequest.getMessage() : "",
                    callbackRequest.getOrderId(),
                    callbackRequest.getOrderInfo() != null ? callbackRequest.getOrderInfo() : "",
                    callbackRequest.getOrderType() != null ? callbackRequest.getOrderType() : "",
                    callbackRequest.getPartnerCode(),
                    callbackRequest.getPayType() != null ? callbackRequest.getPayType() : "",
                    callbackRequest.getRequestId(),
                    String.valueOf(callbackRequest.getResponseTime()),
                    callbackRequest.getResultCode(),
                    callbackRequest.getTransId()
            );
            
            // Verify signature
            boolean isValid = momoService.verifyCallback(callbackRequest.getSignature(), rawHash);
            if (!isValid) {
                log.warn("Signature không hợp lệ từ MoMo callback");
                return ResponseEntity.badRequest().body("{\"status\":\"invalid_signature\"}");
            }
            
            Integer orderId = paymentService.extractOrderIdFromMoMoOrderId(callbackRequest.getOrderId());
            if (orderId == null) {
                log.error("Không thể parse orderId từ giá trị: {}", callbackRequest.getOrderId());
            }
            MoMoPaymentPurpose purpose = MoMoPaymentPurpose.fromExtraData(callbackRequest.getExtraData());

            // Xử lý kết quả thanh toán
            if (purpose == MoMoPaymentPurpose.DEPOSIT) {
                paymentService.handleDepositCallback(orderId, callbackRequest);
            } else {
                orderService.handleOrderPaymentCallback(orderId, callbackRequest);
            }
            
            // Trả về response cho MoMo
            return ResponseEntity.ok().body("{\"status\":\"success\"}");
            
        } catch (Exception e) {
            log.error("Lỗi khi xử lý callback từ MoMo: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("{\"status\":\"error\"}");
        }
    }
    
    /**
     * Return URL sau khi thanh toán (redirect từ MoMo)
     */
    @GetMapping("/momo/return")
    public ResponseEntity<?> momoReturn(
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) Integer resultCode,
            @RequestParam(required = false) String message) {
        
        log.info("Return từ MoMo: orderId={}, resultCode={}, message={}", orderId, resultCode, message);
        
        // URL encode các tham số để tránh lỗi Unicode trong HTTP header
        String encodedOrderId = orderId != null ? URLEncoder.encode(orderId, StandardCharsets.UTF_8) : "";
        String encodedResultCode = resultCode != null ? String.valueOf(resultCode) : "";
        String encodedMessage = message != null ? URLEncoder.encode(message, StandardCharsets.UTF_8) : "";
        
        // Redirect về trang chủ frontend với thông tin kết quả thanh toán trong query params
        // Frontend có thể đọc query params và hiển thị thông báo tương ứng
        String redirectUrl = String.format("http://localhost:3000/orders-page/?paymentResult=true&orderId=%s&resultCode=%s&message=%s",
                encodedOrderId,
                encodedResultCode,
                encodedMessage);
        
        return ResponseEntity.status(302)
                .header("Location", redirectUrl)
                .build();
    }
}
