package com.example.backendplantshop.service.impl;

import com.example.backendplantshop.config.MoMoConfig;
import com.example.backendplantshop.dto.request.momo.CreatePaymentRequest;
import com.example.backendplantshop.dto.request.momo.MoMoPaymentRequest;
import com.example.backendplantshop.dto.response.momo.CreatePaymentResponse;
import com.example.backendplantshop.dto.response.momo.MoMoPaymentResponse;
import com.example.backendplantshop.enums.ErrorCode;
import com.example.backendplantshop.exception.AppException;
import com.example.backendplantshop.service.intf.MoMoService;
import com.example.backendplantshop.util.MoMoUtil;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = "momo.mock.enabled", 
    havingValue = "false", 
    matchIfMissing = false
)
public class MoMoServiceImpl implements MoMoService {
    
    private final MoMoConfig momoConfig;
    private final RestTemplate restTemplate;
    private final Gson gson = new Gson();
    
    @Override
    public CreatePaymentResponse createPayment(CreatePaymentRequest request) {
        try {
            // Tạo requestId và orderId
            String requestId = UUID.randomUUID().toString();
            String orderId = String.valueOf(request.getOrderId());
            Long amount = request.getAmount().longValue();
            
            // Tạo orderInfo nếu chưa có
            String orderInfo = request.getOrderInfo();
            if (orderInfo == null || orderInfo.isEmpty()) {
                orderInfo = "Thanh toán đơn hàng #" + orderId;
            }
            
            // Tạo extraData (có thể để trống hoặc JSON string)
            String extraData = "";
            
            // Tạo raw hash
            String rawHash = MoMoUtil.createRawHash(
                    momoConfig.getAccessKey(),
                    String.valueOf(amount),
                    extraData,
                    momoConfig.getNotifyUrl(),
                    orderId,
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
            
            // Tạo MoMo payment request
            MoMoPaymentRequest momoRequest = MoMoPaymentRequest.builder()
                    .partnerCode(momoConfig.getPartnerCode())
                    .partnerName("Plant Shop")
                    .storeId("PlantShop")
                    .requestId(requestId)
                    .amount(amount)
                    .orderId(orderId)
                    .orderInfo(orderInfo)
                    .redirectUrl(momoConfig.getReturnUrl())
                    .ipnUrl(momoConfig.getNotifyUrl())
                    .requestType(momoConfig.getRequestType())
                    .extraData(extraData)
                    .autoCapture("true")
                    .lang("vi")
                    .signature(signature)
                    .build();
            
            // Gọi MoMo API
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<MoMoPaymentRequest> entity = new HttpEntity<>(momoRequest, headers);
            
            log.info("Gọi MoMo API với requestId: {}, orderId: {}, amount: {}", requestId, orderId, amount);
            
            ResponseEntity<MoMoPaymentResponse> response = restTemplate.exchange(
                    momoConfig.getApiEndpoint(),
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
            
            // Tạo response
            return CreatePaymentResponse.builder()
                    .payUrl(momoResponse.getPayUrl())
                    .qrCodeUrl(momoResponse.getQrCodeUrl())
                    .deeplink(momoResponse.getDeeplink())
                    .orderId(orderId)
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

