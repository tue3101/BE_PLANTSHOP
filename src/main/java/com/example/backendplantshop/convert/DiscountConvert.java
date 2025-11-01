package com.example.backendplantshop.convert;

import com.example.backendplantshop.dto.request.DiscountDtoRequest;
import com.example.backendplantshop.dto.response.DiscountDtoResponse;
import com.example.backendplantshop.entity.Discounts;


import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


public class DiscountConvert {
    public static DiscountDtoResponse convertToDiscountResponse(Discounts discounts) {
        return DiscountDtoResponse.builder()
                .discount_id(discounts.getDiscount_id())
                .discount_code(discounts.getDiscount_code())
                .discount_name(discounts.getDiscount_name())
                .value(discounts.getValue())
                .type(discounts.getType())
                .updated_at(discounts.getUpdated_at())
                .created_at(discounts.getCreated_at())
                .is_deleted(discounts.getIs_deleted())
                .build();
    }

    public static Discounts toEntity(DiscountDtoRequest request) {
        return Discounts.builder()
                .discount_code(request.getDiscount_code())
                .discount_name(request.getDiscount_name())
                .value(request.getValue())
                .type(request.getType())
                .quantity(request.getQuantity())
                .build();
    }

    public static List<DiscountDtoResponse> convertListDiscountToListDiscountDtoResponse(List<Discounts> discountList) {
        return discountList.stream()
                .map(DiscountConvert::convertToDiscountResponse)
                .collect(Collectors.toList());
    }

    public static Discounts toUpdatedEntity(int id, DiscountDtoRequest request, Discounts existingDiscount) {
        return Discounts.builder()
                .discount_id(id)
                .discount_code(request.getDiscount_code())
                .discount_name(request.getDiscount_name())
                .value(request.getValue())
                .type(request.getType())
                .quantity(request.getQuantity())
                .updated_at(LocalDateTime.now()) // Cập nhật ngày sửa
                .created_at(existingDiscount.getCreated_at()) // Giữ nguyên ngày tạo
                .is_deleted(existingDiscount.getIs_deleted())
                .build();
    }
}
