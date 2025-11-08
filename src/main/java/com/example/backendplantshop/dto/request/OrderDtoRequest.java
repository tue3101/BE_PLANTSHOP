package com.example.backendplantshop.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderDtoRequest {
    private Integer discount_id; // Optional, có thể null nếu không dùng discount
    private String discount_code; // Optional, có thể dùng code thay vì id
    
    @NotNull(message = "Tổng tiền không được để trống")
    private BigDecimal total; // Tổng tiền (do FE gửi)
    
    private BigDecimal discount_amount; // Số tiền giảm giá (do FE gửi), có thể null
    
    @NotNull(message = "Tổng tiền cuối cùng không được để trống")
    private BigDecimal final_total; // Tổng tiền cuối cùng sau khi giảm giá (do FE gửi)
    
    @NotEmpty(message = "Danh sách sản phẩm không được để trống")
    @Valid
    private List<OrderDetailDtoRequest> items; // Danh sách sản phẩm muốn đặt hàng
    
    // Thông tin thanh toán (do FE gửi)
    @Valid
    private PaymentDtoRequest payment; // Thông tin thanh toán (method_id, amount, status)
}

