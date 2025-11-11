package com.example.backendplantshop.dto.request;

import com.example.backendplantshop.enums.ShippingStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateShippingStatusDtoRequest {
    @NotNull(message = "Trạng thái vận chuyển không được để trống")
    private ShippingStatus shipping_status;
}

