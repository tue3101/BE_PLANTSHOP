package com.example.backendplantshop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductDtoResponse {
    private int product_id;
    private String product_name;
    private String description;
    private String img_url;
    private BigDecimal price;
    private int quantity;
    private String size;
    private Boolean out_of_stock;
    private int category_id;
}
