package com.example.backendplantshop.service.impl;

import com.example.backendplantshop.config.MoMoConfig;
import com.example.backendplantshop.dto.request.momo.CreatePaymentRequest;
import com.example.backendplantshop.dto.request.momo.MoMoPaymentRequest;
import com.example.backendplantshop.dto.response.momo.CreatePaymentResponse;
import com.example.backendplantshop.dto.response.momo.MoMoPaymentResponse;
import com.example.backendplantshop.enums.ErrorCode;
import com.example.backendplantshop.enums.MoMoPaymentPurpose;
import com.example.backendplantshop.exception.AppException;
import com.example.backendplantshop.service.intf.MoMoService;
import com.example.backendplantshop.util.MoMoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MoMoServiceImpl implements MoMoService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final MoMoConfig momoConfig;
    private final RestTemplate restTemplate;
    
    @Override
    public CreatePaymentResponse createPayment(CreatePaymentRequest request) {
        try {
            // Validate MoMo credentials
            if (momoConfig.getPartnerCode() == null || momoConfig.getPartnerCode().trim().isEmpty()) {
                log.error("MOMO_PARTNER_CODE không được để trống. Vui lòng kiểm tra file .env");
                throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
            if (momoConfig.getAccessKey() == null || momoConfig.getAccessKey().trim().isEmpty()) {
                log.error("MOMO_ACCESS_KEY không được để trống. Vui lòng kiểm tra file .env");
                throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
            if (momoConfig.getSecretKey() == null || momoConfig.getSecretKey().trim().isEmpty()) {
                log.error("MOMO_SECRET_KEY không được để trống. Vui lòng kiểm tra file .env");
                throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
            
            // Tạo requestId và orderId
            // requestId: UUID duy nhất cho mỗi request
            String requestId = UUID.randomUUID().toString();
            
            // orderId cho MoMo: Kết hợp orderId từ DB + timestamp để đảm bảo tính duy nhất
            // Format: ORDER_{orderId}_{timestamp}
            // Ví dụ: ORDER_35_1733831974000
            // Lý do: MoMo yêu cầu orderId phải duy nhất trong hệ thống của họ
            // Nếu user tạo payment nhiều lần cho cùng một order, mỗi lần sẽ có orderId khác nhau
            long timestamp = System.currentTimeMillis();
            MoMoPaymentPurpose purpose = request.getPurpose() != null ? request.getPurpose() : MoMoPaymentPurpose.ORDER_PAYMENT;
            String orderPrefix = purpose == MoMoPaymentPurpose.DEPOSIT ? "DEPOSIT" : "ORDER";
            String momoOrderId = String.format("%s_%d_%d", orderPrefix, request.getOrderId(), timestamp);
            
            log.info("Tạo MoMo orderId: {} từ orderId DB: {}", momoOrderId, request.getOrderId());
            
            Long amount = request.getAmount().longValue();
            
            // Tạo orderInfo nếu chưa có
            String orderInfo = request.getOrderInfo();
            if (orderInfo == null || orderInfo.isEmpty()) {
                orderInfo = "Thanh toán đơn hàng #" + request.getOrderId();
            }

            // Tạo extraData (có thể để trống hoặc JSON string)
            String extraData = "purpose=" + purpose.name();
            
            // Tạo raw hash (sử dụng momoOrderId cho MoMo API)
            String rawHash = MoMoUtil.createRawHash(
                    momoConfig.getAccessKey(),
                    String.valueOf(amount),
                    extraData,
                    momoConfig.getNotifyUrl(),
                    momoOrderId, // Sử dụng momoOrderId thay vì orderId từ DB
                    orderInfo,
                    momoConfig.getPartnerCode(),
                    momoConfig.getReturnUrl(),
                    requestId,
                    momoConfig.getRequestType()
            );
            
            // Tạo signature
            String signature = MoMoUtil.createSignature(
                    momoConfig.getAccessKey(),
                    momoConfig.getSecretKey(),
                    rawHash
            );
            
            if (log.isDebugEnabled()) {
                log.debug("MoMo raw hash trước khi ký: {}", rawHash);
            }
            
            // Tạo MoMo payment request
            MoMoPaymentRequest momoRequest = MoMoPaymentRequest.builder()
                    .partnerCode(momoConfig.getPartnerCode())
                    .partnerName(momoConfig.getStoreName())
                    .storeId(momoConfig.getStoreId())
                    .requestId(requestId)
                    .amount(amount)
                    .orderId(momoOrderId) // Sử dụng momoOrderId (duy nhất) thay vì orderId từ DB
                    .orderInfo(orderInfo)
                    .redirectUrl(momoConfig.getReturnUrl())
                    .ipnUrl(momoConfig.getNotifyUrl())
                    .requestType(momoConfig.getRequestType())
                    .extraData(extraData)
                    .autoCapture(Boolean.TRUE)
                    .lang(momoConfig.getLang())
                    .signature(signature)
                    .build();
            
            if (log.isDebugEnabled()) {
                try {
                    log.debug("MoMo payload gửi đi: {}", OBJECT_MAPPER.writeValueAsString(momoRequest));
                } catch (JsonProcessingException e) {
                    log.warn("Không thể serialize MoMo payload để log: {}", e.getMessage());
                }
            }
            
            // Gọi MoMo API
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<MoMoPaymentRequest> entity = new HttpEntity<>(momoRequest, headers);
            
            String apiEndpoint = momoConfig.getApiEndpoint();
            log.info("Gọi MoMo API với requestId: {}, momoOrderId: {}, orderId DB: {}, amount: {}, purpose: {}",
                    requestId, momoOrderId, request.getOrderId(), amount, purpose);
            
            // Validate endpoint URL
            if (apiEndpoint == null || apiEndpoint.trim().isEmpty()) {
                log.error("MoMo API endpoint không được để trống");
                throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
            
            // Tự động sửa URL nếu thiếu /create
//            if (!apiEndpoint.endsWith("/create")) {
//                log.warn("MoMo API endpoint thiếu /create. Tự động sửa từ: {}", apiEndpoint);
//                // Loại bỏ dấu / ở cuối nếu có, rồi thêm /create
//                apiEndpoint = apiEndpoint.replaceAll("/+$", "") + "/create";
//                log.info("URL đã được sửa thành: {}", apiEndpoint);
//            }
            
//            log.info("MoMo API Endpoint: {}", apiEndpoint);
            
            ResponseEntity<MoMoPaymentResponse> response = restTemplate.exchange(
                    apiEndpoint,
                    HttpMethod.POST,
                    entity,
                    MoMoPaymentResponse.class
            );
            
            MoMoPaymentResponse momoResponse = response.getBody();
            
            if (momoResponse == null) {
                throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
            
            if (momoResponse.getResultCode() != null && momoResponse.getResultCode() != 0) {
                log.error("MoMo API trả về lỗi: {} - {}", momoResponse.getResultCode(), momoResponse.getMessage());
                throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
            
            // Tạo response - trả về đầy đủ thông tin để frontend lựa chọn
            // - deeplink: dùng cho mobile app (momo://)
            // - payUrl: dùng cho web browser
            // - qrCodeUrl: dùng để hiển thị QR code
            log.info("MoMo trả về - payUrl: {}, deeplink: {}, qrCodeUrl: {}", 
                    momoResponse.getPayUrl(), momoResponse.getDeeplink(), momoResponse.getQrCodeUrl());
            
            return CreatePaymentResponse.builder()
                    .payUrl(momoResponse.getPayUrl()) // URL thanh toán cho web
                    .qrCodeUrl(momoResponse.getQrCodeUrl()) // URL QR code
                    .deeplink(momoResponse.getDeeplink()) // Deeplink cho mobile app (momo://)
                    .orderId(String.valueOf(request.getOrderId())) // Trả về orderId từ DB cho frontend
                    .amount(amount)
                    .message("Tạo thanh toán thành công")
                    .build();
                    
        } catch (Exception e) {
            log.error("Lỗi khi tạo payment với MoMo: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
    
    @Override
    public boolean verifyCallback(String signature, String rawHash) {
        try {
            String calculatedSignature = MoMoUtil.createSignature(
                    momoConfig.getAccessKey(),
                    momoConfig.getSecretKey(),
                    rawHash
            );
            return calculatedSignature.equals(signature);
        } catch (Exception e) {
            log.error("Lỗi khi verify callback signature: {}", e.getMessage(), e);
            return false;
        }
    }
}

