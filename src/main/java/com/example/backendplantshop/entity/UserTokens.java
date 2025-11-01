package com.example.backendplantshop.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserTokens {
    private int token_id;
    private int user_id;
    private String token;
    private LocalDateTime expires_at;
    private Boolean revoked;
    private LocalDateTime created_at;

    @JsonBackReference
    private Users users;
}
