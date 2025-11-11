# Hướng dẫn Frontend tích hợp OTP Email

## 1. Flow đăng ký mới với OTP

```
User nhập email → Gửi OTP → Nhập OTP → Verify OTP → Đăng ký thành công
```

## 2. Các API Endpoints

### 2.1. Gửi OTP
**Endpoint:** `POST /api/auth/send-otp`

**Request:**
```json
{
  "email": "user@example.com"
}
```

**Response thành công:**
```json
{
  "statusCode": 201,
  "success": true,
  "message": "Đã gửi mã OTP đến email của bạn",
  "data": null
}
```

**Response lỗi (email đã tồn tại):**
```json
{
  "statusCode": 1003,
  "success": false,
  "message": "email đã được đăng ký"
}
```

### 2.2. Xác thực OTP (Optional - có thể bỏ qua)
**Endpoint:** `POST /api/auth/verify-otp`

**Request:**
```json
{
  "email": "user@example.com",
  "otpCode": "123456"
}
```

**Response:**
```json
{
  "statusCode": 201,
  "success": true,
  "message": "Xác thực OTP thành công",
  "data": true
}
```

### 2.3. Đăng ký với OTP
**Endpoint:** `POST /api/auth/register`

**Request:**
```json
{
  "username": "testuser",
  "email": "user@example.com",
  "password": "password123",
  "otpCode": "123456"
}
```

**Response thành công:**
```json
{
  "statusCode": 202,
  "success": true,
  "message": "đăng ký tài khoản thành công"
}
```

**Response lỗi (OTP sai):**
```json
{
  "statusCode": 403,
  "success": false,
  "message": "không có quyền truy cập"
}
```

## 3. UI/UX Flow đề xuất

### Màn hình đăng ký:

```
┌─────────────────────────────────┐
│  ĐĂNG KÝ TÀI KHOẢN              │
├─────────────────────────────────┤
│  Username: [___________]         │
│  Email:    [___________]         │
│  Password: [___________]         │
│                                 │
│  [Gửi mã OTP]                   │
│                                 │
│  Mã OTP:   [______]              │
│  (Hiển thị sau khi gửi OTP)     │
│                                 │
│  [Đăng ký]                      │
└─────────────────────────────────┘
```

### Flow UI:

1. **Bước 1:** User nhập username, email, password
2. **Bước 2:** Click "Gửi mã OTP"
   - Disable button "Gửi mã OTP" (tránh spam)
   - Hiển thị countdown timer (60s) để gửi lại
   - Hiển thị input OTP
   - Hiển thị thông báo: "Đã gửi mã OTP đến email của bạn"
3. **Bước 3:** User nhập mã OTP
4. **Bước 4:** Click "Đăng ký"
   - Validate OTP
   - Gửi request đăng ký với OTP
   - Nếu thành công → Redirect đến trang login hoặc tự động login

## 4. Code Example (React/TypeScript)

### 4.1. Component Register với OTP

