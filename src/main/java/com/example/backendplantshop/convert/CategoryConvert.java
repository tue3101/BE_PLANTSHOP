package com.example.backendplantshop.convert;

import com.example.backendplantshop.dto.request.CategoryDtoRequest;
import com.example.backendplantshop.dto.response.CategoryDtoResponse;
import com.example.backendplantshop.entity.Category;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


public class CategoryConvert {
    public static CategoryDtoResponse convertToCategoryDtoResponse(Category category) {
        return CategoryDtoResponse.builder()
                .category_id(category.getCategory_id())
                .category_name(category.getCategory_name())
                .updated_at(category.getUpdated_at())
                .created_at(category.getCreated_at())
                .is_deleted(category.getIs_deleted())
                .build();
    }

    public static Category toCategory(CategoryDtoRequest request) {
        return Category.builder()
                .category_name(request.getCategory_name())
                .build();
    }

    public static List<CategoryDtoResponse> convertListCategoryToListCategoryDtoResponse(List<Category> categoryList) {
        return categoryList.stream()
                .map(CategoryConvert::convertToCategoryDtoResponse)
                .collect(Collectors.toList());
    }

    public static Category toUpdatedCategory(int id, CategoryDtoRequest request, Category existingCategory) {
        return Category.builder()
                .category_id(id)
                .category_name(request.getCategory_name())
                .updated_at(LocalDateTime.now()) // Cập nhật ngày sửa
                .created_at(existingCategory.getCreated_at()) // Giữ nguyên ngày tạo
                .is_deleted(existingCategory.getIs_deleted())
                .build();
    }
}
