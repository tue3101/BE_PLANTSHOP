package com.example.backendplantshop.service.impl;

import com.example.backendplantshop.convert.DiscountConvert;
import com.example.backendplantshop.dto.request.DiscountDtoRequest;
import com.example.backendplantshop.dto.response.DiscountDtoResponse;
import com.example.backendplantshop.entity.Discounts;
import com.example.backendplantshop.enums.ErrorCode;
import com.example.backendplantshop.exception.AppException;
import com.example.backendplantshop.mapper.DiscountMapper;
import com.example.backendplantshop.service.intf.DiscountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DiscountServiceImpl implements DiscountService {
    private final DiscountMapper discountMapper;
    private final AuthServiceImpl authService;

    public List<DiscountDtoResponse> getAllDiscounts() {
        var discounts = DiscountConvert.convertListDiscountToListDiscountDtoResponse(discountMapper.getAll());
        if (discounts.isEmpty()) {
            throw new AppException(ErrorCode.LIST_NOT_FOUND);
        }
        return discounts;
    }

    public DiscountDtoResponse getById(int id) {
        var discounts = discountMapper.findById(id);
        if (discounts == null) {
            throw new AppException(ErrorCode.DISCOUNT_NOT_EXISTS);
        }
        return DiscountConvert.convertToDiscountResponse(discounts);
    }

    public void insert(DiscountDtoRequest discountRequest) {
        String role = authService.getCurrentRole();
        if (!authService.isAdmin(role) ) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        Discounts existingDiscount = discountMapper.findByDiscountCodeOrName(
                discountRequest.getDiscount_code(),
                discountRequest.getDiscount_name()
        );
        if (existingDiscount != null) {
            throw new AppException(ErrorCode.DISCOUNT_ALREADY_EXISTS);
        }

        discountMapper.insert(DiscountConvert.toEntity(discountRequest));

    }

    public void update(int id, DiscountDtoRequest discountRequest) {
        String role = authService.getCurrentRole();
        if (!authService.isAdmin(role) ) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        Discounts existingDiscount = discountMapper.findById(id);
        if (existingDiscount == null) {
            throw new AppException(ErrorCode.DISCOUNT_NOT_EXISTS);
        }
        Discounts existingCodeOrName = discountMapper.findByDiscountCodeOrName(
                discountRequest.getDiscount_code(),
                discountRequest.getDiscount_name()
        );
        if (existingCodeOrName != null && existingCodeOrName.getDiscount_id() != id) {
            throw new AppException(ErrorCode.DISCOUNT_ALREADY_EXISTS);
        }


        discountMapper.update(DiscountConvert.toUpdatedEntity(id, discountRequest, existingDiscount));

    }

    public void delete(int id) {
        String role = authService.getCurrentRole();
        if (!authService.isAdmin(role) ) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        if (discountMapper.findById(id) == null) {
            throw new AppException(ErrorCode.DISCOUNT_NOT_EXISTS);
        }
        discountMapper.delete(id);
    }


    @Override
    public void restoreDiscount(int id) {
        String role = authService.getCurrentRole();
        if (!authService.isAdmin(role) ) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        if(discountMapper.findByIdDeleted(id) == null){
            throw new AppException(ErrorCode.NOT_DELETE);
        }
        discountMapper.restoreDiscount(id);
    }
}
