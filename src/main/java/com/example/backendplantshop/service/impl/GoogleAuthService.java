package com.example.backendplantshop.service.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class GoogleAuthService {

    @Value("${google.oauth2.client-id}")
    private String clientId;

    @Value("${google.oauth2.client-secret}")
    private String clientSecret;

    @Value("${google.oauth2.redirect-uri}")
    private String redirectUri;

    private final Gson gson = new Gson();
    private final RestTemplate restTemplate = new RestTemplate();


    public String exchangeCodeForAccessToken(String code, String redirectUri) throws Exception {
        try {
            String tokenUrl = "https://oauth2.googleapis.com/token";
            
            // Sử dụng redirectUri từ param, nếu null thì dùng từ config
            String finalRedirectUri = (redirectUri != null && !redirectUri.trim().isEmpty()) 
                ? redirectUri 
                : this.redirectUri;
            
            log.info("Bắt đầu exchange code với redirect_uri: {}", finalRedirectUri);
            
            // Tạo request body
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("code", code);
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("redirect_uri", finalRedirectUri);
            body.add("grant_type", "authorization_code");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            log.debug("Gửi request đến Google token endpoint...");
            ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, request, String.class);

            String responseBody = response.getBody();
            log.info("Response từ Google: status={}, body={}", response.getStatusCode(), 
                responseBody != null && responseBody.length() > 200 
                    ? responseBody.substring(0, 200) + "..." 
                    : responseBody);

            if (response.getStatusCode() != HttpStatus.OK || responseBody == null) {
                log.error("Lỗi khi exchange code: {} - {}", response.getStatusCode(), responseBody);
                throw new Exception("Không thể exchange code. Status: " + response.getStatusCode() + ", Body: " + responseBody);
            }

            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
            
            // Kiểm tra xem Google có trả về lỗi không
            if (jsonResponse.has("error")) {
                String error = jsonResponse.get("error").getAsString();
                String errorDescription = jsonResponse.has("error_description") 
                    ? jsonResponse.get("error_description").getAsString() 
                    : error;
                log.error("Google trả về lỗi: {} - {}", error, errorDescription);
                throw new Exception("Lỗi từ Google: " + errorDescription);
            }
            
            if (!jsonResponse.has("access_token")) {
                log.error("Response không chứa access_token. Response: {}", responseBody);
                throw new Exception("Response không chứa access_token. Response: " + responseBody);
            }

            String accessToken = jsonResponse.get("access_token").getAsString();
            log.info("Exchange code thành công, đã lấy được access token");
            return accessToken;
        } catch (HttpClientErrorException e) {
            String errorBody = e.getResponseBodyAsString();
            log.error("Lỗi HTTP khi exchange code: status={}, body={}", e.getStatusCode(), errorBody);
            
            // Parse error message từ Google
            try {
                if (errorBody != null) {
                    JsonObject errorJson = gson.fromJson(errorBody, JsonObject.class);
                    if (errorJson.has("error_description")) {
                        String errorDesc = errorJson.get("error_description").getAsString();
                        log.error("Chi tiết lỗi từ Google: {}", errorDesc);
                        throw new Exception("Lỗi từ Google: " + errorDesc);
                    }
                }
            } catch (Exception parseEx) {
                // Ignore parse error, dùng message mặc định
                log.debug("Không thể parse error body: {}", parseEx.getMessage());
            }
            throw new Exception("Lỗi khi exchange code: " + e.getMessage() + (errorBody != null ? ", Body: " + errorBody : ""));
        } catch (Exception e) {
            log.error("Lỗi khi exchange code cho access token: {}", e.getMessage(), e);
            throw e; // Re-throw để giữ nguyên message
        }
    }


//     Lấy thông tin user từ Google bằng access token
    public Map<String, String> getUserInfoFromGoogle(String accessToken) throws Exception {
        try {
            String userInfoUrl = "https://www.googleapis.com/oauth2/v2/userinfo";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<Void> request = new HttpEntity<>(headers);

            log.debug("Lấy thông tin user từ Google...");
            ResponseEntity<String> response = restTemplate.exchange(
                userInfoUrl, 
                HttpMethod.GET, 
                request, 
                String.class
            );

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                log.error("Lỗi khi lấy user info: {} - {}", response.getStatusCode(), response.getBody());
                throw new Exception("Không thể lấy thông tin user. Status: " + response.getStatusCode());
            }

            JsonObject jsonResponse = gson.fromJson(response.getBody(), JsonObject.class);
            log.info("Đã lấy được thông tin user từ Google: email={}", 
                jsonResponse.has("email") ? jsonResponse.get("email").getAsString() : "N/A");

            Map<String, String> userInfo = new HashMap<>();
            userInfo.put("id", jsonResponse.has("id") ? jsonResponse.get("id").getAsString() : null);
            userInfo.put("email", jsonResponse.has("email") ? jsonResponse.get("email").getAsString() : null);
            userInfo.put("name", jsonResponse.has("name") ? jsonResponse.get("name").getAsString() : null);
            userInfo.put("picture", jsonResponse.has("picture") ? jsonResponse.get("picture").getAsString() : null);

            return userInfo;
        } catch (HttpClientErrorException e) {
            log.error("Lỗi HTTP khi lấy user info: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new Exception("Lỗi khi lấy thông tin user: " + e.getMessage());
        } catch (Exception e) {
            log.error("Lỗi khi lấy thông tin user từ Google: {}", e.getMessage(), e);
            throw new Exception("Lỗi khi lấy thông tin user: " + e.getMessage());
        }
    }


    public String getGoogleId(Map<String, String> userInfo) {
        return userInfo.get("id");
    }
    public String getEmail(Map<String, String> userInfo) {
        return userInfo.get("email");
    }
    public String getName(Map<String, String> userInfo) {
        return userInfo.get("name");
    }
//    public String getPicture(Map<String, String> userInfo) {
//        return userInfo.get("picture");
//    }
}
