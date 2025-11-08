package com.example.backendplantshop.dto.request;

import com.example.backendplantshop.enums.OrderSatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateOrderStatusDtoRequest {
    @NotNull(message = "Trạng thái đơn hàng không được để trống")
    private OrderSatus status;
}