```typescript
import { useState } from 'react';
import axios from 'axios';

interface RegisterForm {
  username: string;
  email: string;
  password: string;
  otpCode: string;
}

const RegisterPage = () => {
  const [formData, setFormData] = useState<RegisterForm>({
    username: '',
    email: '',
    password: '',
    otpCode: ''
  });
  
  const [otpSent, setOtpSent] = useState(false);
  const [countdown, setCountdown] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string>('');

  // Gửi OTP
  const handleSendOtp = async () => {
    if (!formData.email) {
      setError('Vui lòng nhập email');
      return;
    }

    try {
      setLoading(true);
      setError('');
      
      const response = await axios.post('http://localhost:1234/api/auth/send-otp', {
        email: formData.email
      });

      if (response.data.success) {
        setOtpSent(true);
        setCountdown(60); // 60 giây countdown
        
        // Countdown timer
        const timer = setInterval(() => {
          setCountdown((prev) => {
            if (prev <= 1) {
              clearInterval(timer);
              return 0;
            }
            return prev - 1;
          });
        }, 1000);
        
        alert('Đã gửi mã OTP đến email của bạn!');
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Lỗi khi gửi OTP');
    } finally {
      setLoading(false);
    }
  };

  // Đăng ký
  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!formData.otpCode) {
      setError('Vui lòng nhập mã OTP');
      return;
    }

    try {
      setLoading(true);
      setError('');
      
      const response = await axios.post('http://localhost:1234/api/auth/register', {
        username: formData.username,
        email: formData.email,
        password: formData.password,
        otpCode: formData.otpCode
      });

      if (response.data.success) {
        alert('Đăng ký thành công!');
        // Redirect đến trang login hoặc tự động login
        window.location.href = '/login';
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Đăng ký thất bại');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="register-container">
      <h2>Đăng ký tài khoản</h2>
      
      {error && <div className="error">{error}</div>}
      
      <form onSubmit={handleRegister}>
        <div>
          <label>Username:</label>
          <input
            type="text"
            value={formData.username}
            onChange={(e) => setFormData({...formData, username: e.target.value})}
            required
          />
        </div>
        
        <div>
          <label>Email:</label>
          <input
            type="email"
            value={formData.email}
            onChange={(e) => setFormData({...formData, email: e.target.value})}
            required
            disabled={otpSent}
          />
        </div>
        
        <div>
          <label>Password:</label>
          <input
            type="password"
            value={formData.password}
            onChange={(e) => setFormData({...formData, password: e.target.value})}
            required
          />
        </div>
        
        <div>
          <button
            type="button"
            onClick={handleSendOtp}
            disabled={!formData.email || loading || countdown > 0}
          >
            {countdown > 0 ? `Gửi lại sau ${countdown}s` : 'Gửi mã OTP'}
          </button>
        </div>
        
        {otpSent && (
          <div>
            <label>Mã OTP:</label>
            <input
              type="text"
              value={formData.otpCode}
              onChange={(e) => setFormData({...formData, otpCode: e.target.value})}
              placeholder="Nhập mã 6 chữ số"
              maxLength={6}
              required
            />
            <small>Mã OTP đã được gửi đến {formData.email}</small>
          </div>
        )}
        
        <button type="submit" disabled={!otpSent || loading}>
          {loading ? 'Đang xử lý...' : 'Đăng ký'}
        </button>
      </form>
    </div>
  );
};

export default RegisterPage;
```

## 5. Xử lý lỗi

### Các lỗi có thể xảy ra:

1. **Email đã tồn tại** (khi gửi OTP)
   - Status: 1003
   - Message: "email đã được đăng ký"
   - **Xử lý:** Hiển thị thông báo, không cho gửi OTP

2. **OTP không hợp lệ** (khi đăng ký)
   - Status: 403
   - Message: "không có quyền truy cập"
   - **Xử lý:** Hiển thị thông báo, cho phép gửi lại OTP

3. **OTP hết hạn**
   - Status: 403
   - Message: "không có quyền truy cập"
   - **Xử lý:** Hiển thị "Mã OTP đã hết hạn, vui lòng gửi lại"

4. **OTP đã được sử dụng**
   - Status: 403
   - Message: "không có quyền truy cập"
   - **Xử lý:** Hiển thị "Mã OTP đã được sử dụng, vui lòng gửi lại"

## 6. Best Practices

### 6.1. Validation phía Frontend
```typescript
// Validate email
const validateEmail = (email: string): boolean => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
};

// Validate OTP (6 chữ số)
const validateOtp = (otp: string): boolean => {
  return /^\d{6}$/.test(otp);
};
```

### 6.2. Rate Limiting (tránh spam)
- Disable button "Gửi OTP" sau khi click
- Countdown timer 60 giây trước khi cho phép gửi lại
- Giới hạn số lần gửi OTP trong 1 giờ (nếu cần)

### 6.3. UX Improvements
- Hiển thị loading spinner khi đang gửi OTP
- Auto-focus vào input OTP sau khi gửi thành công
- Auto-submit form khi nhập đủ 6 chữ số OTP (optional)
- Hiển thị thời gian còn lại của OTP (5 phút)

