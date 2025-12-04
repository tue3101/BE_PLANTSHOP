package com.example.backendplantshop.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductReviewDtoRequest {
    @NotNull(message = "Sản phẩm không được để trống")
    private Integer product_id;
    
    @NotNull(message = "Chi tiết đơn hàng không được để trống")
    private Integer order_detail_id;

    @NotNull(message = "Đánh giá không được để trống")
    @Min(value = 1, message = "Đánh giá phải từ 1 đến 5 sao")
    @Max(value = 5, message = "Đánh giá phải từ 1 đến 5 sao")
    private Integer rating;
    
    private String comment; // Comment có thể để trống
}

