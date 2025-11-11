package com.example.backendplantshop.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmailOtp {
    private int otp_id;
    private Integer user_id; 
    private String email;
    private String otp_code;
    private LocalDateTime created_at;
    private LocalDateTime expires_at;
    private Boolean is_deleted;
}

