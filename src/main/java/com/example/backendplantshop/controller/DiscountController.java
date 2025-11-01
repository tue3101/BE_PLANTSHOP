package com.example.backendplantshop.controller;

import com.example.backendplantshop.dto.request.DiscountDtoRequest;
import com.example.backendplantshop.dto.response.ApiResponse;
import com.example.backendplantshop.dto.response.DiscountDtoResponse;
import com.example.backendplantshop.enums.ErrorCode;
import com.example.backendplantshop.service.intf.DiscountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/discount")
@RequiredArgsConstructor
public class DiscountController {
    @Autowired
    private final DiscountService discountService;

    @GetMapping("getbyid/{id}")
    public ApiResponse<DiscountDtoResponse> doGetById(@PathVariable("id") int id) {
        return ApiResponse.<DiscountDtoResponse>builder()
                .statusCode(ErrorCode.CALL_API_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.CALL_API_SUCCESSFULL.getMessage())
                .data(discountService.getById(id))
                .build();

    }

    @GetMapping("/getall")
    public ApiResponse<List<DiscountDtoResponse>> doGetAllDiscounts() {
        return ApiResponse.<List<DiscountDtoResponse>>builder()
                .statusCode(ErrorCode.CALL_API_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.CALL_API_SUCCESSFULL.getMessage())
                .data(discountService.getAllDiscounts())
                .build();


    }

    @PostMapping("/add")
    public ApiResponse<Void> doInsertDiscount(@Valid @RequestBody DiscountDtoRequest discountRequest) {

        discountService.insert(discountRequest);
        return ApiResponse.<Void>builder()
                .statusCode(ErrorCode.ADD_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.ADD_SUCCESSFULL.getMessage())
                .build();

    }

    @PutMapping("/update/{id}")
    public ApiResponse<Void> doUpdateDiscount( @PathVariable("id") int id,@Valid @RequestBody DiscountDtoRequest discountRequest) {

        discountService.update(id, discountRequest);
        return ApiResponse.<Void>builder()
                .statusCode(ErrorCode.UPDATE_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.UPDATE_SUCCESSFULL.getMessage())
                .build();

    }

    @DeleteMapping("/delete/{id}")
    public ApiResponse<Void> doDeleteDiscount(@PathVariable("id") int id) {
        discountService.delete(id);
        return ApiResponse.<Void>builder()
                .statusCode(ErrorCode.DELETE_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.DELETE_SUCCESSFULL.getMessage())
                .build();
    }

    @PutMapping("/restore/{id}")
    ApiResponse<Void> restore(@PathVariable("id") int id) {
        discountService.restoreDiscount(id);
        return ApiResponse.<Void>builder()
                .statusCode(ErrorCode.RESTORE_SUCCESSFULL.getCode())
                .success(Boolean.TRUE)
                .message(ErrorCode.RESTORE_SUCCESSFULL.getMessage())
                .build();
    }
}
