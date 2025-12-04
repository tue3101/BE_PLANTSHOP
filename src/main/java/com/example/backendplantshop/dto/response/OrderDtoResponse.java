package com.example.backendplantshop.dto.response;

import com.example.backendplantshop.dto.response.user.UserDtoResponse;
import com.example.backendplantshop.dto.response.DepositDtoResponse;
import com.example.backendplantshop.dto.response.momo.CreatePaymentResponse;
import com.example.backendplantshop.enums.OrderSatus;
import com.example.backendplantshop.enums.ShippingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderDtoResponse {
    private int order_id;
    private BigDecimal total;
    private BigDecimal discount_amount;
    private BigDecimal final_total;
    private LocalDateTime order_date;
    private OrderSatus status;
    private ShippingStatus shipping_status;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
    private int user_id;
    private UserDtoResponse user; 
    private Integer discount_id;
    private String shipping_name;
    private String shipping_address;
    private String shipping_phone;
    private List<OrderDetailDtoResponse> order_details;
    private Boolean deposit_required;
    private DepositDtoResponse deposit;
    private CreatePaymentResponse deposit_payment;
}

