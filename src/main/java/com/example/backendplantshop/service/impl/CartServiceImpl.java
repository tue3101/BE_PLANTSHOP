package com.example.backendplantshop.service.impl;

import com.example.backendplantshop.entity.Carts;
import com.example.backendplantshop.enums.ErrorCode;
import com.example.backendplantshop.exception.AppException;
import com.example.backendplantshop.mapper.CartMapper;
import com.example.backendplantshop.security.JwtUtil;
import com.example.backendplantshop.service.intf.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    private final CartMapper cartMapper;
    private final AuthServiceImpl authService;
    @Override
    public Carts findCartByUserId(int userId, String authHeader) {
        int currentUserId = authService.getCurrentUserId();
        String role = authService.getCurrentRole();
        if (!authService.isUser(role) || currentUserId != userId) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        // Tìm giỏ hàng của user
        Carts cart = cartMapper.findCartByUserId(userId);
        if (cart == null) {
            throw new AppException(ErrorCode.CART_IS_EMPTY);
        }

        return cart;
    }
}
