package com.example.backendplantshop.dto.response;

import com.example.backendplantshop.dto.response.user.UserDtoResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductReviewDtoResponse {
    private int review_id;
    private int rating;
    private String comment;
    private int product_id;
    private int order_detail_id;
    private int user_id;
    private UserDtoResponse user; // Thông tin user đầy đủ
    private ProductDtoResponse product;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
}