### 6.4. Security
- Không lưu OTP trong localStorage/sessionStorage
- Clear form sau khi đăng ký thành công
- Validate tất cả input trước khi gửi

## 7. Flow hoàn chỉnh

```
1. User nhập thông tin đăng ký
   ↓
2. Click "Gửi mã OTP"
   ↓
3. Backend gửi email OTP
   ↓
4. User nhận email, lấy mã OTP
   ↓
5. User nhập mã OTP vào form
   ↓
6. Click "Đăng ký"
   ↓
7. Backend verify OTP + tạo tài khoản
   ↓
8. Thành công → Redirect/Login
```

## 8. Test Cases cho Frontend

1. ✅ Gửi OTP với email hợp lệ
2. ✅ Gửi OTP với email đã tồn tại (lỗi)
3. ✅ Gửi OTP nhiều lần (rate limiting)
4. ✅ Nhập OTP đúng → Đăng ký thành công
5. ✅ Nhập OTP sai → Hiển thị lỗi
6. ✅ Nhập OTP hết hạn → Hiển thị lỗi
7. ✅ Đăng ký không có OTP → Hiển thị lỗi
8. ✅ Validation form (email format, password length, etc.)

## 9. API Base URL

```typescript
const API_BASE_URL = 'http://localhost:1234/api/auth';

// Hoặc dùng environment variable
const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:1234/api/auth';
```

## 10. Quên mật khẩu với OTP

### 10.1. Flow quên mật khẩu

```
User nhập email → Gửi OTP → Nhập OTP + Mật khẩu mới → Reset password thành công
```

### 10.2. API Endpoints

#### 10.2.1. Gửi OTP cho quên mật khẩu
**Endpoint:** `POST /api/auth/forgot-password/send-otp`

**Request:**
```json
{
  "email": "user@example.com"
}
```

**Response thành công:**
```json
{
  "statusCode": 201,
  "success": true,
  "message": "Đã gửi mã OTP đến email của bạn để đặt lại mật khẩu",
  "data": null
}
```

**Response lỗi (email không tồn tại):**
```json
{
  "statusCode": 1001,
  "success": false,
  "message": "người dùng không tồn tại"
}
```

#### 10.2.2. Đặt lại mật khẩu
**Endpoint:** `POST /api/auth/forgot-password/reset`

**Request:**
```json
{
  "email": "user@example.com",
  "otpCode": "123456",
  "newPassword": "newpassword123"
}
```

**Response thành công:**
```json
{
  "statusCode": 203,
  "success": true,
  "message": "Đặt lại mật khẩu thành công"
}
```

**Response lỗi (OTP sai):**
```json
{
  "statusCode": 403,
  "success": false,
  "message": "không có quyền truy cập"
}
```

### 10.3. Code Example (React/TypeScript)

