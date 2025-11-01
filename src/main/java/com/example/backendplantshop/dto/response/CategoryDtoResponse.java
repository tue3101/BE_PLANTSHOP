package com.example.backendplantshop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryDtoResponse {
    private int category_id;
    private String category_name;
    private LocalDateTime updated_at;
    private LocalDateTime created_at;
    private Boolean is_deleted;
}
