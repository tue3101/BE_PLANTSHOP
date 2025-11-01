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
public class ChangePasswordDtoRequest {
    @NotBlank(message = "mật khẩu không được bỏ trống")
    @Size(min = 8, max = 20, message = "Mật khẩu phải tối thiểu 8 kí tự!")
    private String oldPassword;
    @NotBlank(message = "vui lòng nhập mật khẩu mới!")
    @Size(min = 8, max = 20, message = "Mật khẩu phải tối thiểu 8 kí tự!")
    private String newPassword;
}
