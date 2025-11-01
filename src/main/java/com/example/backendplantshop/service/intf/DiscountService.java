package com.example.backendplantshop.service.intf;

import com.example.backendplantshop.dto.request.DiscountDtoRequest;
import com.example.backendplantshop.dto.response.DiscountDtoResponse;

import java.util.List;

public interface DiscountService {
    List<DiscountDtoResponse> getAllDiscounts();
    DiscountDtoResponse getById(int id);
    void insert(DiscountDtoRequest discountRequest);
    void update(int id, DiscountDtoRequest discountRequest);
    void delete(int id);

    void restoreDiscount(int id);

}
