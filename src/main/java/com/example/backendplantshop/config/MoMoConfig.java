package com.example.backendplantshop.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class MoMoConfig {
    
    @Value("${momo.partner-code}")
    private String partnerCode;
    
    @Value("${momo.access-key}")
    private String accessKey;
    
    @Value("${momo.secret-key}")
    private String secretKey;
    
    @Value("${momo.api-endpoint:https://test-payment.momo.vn/v2/gateway/api/create}")
    private String apiEndpoint;
    
    @Value("${momo.return-url}")
    private String returnUrl;
    
    @Value("${momo.notify-url}")
    private String notifyUrl;
    
    @Value("${momo.request-type:captureWallet}")
    private String requestType;
}

