package com.example.backendplantshop.service.impl;

import com.example.backendplantshop.convert.CategoryConvert;
import com.example.backendplantshop.dto.request.CategoryDtoRequest;
import com.example.backendplantshop.dto.response.CategoryDtoResponse;
import com.example.backendplantshop.entity.Category;
import com.example.backendplantshop.enums.ErrorCode;
import com.example.backendplantshop.exception.AppException;
import com.example.backendplantshop.mapper.CategoryMapper;
import com.example.backendplantshop.service.intf.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;
    private final AuthServiceImpl authService;

    public List<CategoryDtoResponse> getAllCategory(){
        var category = CategoryConvert.convertListCategoryToListCategoryDtoResponse(categoryMapper.getAll());
        if(category.isEmpty()){
            throw new AppException(ErrorCode.LIST_NOT_FOUND);
        }
        return category;
    }

    public CategoryDtoResponse findById(int id){
        var category = categoryMapper.findById(id);
        if(category == null){
            throw new AppException(ErrorCode.CATEGORY_NOT_EXISTS);
        }
        return CategoryConvert.convertToCategoryDtoResponse(category);
    }

    @Override
    public Category findProductByCategory(int id) {
        var categoryEntity = categoryMapper.findById(id);
        if(categoryEntity == null){
            throw new AppException(ErrorCode.CATEGORY_NOT_EXISTS);
        }

        Category category = categoryMapper.findProductByCategory(id);
        
        // Lọc bỏ các sản phẩm có product_id = 0 (sản phẩm null từ LEFT JOIN)
        if (category != null && category.getProducts() != null) {
            category.getProducts().removeIf(product -> product.getProduct_id() == 0);
        }
        
        // Kiểm tra danh mục có sản phẩm nào không
        if (category.getProducts() == null || category.getProducts().isEmpty()) {
            throw new AppException(ErrorCode.LIST_PRODUCT_NOT_EXISTS);
        }
        
        return category;
    }

    public void insert(CategoryDtoRequest categoryRequest) {
        String role = authService.getCurrentRole();
        if (!authService.isAdmin(role) ) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        // Validate input
        if (categoryRequest.getCategory_name() == null || categoryRequest.getCategory_name().trim().isEmpty()) {
            throw new AppException(ErrorCode.NAME_EMPTY);
        }
        
        Category existingCategory = categoryMapper.findByName(categoryRequest.getCategory_name().trim());
        if (existingCategory != null) {
            throw new AppException(ErrorCode.CATEGORY_ALREADY_EXITST);
        }
        
        categoryMapper.insert(CategoryConvert.toCategory(categoryRequest));
    }

    public void update(int id, CategoryDtoRequest categoryRequest) {
        String role = authService.getCurrentRole();
        if (!authService.isAdmin(role) ) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        // Kiểm tra category cần update có tồn tại không
        Category existingCategory = categoryMapper.findById(id);
        if (existingCategory == null) {
            throw new AppException(ErrorCode.CATEGORY_NOT_EXISTS);
        }
        
        // Kiểm tra tên mới có trùng với category khác không (loại trừ category hiện tại)
        Category duplicateCategory = categoryMapper.findByName(categoryRequest.getCategory_name());
        if (duplicateCategory != null && duplicateCategory.getCategory_id() != id) {
            throw new AppException(ErrorCode.CATEGORY_ALREADY_EXITST);
        }
        
        // Nếu tên không thay đổi hoặc tên mới không trùng với category khác
        categoryMapper.update(CategoryConvert.toUpdatedCategory(id, categoryRequest, existingCategory));
    }

    public void delete(int id){
        String role = authService.getCurrentRole();
        if (!authService.isAdmin(role) ) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        if (categoryMapper.findById(id) == null) {
            throw new AppException(ErrorCode.CATEGORY_NOT_EXISTS);
        }
        Integer productCount = categoryMapper.countProductsByCategory(id);
        if(productCount!=null&& productCount>0){
            throw new AppException(ErrorCode.CATEGORY_HAS_PRODUCTS);
        }
        categoryMapper.delete(id);
    }

    @Override
    public void restoreCategory(int id) {
        String role = authService.getCurrentRole();
        if (!authService.isAdmin(role) ) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        if(categoryMapper.findByIdDeleted(id) == null){
            throw new AppException(ErrorCode.NOT_DELETE);
        }
        categoryMapper.restoreCategory(id);
    }
}
