# Sửa Lỗi 404 - MoMo API Endpoint Sai

## ❌ Lỗi Hiện Tại

```
404 on POST request for "https://test-payment.momo.vn/v2/gateway/api": 
{"status":404,"error":"Not Found","path":"/v2/gateway/api"}
```

**Nguyên nhân:** URL API endpoint bị thiếu phần `/create` ở cuối.

**URL sai:** `https://test-payment.momo.vn/v2/gateway/api`  
**URL đúng:** `https://test-payment.momo.vn/v2/gateway/api/create`

---

## 🔍 Phân Tích

### URL được gọi (SAI):
```
https://test-payment.momo.vn/v2/gateway/api
```

### URL đúng (theo tài liệu MoMo):
```
https://test-payment.momo.vn/v2/gateway/api/create
```

**Thiếu:** `/create` ở cuối URL

---

## ✅ Cách Sửa

### ⚠️ QUAN TRỌNG: Code đã được cập nhật để TỰ ĐỘNG SỬA URL

Code hiện tại sẽ **tự động thêm `/create`** vào cuối URL nếu thiếu. Tuy nhiên, bạn vẫn nên sửa file `.env` để đúng ngay từ đầu.

### 1. Kiểm tra file `.env`

Đảm bảo biến `DEV_MOMO_ENDPOINT` có giá trị đúng:

**❌ SAI:**
```env
DEV_MOMO_ENDPOINT=https://test-payment.momo.vn/v2/gateway/api
```

**✅ ĐÚNG:**
```env
DEV_MOMO_ENDPOINT=https://test-payment.momo.vn/v2/gateway/api/create
```

### 2. Nếu không có trong `.env`

Nếu bạn không set `DEV_MOMO_ENDPOINT` trong `.env`, application.yml sẽ dùng default value:

```yaml
api-endpoint: ${DEV_MOMO_ENDPOINT:https://test-payment.momo.vn/v2/gateway/api/create}
```

Default value đã đúng rồi, nhưng nếu bạn có set `DEV_MOMO_ENDPOINT` trong `.env` với giá trị sai → code sẽ tự động sửa.

---

## 🛠️ Các Bước Sửa

### ⚡ TỰ ĐỘNG SỬA (Đã được thêm vào code)

Code hiện tại sẽ **tự động sửa URL** nếu thiếu `/create`. Bạn chỉ cần **restart ứng dụng** là được!

### Bước 1: Restart ứng dụng (BẮT BUỘC)

Sau khi code đã được cập nhật, restart lại:
```bash
# Dừng (Ctrl+C) và chạy lại
mvn spring-boot:run
```

### Bước 2: Kiểm tra logs

Sau khi restart, khi gọi API, logs sẽ hiển thị:

```
WARN - MoMo API endpoint thiếu /create. Tự động sửa từ: https://test-payment.momo.vn/v2/gateway/api
INFO - URL đã được sửa thành: https://test-payment.momo.vn/v2/gateway/api/create
INFO - MoMo API Endpoint: https://test-payment.momo.vn/v2/gateway/api/create
```

### Bước 3: (Tùy chọn) Sửa file `.env` để đúng ngay từ đầu

Mở file `.env` và kiểm tra:

```env
# Nếu có dòng này, đảm bảo URL đầy đủ:
DEV_MOMO_ENDPOINT=https://test-payment.momo.vn/v2/gateway/api/create
```

**Lưu ý:** Phải có `/create` ở cuối!

Nếu URL thiếu `/create`, sửa thành:

```env
DEV_MOMO_ENDPOINT=https://test-payment.momo.vn/v2/gateway/api/create
```

Hoặc xóa dòng `DEV_MOMO_ENDPOINT` trong `.env` để dùng default từ `application.yml`.

---

## 📋 Checklist

- [ ] File `.env` có `DEV_MOMO_ENDPOINT` với URL đầy đủ: `.../api/create`
- [ ] Hoặc xóa `DEV_MOMO_ENDPOINT` để dùng default
- [ ] Đã restart ứng dụng
- [ ] Kiểm tra logs để xem URL được gọi

---

## 🔍 Kiểm Tra Logs

Sau khi restart, kiểm tra log khi gọi API:

```
INFO - Gọi MoMo API với requestId: xxx, orderId: 1, amount: 10000
```

Nếu vẫn lỗi 404, kiểm tra log để xem URL thực tế được gọi.

---

## ⚠️ Lưu Ý

### 1. **URL phải đầy đủ:**
- ✅ `https://test-payment.momo.vn/v2/gateway/api/create`
- ❌ `https://test-payment.momo.vn/v2/gateway/api` (thiếu `/create`)

### 2. **Theo tài liệu MoMo:**
- Endpoint: `POST /v2/gateway/api/create`
- Base URL: `https://test-payment.momo.vn`
- Full URL: `https://test-payment.momo.vn/v2/gateway/api/create`

### 3. **Test vs Production:**
- Test: `https://test-payment.momo.vn/v2/gateway/api/create`
- Production: `https://payment.momo.vn/v2/gateway/api/create` (khi deploy)

---

## 🎯 Tóm Tắt

**Lỗi 404** xảy ra vì:
- ❌ URL endpoint thiếu `/create` ở cuối
- ❌ `DEV_MOMO_ENDPOINT` trong `.env` có giá trị sai

**Cách sửa:**
1. ✅ Kiểm tra `DEV_MOMO_ENDPOINT` trong `.env`
2. ✅ Đảm bảo URL đầy đủ: `.../api/create`
3. ✅ Hoặc xóa để dùng default
4. ✅ Restart ứng dụng

**Sau khi sửa, lỗi 404 sẽ hết!** ✅

