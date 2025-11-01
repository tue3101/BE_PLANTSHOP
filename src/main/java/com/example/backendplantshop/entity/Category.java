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
public class Category {
    private int category_id;
    private String category_name;
    private LocalDateTime updated_at;
    private LocalDateTime created_at;
    private Boolean is_deleted;

    @JsonManagedReference
    private List<Products>  products;


}
