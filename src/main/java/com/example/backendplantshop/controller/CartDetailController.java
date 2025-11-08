package com.example.backendplantshop.controller;

import com.example.backendplantshop.dto.response.ApiResponse;
import com.example.backendplantshop.entity.CartDetails;
import com.example.backendplantshop.enums.ErrorCode;
import com.example.backendplantshop.service.intf.CartDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cartdetail")
@RequiredArgsConstructor
public class CartDetailController {
    private final CartDetailService cartDetailService;

    // Lấy chi tiết giỏ hàng theo cartId
    @GetMapping("/get-cart-detail/{cartId}")
    public ApiResponse<List<CartDetails>> getCartDetailsByCartId(@PathVariable("cartId") int cartId) {
        List<CartDetails> cartDetails = cartDetailService.getCartDetailsByCartId(cartId);
        return ApiResponse.<List<CartDetails>>builder()
                .statusCode(ErrorCode.CALL_API_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.CALL_API_SUCCESSFULL.getMessage())
                .data(cartDetails)
                .build();
    }

    // Thêm sản phẩm vào giỏ hàng
    @PostMapping("/add-product/{userId}/{productId}/{quantity}")
    public ApiResponse<Void> addProductToCart(@PathVariable("userId") int userId,
                                              @PathVariable("productId") int productId,
                                              @PathVariable("quantity") int quantity

    ) {
        cartDetailService.addProductToCart(userId, productId, quantity);
        return ApiResponse.<Void>builder()
                .statusCode(ErrorCode.CALL_API_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.ADD_SUCCESSFULL.getMessage())
                .build();
    }

    // Cập nhật số lượng trực tiếp
    @PutMapping("/update-quantity/{userId}/{productId}/{quantity}")
    public ApiResponse<Void> updateQuantity(@PathVariable("userId") int userId,
                       @PathVariable("productId") int productId,
                       @PathVariable("quantity") int quantity,
                        @RequestParam(value = "selected", required = false) Boolean selected,
                       @RequestHeader("Authorization") String authHeader) {
    cartDetailService.updateQuantity(userId, productId, quantity, selected, authHeader);
    return ApiResponse.<Void>builder()
        .statusCode(ErrorCode.UPDATE_SUCCESSFULL.getCode())
        .success(Boolean.TRUE)
        .message(ErrorCode.UPDATE_SUCCESSFULL.getMessage())
        .build();
    }

    // Tăng số lượng lên 1
//    @PutMapping("/increase-quantity/{userId}/{productId}")
//    public ApiResponse<Void> increaseQuantity(@PathVariable("userId") int userId,
//                          @PathVariable("productId") int productId,
//                          @RequestHeader("Authorization") String authHeader) {
//    cartDetailService.increaseQuantity(userId, productId, authHeader);
//    return ApiResponse.<Void>builder()
//        .statusCode(ErrorCode.UPDATE_SUCCESSFULL.getCode())
//        .success(Boolean.TRUE)
//        .message("Tăng số lượng thành công")
//        .build();
//    }
//
//    // Giảm số lượng xuống 1
//    @PutMapping("/decrease-quantity/{userId}/{productId}")
//    public ApiResponse<Void> decreaseQuantity(@PathVariable("userId") int userId,
//                          @PathVariable("productId") int productId,
//                          @RequestHeader("Authorization") String authHeader) {
//    cartDetailService.decreaseQuantity(userId, productId, authHeader);
//    return ApiResponse.<Void>builder()
//        .statusCode(ErrorCode.UPDATE_SUCCESSFULL.getCode())
//        .success(Boolean.TRUE)
//        .message("Giảm số lượng thành công")
//        .build();
//    }

    
    // Xóa sản phẩm khỏi giỏ hàng (soft delete)
    @DeleteMapping("/remove-product/{userId}/{productId}")
    public ApiResponse<Void> removeProductFromCart(@PathVariable("userId") int userId,
                                                   @PathVariable("productId") int productId,
                                                   @RequestHeader("Authorization") String authHeader) {
        cartDetailService.removeProductFromCart(userId, productId, authHeader);
        return ApiResponse.<Void>builder()
                .statusCode(ErrorCode.DELETE_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message("Xóa sản phẩm khỏi giỏ hàng thành công")
                .build();
    }
}
