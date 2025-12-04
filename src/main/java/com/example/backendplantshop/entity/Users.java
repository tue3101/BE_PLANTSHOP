package com.example.backendplantshop.entity;


import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Users {
    private int user_id;
    private String email;
    private String password;
    private String username;
    private String phone_number;
    private String address;
    private String role;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
    private Boolean is_deleted;

    @JsonManagedReference
    private Carts cart;

    @JsonManagedReference
    private List<UserTokens> user_tokens;

}
