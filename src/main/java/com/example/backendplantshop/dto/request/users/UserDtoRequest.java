package com.example.backendplantshop.dto.request.users;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDtoRequest {
    private String email;
    private String username;
    private String phone_number;
    private String address;
    private String role;

}
