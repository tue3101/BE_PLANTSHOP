package com.example.backendplantshop.dto.request;
import com.example.backendplantshop.enums.DiscountType;
import com.example.backendplantshop.enums.ErrorCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DiscountDtoRequest {
    private int discount_id;
    @NotBlank(message = "mã ko được trống")
    private String discount_code;
    @NotBlank(message = "tên ko được trống")
    private String discount_name;
    @NotNull(message = "giá trị ko được trống")
    private BigDecimal value;
    private DiscountType type;
    @NotNull(message = "số lượng ko được trống")
    private Integer quantity;
    private boolean is_deleted;
}
