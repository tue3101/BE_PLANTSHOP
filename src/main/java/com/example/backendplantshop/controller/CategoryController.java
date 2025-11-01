package com.example.backendplantshop.controller;

import com.example.backendplantshop.dto.request.CategoryDtoRequest;
import com.example.backendplantshop.dto.response.ApiResponse;
import com.example.backendplantshop.dto.response.CategoryDtoResponse;
import com.example.backendplantshop.entity.Category;
import com.example.backendplantshop.enums.ErrorCode;
import com.example.backendplantshop.service.intf.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/category")
@RequiredArgsConstructor
public class CategoryController {
    @Autowired
    private final CategoryService categoryService;

    @GetMapping("/getall")
    public ApiResponse<List<CategoryDtoResponse>> doGetAllCategory() {
        return ApiResponse.<List<CategoryDtoResponse>>builder()
                .statusCode(ErrorCode.CALL_API_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.CALL_API_SUCCESSFULL.getMessage())
                .data(categoryService.getAllCategory())
                .build();
    }

    @GetMapping("/get-product-by-category/{id}")
    ApiResponse<Category> getProductByCategory(@PathVariable("id") int id) {
        return ApiResponse.<Category>builder()
                .statusCode(ErrorCode.CALL_API_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.CALL_API_SUCCESSFULL.getMessage())
                .data(categoryService.findProductByCategory(id))
                .build();
    }
    @PostMapping("/add")
    public ApiResponse<Void> doInsertCategory(@RequestBody CategoryDtoRequest categoryRequest) {

        categoryService.insert(categoryRequest);
        return ApiResponse.<Void>builder()
                .statusCode(ErrorCode.ADD_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.ADD_SUCCESSFULL.getMessage())
                .build();

    }

    @PutMapping("/update/{id}")
    public ApiResponse<Void> doUpdateCategory(@PathVariable("id") int id, @RequestBody CategoryDtoRequest categoryRequest) {

        categoryService.update(id, categoryRequest);
        return ApiResponse.<Void>builder()
                .statusCode(ErrorCode.UPDATE_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.UPDATE_SUCCESSFULL.getMessage())
                .build();

    }

    @DeleteMapping("/delete/{id}")
    public ApiResponse<Void> doDeleteCategory(@PathVariable("id") int id) {
        categoryService.delete(id);
        return ApiResponse.<Void>builder()
                .statusCode(ErrorCode.DELETE_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.DELETE_SUCCESSFULL.getMessage())
                .build();
    }

    @PutMapping("/restore/{id}")
    ApiResponse<Void> restore(@PathVariable("id") int id) {
        categoryService.restoreCategory(id);
        return ApiResponse.<Void>builder()
                .statusCode(ErrorCode.RESTORE_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.RESTORE_SUCCESSFULL.getMessage())
                .build();
    }

}
