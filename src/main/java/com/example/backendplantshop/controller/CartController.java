package com.example.backendplantshop.controller;

import com.example.backendplantshop.dto.response.ApiResponse;
import com.example.backendplantshop.entity.Carts;
import com.example.backendplantshop.enums.ErrorCode;
import com.example.backendplantshop.service.intf.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    @GetMapping("get-cart-of-user/{userID}")
    ApiResponse<Carts> findCartByUserId(@PathVariable("userID") int userId,
                                       @RequestHeader("Authorization") String authHeader) {
        Carts cart = cartService.findCartByUserId(userId, authHeader);
        return ApiResponse.<Carts>builder()
                .statusCode(ErrorCode.CALL_API_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.CALL_API_SUCCESSFULL.getMessage())
                .data(cart)
                .build();
    }
}
