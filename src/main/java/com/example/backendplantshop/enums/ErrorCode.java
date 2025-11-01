package com.example.backendplantshop.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    TOKEN_HAS_EXPIRED(401,"token đã hết hạn, vui lòng đăng nhập lại!"),
    TOKEN_REVOKED(401,"token đã bị vô hiệu hóa"),
    TOKEN_NOT_EXISTS(401,"token không tồn tại!"),
    TOKEN_ALREADY_REVOKED(401,"token đã bị vô hiệu hóa!"),
    AUTHENTICATION_ERROR(401, "lỗi xác thực"),
    ACCESS_DENIED(403, "không có quyền truy cập"),
    CART_IS_EMPTY(1002, "giỏ hàng trống"),
//    ===============================================================
    REGISTER_SUCCESSFULL (202,"đăng ký tài khoản thành công"),
    LOGIN_SUCCESSFULL(202, "dăng nhập thành công"),
    CHANGEPASSWORD_SUCCESSFULL (202, "đổi mật khẩu thành công"),

    REGISTER_FAILED (1000,"đăng ký tài khoản không thành công"),
    LOGIN_FAILED(1000, "dăng nhập không thành công"),
    CHANGEPASSWORD_FAILED (1000, "đổi mật khẩu không thành công"),


    MISSING_REQUIRED_FIELD(1004, "không được để trống"),
    EMAIL_ALREADY_EXISTS(1003, "email đã được đăng ký"),
    PHONE_ALREADY_EXISTS(1003, "số điện thoại đã được đăng ký"),
    USERNAME_ALREADY_EXISTS(1003, "username đã tồn tại"),

    USER_NOT_EXISTS(1002, "user không tồn tại"),
    INVALID_CREDENTIALS(401, "mật khẩu không đúng"),

//    =========================================================================

    CALL_API_SUCCESSFULL(201, "gọi api thành công"),
    ADD_SUCCESSFULL(200, "thêm thành công"),
    UPDATE_SUCCESSFULL(200, "cập nhật thành công"),
    DELETE_SUCCESSFULL(200, "xoá thành công"),
    RESTORE_SUCCESSFULL(200, "khôi phục thành công"),
    NOT_DELETE(409, "chưa bị xóa hoặc không tồn tại"),

//    ======================================================================
// ErrorCode.java

    CATEGORY_HAS_PRODUCTS(1000,"Danh mục vẫn còn sản phẩm, không thể xóa"),

    LIST_NOT_FOUND(1000, "Danh sách rỗng"),
    NAME_EMPTY(1001, "tên không được để trống"),


    CATEGORY_NOT_EXISTS(1002, "danh mục không tồn tại"),
    PRODUCT_NOT_EXISTS(1002, "sản phẩm không tồn tại"),
    LIST_PRODUCT_NOT_EXISTS(1002, "không có sản phẩm nào"),
    DISCOUNT_NOT_EXISTS(1002, "mã khuyến mãi không tồn tại"),

    CATEGORY_ALREADY_EXITST(1003,"danh mục đã tồn tại"),
    PRODUCT_ALREADY_EXISTS(1003, "sản phẩm đã tồn tại"),
    DISCOUNT_ALREADY_EXISTS(1003, "mã khuyến mãi đã tồn tại"),


    QUANTITY_IS_NOT_ENOUGH(1004, "số lượng sản phẩm trong kho không đủ"),
    INVALID_QUANTITY(1004, "số lượng sản phẩm phải >=1"),


//    ========================================================================
    BAD_SQL(500, "Sai syntax SQL"),
    NULL_POINTER(500, "Loi Null Pointer"),
    UNKNOWN_EXCEPTION(500, "Unknown Exception"),
    ;
    private final Integer code;
    private final String message;
}
