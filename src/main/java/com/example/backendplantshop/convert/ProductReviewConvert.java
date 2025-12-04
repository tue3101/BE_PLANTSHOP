package com.example.backendplantshop.convert;

import com.example.backendplantshop.dto.request.ProductReviewDtoRequest;
import com.example.backendplantshop.dto.response.ProductReviewDtoResponse;
import com.example.backendplantshop.entity.ProductReview;
import com.example.backendplantshop.entity.Products;
import com.example.backendplantshop.entity.Users;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class ProductReviewConvert {
    
    // Convert từ ProductReviewDtoRequest sang ProductReview entity
    public static ProductReview convertToProductReview(ProductReviewDtoRequest request, int userId, LocalDateTime now) {
        return ProductReview.builder()
                .product_id(request.getProduct_id())
                .order_detail_id(request.getOrder_detail_id())
                .user_id(userId)
                .rating(request.getRating())
                .comment(request.getComment() != null ? request.getComment() : "")
                .is_deleted(false)
                .created_at(now)
                .updated_at(now)
                .build();
    }
    
    // Convert từ ProductReview entity sang ProductReviewDtoResponse (không có user info)
    public static ProductReviewDtoResponse convertToProductReviewDtoResponse(ProductReview review) {
        return ProductReviewDtoResponse.builder()
                .review_id(review.getReview_id())
                .rating(review.getRating())
                .comment(review.getComment())
                .product_id(review.getProduct_id())
                .order_detail_id(review.getOrder_detail_id())
                .user_id(review.getUser_id())
                .created_at(review.getCreated_at())
                .updated_at(review.getUpdated_at())
                .build();
    }
    
    // Convert từ ProductReview entity sang ProductReviewDtoResponse (có user info)
    public static ProductReviewDtoResponse convertToProductReviewDtoResponseWithUser(ProductReview review, Users user, Products product) {
        ProductReviewDtoResponse response = convertToProductReviewDtoResponse(review);
        if (user != null && product !=null) {
            response.setUser(UserConvert.convertUsersToUserDtoResponse(user));
            response.setProduct(ProductConvert.convertToProductDtoResponse(product));

        }
        return response;
    }
    
    // Convert list ProductReview sang list ProductReviewDtoResponse
    public static List<ProductReviewDtoResponse> convertListToProductReviewDtoResponse(List<ProductReview> reviews) {
        return reviews.stream()
                .map(ProductReviewConvert::convertToProductReviewDtoResponse)
                .collect(Collectors.toList());
    }
    
    // Convert ProductReview entity để update
    public static ProductReview convertToUpdatedProductReview(ProductReview existingReview, ProductReviewDtoRequest request, LocalDateTime now) {
        return ProductReview.builder()
                .review_id(existingReview.getReview_id())
                .product_id(existingReview.getProduct_id())
                .order_detail_id(existingReview.getOrder_detail_id())
                .user_id(existingReview.getUser_id())
                .rating(request.getRating())
                .comment(request.getComment() != null ? request.getComment() : "")
                .is_deleted(existingReview.getIs_deleted())
                .created_at(existingReview.getCreated_at())
                .updated_at(now)
                .build();
    }
}

