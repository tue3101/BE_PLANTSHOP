package com.example.backendplantshop.service.intf;

import com.example.backendplantshop.dto.request.CategoryDtoRequest;
import com.example.backendplantshop.dto.response.CategoryDtoResponse;
import com.example.backendplantshop.entity.Category;

import java.util.List;

public interface CategoryService {
    List<CategoryDtoResponse> getAllCategory();
    CategoryDtoResponse findById(int id);
    Category findProductByCategory(int id);
    void insert(CategoryDtoRequest categoryRequest);
    void update(int id, CategoryDtoRequest categoryRequest);
    void delete(int id);

    void restoreCategory(int id);

}
