package com.example.backendplantshop.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductReview {
    private int review_id;
    private int rating;
    private String comment;
    private int product_id;
    private int user_id;
    private Boolean is_deleted;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;

}