```typescript
import { useState } from 'react';
import axios from 'axios';

interface ForgotPasswordForm {
  email: string;
  otpCode: string;
  newPassword: string;
}

const ForgotPasswordPage = () => {
  const [formData, setFormData] = useState<ForgotPasswordForm>({
    email: '',
    otpCode: '',
    newPassword: ''
  });
  
  const [otpSent, setOtpSent] = useState(false);
  const [countdown, setCountdown] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string>('');

  // Gửi OTP
  const handleSendOtp = async () => {
    if (!formData.email) {
      setError('Vui lòng nhập email');
      return;
    }

    try {
      setLoading(true);
      setError('');
      
      const response = await axios.post('http://localhost:1234/api/auth/forgot-password/send-otp', {
        email: formData.email
      });

      if (response.data.success) {
        setOtpSent(true);
        setCountdown(60);
        
        const timer = setInterval(() => {
          setCountdown((prev) => {
            if (prev <= 1) {
              clearInterval(timer);
              return 0;
            }
            return prev - 1;
          });
        }, 1000);
        
        alert('Đã gửi mã OTP đến email của bạn!');
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Lỗi khi gửi OTP');
    } finally {
      setLoading(false);
    }
  };

  // Reset password
  const handleResetPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!formData.otpCode || !formData.newPassword) {
      setError('Vui lòng nhập đầy đủ thông tin');
      return;
    }

    try {
      setLoading(true);
      setError('');
      
      const response = await axios.post('http://localhost:1234/api/auth/forgot-password/reset', {
        email: formData.email,
        otpCode: formData.otpCode,
        newPassword: formData.newPassword
      });

      if (response.data.success) {
        alert('Đặt lại mật khẩu thành công! Vui lòng đăng nhập lại.');
        window.location.href = '/login';
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Đặt lại mật khẩu thất bại');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="forgot-password-container">
      <h2>Quên mật khẩu</h2>
      
      {error && <div className="error">{error}</div>}
      
      <form onSubmit={handleResetPassword}>
        <div>
          <label>Email:</label>
          <input
            type="email"
            value={formData.email}
            onChange={(e) => setFormData({...formData, email: e.target.value})}
            required
            disabled={otpSent}
          />
        </div>
        
        <div>
          <button
            type="button"
            onClick={handleSendOtp}
            disabled={!formData.email || loading || countdown > 0}
          >
            {countdown > 0 ? `Gửi lại sau ${countdown}s` : 'Gửi mã OTP'}
          </button>
        </div>
        
        {otpSent && (
          <>
            <div>
              <label>Mã OTP:</label>
              <input
                type="text"
                value={formData.otpCode}
                onChange={(e) => setFormData({...formData, otpCode: e.target.value})}
                placeholder="Nhập mã 6 chữ số"
                maxLength={6}
                required
              />
            </div>
            
            <div>
              <label>Mật khẩu mới:</label>
              <input
                type="password"
                value={formData.newPassword}
                onChange={(e) => setFormData({...formData, newPassword: e.target.value})}
                placeholder="Tối thiểu 8 ký tự"
                required
              />
            </div>
            
            <button type="submit" disabled={loading}>
              {loading ? 'Đang xử lý...' : 'Đặt lại mật khẩu'}
            </button>
          </>
        )}
      </form>
    </div>
  );
};

export default ForgotPasswordPage;
```

### 10.4. Xử lý lỗi quên mật khẩu

1. **Email không tồn tại**
   - Status: 1001
   - Message: "người dùng không tồn tại"
   - **Xử lý:** Hiển thị "Email không tồn tại trong hệ thống"

2. **OTP sai/hết hạn**
   - Status: 403
   - Message: "không có quyền truy cập"
   - **Xử lý:** Hiển thị "Mã OTP không hợp lệ, vui lòng gửi lại"

3. **Mật khẩu mới không hợp lệ**
   - Status: 400
   - Message: "Mật khẩu phải tối thiểu 8 kí tự!"
   - **Xử lý:** Hiển thị validation error

### 10.5. Lưu ý

- Sau khi reset password thành công, tất cả token của user sẽ bị thu hồi
- User cần đăng nhập lại với mật khẩu mới
- OTP chỉ có hiệu lực trong 5 phút

## 11. Ví dụ với Axios

```typescript
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:1234/api',
  headers: {
    'Content-Type': 'application/json'
  }
});

// Gửi OTP (đăng ký)
export const sendOtp = async (email: string) => {
  const response = await api.post('/auth/send-otp', { email });
  return response.data;
};

// Verify OTP
export const verifyOtp = async (email: string, otpCode: string) => {
  const response = await api.post('/auth/verify-otp', { email, otpCode });
  return response.data;
};

// Đăng ký
export const register = async (data: RegisterForm) => {
  const response = await api.post('/auth/register', data);
  return response.data;
};

// Gửi OTP quên mật khẩu
export const sendOtpForgotPassword = async (email: string) => {
  const response = await api.post('/auth/forgot-password/send-otp', { email });
  return response.data;
};

// Reset password
export const resetPassword = async (email: string, otpCode: string, newPassword: string) => {
  const response = await api.post('/auth/forgot-password/reset', {
    email,
    otpCode,
    newPassword
  });
  return response.data;
};
```

