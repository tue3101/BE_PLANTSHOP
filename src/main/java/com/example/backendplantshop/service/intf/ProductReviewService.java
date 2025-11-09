package com.example.backendplantshop.service.intf;

import com.example.backendplantshop.dto.request.ProductReviewDtoRequest;
import com.example.backendplantshop.dto.response.ProductReviewDtoResponse;

import java.util.List;

public interface ProductReviewService {
    ProductReviewDtoResponse createReview(ProductReviewDtoRequest request);
    ProductReviewDtoResponse getReviewById(int reviewId);
    List<ProductReviewDtoResponse> getReviewsByProductId(int productId);
    List<ProductReviewDtoResponse> getReviewsByUserId(int userId);
    ProductReviewDtoResponse updateReview(int reviewId, ProductReviewDtoRequest request);
    void deleteReview(int reviewId);
    List<ProductReviewDtoResponse> getAllReviews();
    List<ProductReviewDtoResponse> getAllDeletedReviews(); 
    ProductReviewDtoResponse restoreReview(int reviewId); 
}

