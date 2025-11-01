package com.example.backendplantshop.service.intf;

import com.example.backendplantshop.entity.Carts;

public interface CartService {
    Carts findCartByUserId(int userId, String authHeader);
}
