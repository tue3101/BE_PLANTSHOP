package com.example.backendplantshop.dto.request.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterDtoRequest {
    @NotBlank(message = "username không được bỏ trống")
    private String username;
    private String email;
    @Size(min = 8, max = 20, message = "Mật khẩu phải tối thiểu 8 kí tự!")
    private String password;



}
