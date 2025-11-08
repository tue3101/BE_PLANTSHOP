package com.example.backendplantshop.dto.request;

import com.example.backendplantshop.enums.PaymentStatus;
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
public class PaymentDtoRequest {
    private Integer method_id;
    
    @NotNull(message = "Số tiền thanh toán không được để trống")
    @Positive(message = "Số tiền thanh toán phải lớn hơn 0")
    private BigDecimal amount;
    
    @NotNull(message = "Trạng thái thanh toán không được để trống")
    private PaymentStatus status;
}

