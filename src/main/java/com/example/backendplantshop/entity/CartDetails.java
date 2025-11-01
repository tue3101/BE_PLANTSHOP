package com.example.backendplantshop.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CartDetails {
    private int cart_detail_id;
    private int product_id;
    private int quantity;
    private Boolean selected;
    private LocalDateTime updated_at;
    private LocalDateTime created_at;
    private Boolean is_deleted;
    private int cart_id;

    @JsonManagedReference
    private Products products;

    @JsonBackReference
    private Carts cart;
}
