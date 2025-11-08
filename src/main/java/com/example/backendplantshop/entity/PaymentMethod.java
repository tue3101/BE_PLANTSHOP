package com.example.backendplantshop.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentMethod {
    private int method_id;
    private String method_name;
    private Boolean is_deleted;
}
