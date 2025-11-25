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
    private Integer discount_id;
    private String discount_code;
    
    @NotNull(message = "Tổng tiền không được để trống")
    private BigDecimal total;
    
    private BigDecimal discount_amount;
    
    @NotNull(message = "Tổng tiền cuối cùng không được để trống")
    private BigDecimal final_total;
    
    @NotEmpty(message = "Danh sách sản phẩm không được để trống")
    @Valid
    private List<OrderDetailDtoRequest> items;
    
    // Thông tin giao hàng
    @NotNull(message = "Tên người nhận không được để trống")
    private String shipping_name;
    
    @NotNull(message = "Địa chỉ giao hàng không được để trống")
    private String shipping_address;
    
    @NotNull(message = "Số điện thoại người nhận không được để trống")
    private String shipping_phone;
    
    // Thông tin thanh toán (do FE gửi)
    @Valid
    private PaymentDtoRequest payment;
}

