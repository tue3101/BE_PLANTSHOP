package com.example.backendplantshop.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Products {
    private int product_id;
    private String product_name;
    private String description;
    private String img_url;
    private BigDecimal price;
    private int quantity;
    private String size;
    private boolean out_of_stock;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
    private boolean is_deleted;
    private int category_id;

    @JsonBackReference
    private Category category;

    @JsonBackReference
    private List<CartDetails> cart_details;
}
