package com.example.backendplantshop.controller;

import com.example.backendplantshop.dto.request.ProductReviewDtoRequest;
import com.example.backendplantshop.dto.response.ApiResponse;
import com.example.backendplantshop.dto.response.ProductReviewDtoResponse;
import com.example.backendplantshop.enums.ErrorCode;
import com.example.backendplantshop.service.intf.ProductReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ProductReviewController {
    
    private final ProductReviewService productReviewService;
    
    /**
     * Tạo review mới cho sản phẩm
     */
    @PostMapping("/add")
    public ApiResponse<ProductReviewDtoResponse> createReview(@Valid @RequestBody ProductReviewDtoRequest request) {
        ProductReviewDtoResponse review = productReviewService.createReview(request);
        return ApiResponse.<ProductReviewDtoResponse>builder()
                .statusCode(ErrorCode.ADD_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.ADD_SUCCESSFULL.getMessage())
                .data(review)
                .build();
    }
    
    /**
     * Lấy review theo ID
     */
    @GetMapping("/{reviewId}")
    public ApiResponse<ProductReviewDtoResponse> getReviewById(@PathVariable("reviewId") int reviewId) {
        ProductReviewDtoResponse review = productReviewService.getReviewById(reviewId);
        return ApiResponse.<ProductReviewDtoResponse>builder()
                .statusCode(ErrorCode.CALL_API_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.CALL_API_SUCCESSFULL.getMessage())
                .data(review)
                .build();
    }
    
    /**
     * Lấy tất cả review của một sản phẩm
     */
    @GetMapping("/product/{productId}")
    public ApiResponse<List<ProductReviewDtoResponse>> getReviewsByProductId(@PathVariable("productId") int productId) {
        List<ProductReviewDtoResponse> reviews = productReviewService.getReviewsByProductId(productId);
        return ApiResponse.<List<ProductReviewDtoResponse>>builder()
                .statusCode(ErrorCode.CALL_API_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.CALL_API_SUCCESSFULL.getMessage())
                .data(reviews)
                .build();
    }
    
    /**
     * Lấy tất cả review của một user
     */
    @GetMapping("/user/{userId}")
    public ApiResponse<List<ProductReviewDtoResponse>> getReviewsByUserId(@PathVariable("userId") int userId) {
        List<ProductReviewDtoResponse> reviews = productReviewService.getReviewsByUserId(userId);
        return ApiResponse.<List<ProductReviewDtoResponse>>builder()
                .statusCode(ErrorCode.CALL_API_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.CALL_API_SUCCESSFULL.getMessage())
                .data(reviews)
                .build();
    }
    
    /**
     * Cập nhật review (chỉ user tạo review hoặc admin mới được)
     */
    @PutMapping("/{reviewId}")
    public ApiResponse<ProductReviewDtoResponse> updateReview(
            @PathVariable("reviewId") int reviewId,
            @Valid @RequestBody ProductReviewDtoRequest request) {
        ProductReviewDtoResponse review = productReviewService.updateReview(reviewId, request);
        return ApiResponse.<ProductReviewDtoResponse>builder()
                .statusCode(ErrorCode.UPDATE_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.UPDATE_SUCCESSFULL.getMessage())
                .data(review)
                .build();
    }
    
    /**
     * Xóa review (soft delete, chỉ user tạo review hoặc admin mới được)
     */
    @DeleteMapping("/{reviewId}")
    public ApiResponse<Void> deleteReview(@PathVariable("reviewId") int reviewId) {
        productReviewService.deleteReview(reviewId);
        return ApiResponse.<Void>builder()
                .statusCode(ErrorCode.DELETE_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.DELETE_SUCCESSFULL.getMessage())
                .build();
    }
    
    /**
     * Lấy tất cả review (chỉ admin)
     */
    @GetMapping("/get-all")
    public ApiResponse<List<ProductReviewDtoResponse>> getAllReviews() {
        List<ProductReviewDtoResponse> reviews = productReviewService.getAllReviews();
        return ApiResponse.<List<ProductReviewDtoResponse>>builder()
                .statusCode(ErrorCode.CALL_API_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.CALL_API_SUCCESSFULL.getMessage())
                .data(reviews)
                .build();
    }
    
    /**
     * Lấy tất cả review đã bị xóa (is_deleted = 1) - chỉ admin
     */
    @GetMapping("/get-all-deleted")
    public ApiResponse<List<ProductReviewDtoResponse>> getAllDeletedReviews() {
        List<ProductReviewDtoResponse> reviews = productReviewService.getAllDeletedReviews();
        return ApiResponse.<List<ProductReviewDtoResponse>>builder()
                .statusCode(ErrorCode.CALL_API_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.CALL_API_SUCCESSFULL.getMessage())
                .data(reviews)
                .build();
    }
    
    /**
     * Khôi phục review đã bị xóa (chỉ admin)
     */
    @PutMapping("/restore/{reviewId}")
    public ApiResponse<ProductReviewDtoResponse> restoreReview(@PathVariable("reviewId") int reviewId) {
        ProductReviewDtoResponse review = productReviewService.restoreReview(reviewId);
        return ApiResponse.<ProductReviewDtoResponse>builder()
                .statusCode(ErrorCode.RESTORE_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.RESTORE_SUCCESSFULL.getMessage())
                .data(review)
                .build();
    }
}

