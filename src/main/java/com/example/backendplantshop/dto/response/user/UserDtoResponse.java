package com.example.backendplantshop.dto.response.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDtoResponse {
    private int user_id;
    private String email;
    private String username;
    private String phone_number;
    private String address;
    private String role;
    private Boolean is_deleted;
}
