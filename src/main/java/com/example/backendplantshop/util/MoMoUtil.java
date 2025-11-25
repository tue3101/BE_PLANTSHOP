package com.example.backendplantshop.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class MoMoUtil {
    
    private static final String HMAC_SHA256 = "HmacSHA256";
    

//     Tạo chữ ký số (signature) cho MoMo Payment
    public static String createSignature(String accessKey, String secretKey, String rawHash) {
        // Validate credentials
        if (secretKey == null || secretKey.trim().isEmpty()) {
            throw new IllegalArgumentException("MOMO_SECRET_KEY không được để trống. Vui lòng kiểm tra file .env");
        }
        if (accessKey == null || accessKey.trim().isEmpty()) {
            throw new IllegalArgumentException("MOMO_ACCESS_KEY không được để trống. Vui lòng kiểm tra file .env");
        }
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            //lấy chuỗi secretKey chuyển thành mảng byte bằng UTF-8
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec); //khởi tạo với secretkey
            //sinh ra mã hash từ rawhash bằng HMAC
            byte[] hashBytes = mac.doFinal(rawHash.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Lỗi khi tạo signature: " + e.getMessage(), e);
        }
    }
    
    /**
     * Chuyển đổi byte array sang hex string
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    /**
     * Tạo raw hash string từ các tham số
     */
    public static String createRawHash(String accessKey, String amount, String extraData, 
                                       String ipnUrl, String orderId, String orderInfo, 
                                       String partnerCode, String redirectUrl, String requestId, 
                                       String requestType) {
        return String.format("accessKey=%s&amount=%s&extraData=%s&ipnUrl=%s&orderId=%s&orderInfo=%s&partnerCode=%s&redirectUrl=%s&requestId=%s&requestType=%s",
                accessKey, amount, extraData, ipnUrl, orderId, orderInfo, partnerCode, redirectUrl, requestId, requestType);
    }
    
    /**
     * Tạo raw hash cho callback verification
     */
    public static String createCallbackRawHash(String accessKey, String amount, String extraData,
                                                String message, String orderId, String orderInfo,
                                                String orderType, String partnerCode, String payType,
                                                String requestId, String responseTime, Integer resultCode,
                                                Long transId) {
        // MoMo callback raw hash format
        return String.format("accessKey=%s&amount=%s&extraData=%s&message=%s&orderId=%s&orderInfo=%s&orderType=%s&partnerCode=%s&payType=%s&requestId=%s&responseTime=%s&resultCode=%s&transId=%s",
                accessKey != null ? accessKey : "",
                amount != null ? amount : "",
                extraData != null ? extraData : "",
                message != null ? message : "",
                orderId != null ? orderId : "",
                orderInfo != null ? orderInfo : "",
                orderType != null ? orderType : "",
                partnerCode != null ? partnerCode : "",
                payType != null ? payType : "",
                requestId != null ? requestId : "",
                responseTime != null ? responseTime : "",
                resultCode != null ? resultCode : "",
                transId != null ? transId : "");
    }
}

