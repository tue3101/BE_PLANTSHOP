package com.example.backendplantshop.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryDtoRequest {
    private int category_id;
    private String category_name;
    private Boolean is_deleted;

}
