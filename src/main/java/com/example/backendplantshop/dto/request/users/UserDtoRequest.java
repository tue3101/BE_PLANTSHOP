package com.example.backendplantshop.dto.request.users;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDtoRequest {
    @NotBlank(message = "email không được bỏ trống")
    private String email;
    @NotBlank(message = "username không được bỏ trống")
    private String username;
    private String phone_number;
    private String address;
    private String role;

}
