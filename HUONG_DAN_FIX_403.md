# Hướng dẫn fix lỗi 403 Forbidden khi gửi OTP

## Nguyên nhân có thể:

### 1. Backend chưa restart
Sau khi cập nhật `SecurityConfig.java`, backend cần được **restart hoàn toàn** để áp dụng thay đổi.

**Giải pháp:**
- Dừng backend (Ctrl+C hoặc stop trong IDE)
- Build lại project: `./gradlew clean build -x test`
- Start lại backend

### 2. Frontend gọi sai URL
Backend đang chạy ở **port 1234**, nhưng frontend có thể đang gọi đến port khác.

**Kiểm tra:**
- Backend URL: `http://localhost:1234/api/auth/send-otp`
- Frontend có thể đang gọi: `http://localhost:3000/api/auth/send-otp` (sai)

**Giải pháp:**
- Cấu hình proxy trong frontend (nếu dùng React/Vue)
- Hoặc đổi URL trong frontend thành: `http://localhost:1234/api/auth/send-otp`

### 3. Email đã tồn tại
Nếu email đã được đăng ký, backend sẽ trả về lỗi `EMAIL_ALREADY_EXISTS` (code 1003), nhưng có thể bị hiểu nhầm là 403.

**Kiểm tra:**
- Thử với email mới chưa được đăng ký
- Hoặc kiểm tra response body để xem lỗi thực sự là gì

### 4. CORS issue
Có thể CORS preflight (OPTIONS request) bị chặn.

**Giải pháp:**
- Kiểm tra Network tab trong DevTools
- Xem có request OPTIONS nào bị fail không
- Đảm bảo CORS config đã đúng trong `WebConfig.java`

## Các bước debug:

### Bước 1: Kiểm tra backend logs
Xem logs backend khi gọi API để biết lỗi chi tiết:
```
2025-XX-XX ERROR ... Access denied for user on endpoint: /api/auth/send-otp
```

### Bước 2: Test trực tiếp với Postman/curl
```bash
curl -X POST http://localhost:1234/api/auth/send-otp \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com"}'
```

### Bước 3: Kiểm tra SecurityConfig
Đảm bảo endpoint đã được thêm vào `permitAll()`:
```java
.requestMatchers("/api/auth/send-otp", ...).permitAll()
```

### Bước 4: Kiểm tra Network tab
Trong DevTools:
- Xem request có được gửi đi không
- Xem response status code và body
- Xem có request OPTIONS (preflight) nào bị fail không

## Quick Fix:

1. **Restart backend:**
   ```bash
   # Dừng backend (Ctrl+C)
   # Build lại
   ./gradlew clean build -x test
   # Start lại
   ./gradlew bootRun
   ```

2. **Kiểm tra URL frontend:**
   - Đảm bảo gọi đến: `http://localhost:1234/api/auth/send-otp`
   - Hoặc cấu hình proxy trong `package.json` (React) hoặc `vite.config.js` (Vite)

3. **Test với email mới:**
   - Thử với email chưa được đăng ký
   - Nếu vẫn lỗi, kiểm tra backend logs

