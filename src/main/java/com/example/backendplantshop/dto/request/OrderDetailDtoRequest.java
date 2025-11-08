package com.example.backendplantshop.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderDetailDtoRequest {
    @NotNull(message = "Mã sản phẩm không được để trống")
    @Positive(message = "Mã sản phẩm phải lớn hơn 0")
    private Integer product_id;
    
    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    private Integer quantity;
    
    @NotNull(message = "Giá sản phẩm không được để trống")
    @Positive(message = "Giá sản phẩm phải lớn hơn 0")
    private BigDecimal price_at_order; // Giá sản phẩm tại thời điểm đặt hàng (do FE gửi)
    
    @NotNull(message = "Tổng tiền sản phẩm không được để trống")
    @Positive(message = "Tổng tiền sản phẩm phải lớn hơn 0")
    private BigDecimal sub_total; // Tổng tiền của sản phẩm này (do FE gửi)
    
    private String note; // Ghi chú (optional)
}
