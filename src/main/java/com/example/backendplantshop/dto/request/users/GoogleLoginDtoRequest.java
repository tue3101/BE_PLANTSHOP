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
public class GoogleLoginDtoRequest {
    @NotBlank(message = "Google authorization code không được bỏ trống")
    private String code;
    private String redirectUri;
}

