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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
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
        
        log.info("=== BẮT ĐẦU INSERT DISCOUNT ===");
        log.info("Request: code='{}', name='{}'", discountRequest.getDiscount_code(), discountRequest.getDiscount_name());
        
        // Kiểm tra trùng code 
        Discounts existingByCode = discountMapper.findByDiscountCode(discountRequest.getDiscount_code());
        log.info("Kết quả tìm kiếm theo code: {}", existingByCode != null ? 
            String.format("FOUND - discount_id=%d, is_deleted=%s", existingByCode.getDiscount_id(), existingByCode.getIs_deleted()) : 
            "NOT FOUND");
        
        if (existingByCode != null) {
            // Nếu discount đã tồn tại và chưa bị xóa 
            if (existingByCode.getIs_deleted() == null || !existingByCode.getIs_deleted()) {
                log.warn("Discount code đã tồn tại và chưa bị xóa: discount_id={}", existingByCode.getDiscount_id());
                throw new AppException(ErrorCode.DISCOUNT_ALREADY_EXISTS);
            }
            // Nếu discount đã tồn tại nhưng đã bị xóa mềm 
            if (existingByCode.getIs_deleted()) {
                log.info("Khôi phục discount theo code: discount_id={}", existingByCode.getDiscount_id());
                Discounts discountToUpdate = DiscountConvert.toUpdatedEntity(existingByCode.getDiscount_id(), discountRequest, existingByCode);
                discountToUpdate.setIs_deleted(false);
                discountMapper.update(discountToUpdate);
                log.info("Đã khôi phục và cập nhật discount thành công: discount_id={}", existingByCode.getDiscount_id());
                return;
            }
        }
        
        // Kiểm tra trùng name
        Discounts existingByName = discountMapper.findByDiscountName(discountRequest.getDiscount_name());
        log.info("Kết quả tìm kiếm theo name: {}", existingByName != null ? 
            String.format("FOUND - discount_id=%d, is_deleted=%s", existingByName.getDiscount_id(), existingByName.getIs_deleted()) : 
            "NOT FOUND");
        
        if (existingByName != null) {
            // Nếu discount đã tồn tại và chưa bị xóa 
            if (existingByName.getIs_deleted() == null || !existingByName.getIs_deleted()) {
                log.warn("Discount name đã tồn tại và chưa bị xóa: discount_id={}", existingByName.getDiscount_id());
                throw new AppException(ErrorCode.DISCOUNT_ALREADY_EXISTS);
            }
            // Nếu discount đã tồn tại nhưng đã bị xóa mềm 
            if (existingByName.getIs_deleted()) {
                log.info("Khôi phục discount theo name: discount_id={}", existingByName.getDiscount_id());
                Discounts discountToUpdate = DiscountConvert.toUpdatedEntity(existingByName.getDiscount_id(), discountRequest, existingByName);
                discountToUpdate.setIs_deleted(false);
                discountMapper.update(discountToUpdate);
                log.info("Đã khôi phục và cập nhật discount thành công: discount_id={}", existingByName.getDiscount_id());
                return;
            }
        }
        
        // Nếu không tìm thấy discount nào
        log.info("Không tìm thấy discount trùng, sẽ tạo mới: code='{}', name='{}'", 
            discountRequest.getDiscount_code(), discountRequest.getDiscount_name());
        discountMapper.insert(DiscountConvert.toEntity(discountRequest));
        log.info("Đã tạo discount mới thành công");
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
        
        // Kiểm tra trùng code (loại trừ discount hiện tại)
        Discounts existingByCode = discountMapper.findByDiscountCode(discountRequest.getDiscount_code());
        if (existingByCode != null && existingByCode.getDiscount_id() != id) {
            // Chỉ kiểm tra discount chưa bị xóa
            if (existingByCode.getIs_deleted() == null || !existingByCode.getIs_deleted()) {
                throw new AppException(ErrorCode.DISCOUNT_ALREADY_EXISTS);
            }
        }
        
        // Kiểm tra trùng name (loại trừ discount hiện tại)
        Discounts existingByName = discountMapper.findByDiscountName(discountRequest.getDiscount_name());
        if (existingByName != null && existingByName.getDiscount_id() != id) {
            // Chỉ kiểm tra discount chưa bị xóa
            if (existingByName.getIs_deleted() == null || !existingByName.getIs_deleted()) {
                throw new AppException(ErrorCode.DISCOUNT_ALREADY_EXISTS);
            }
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
    public List<DiscountDtoResponse> getAllDiscountDeleted() {
        var discounts = DiscountConvert.convertListDiscountToListDiscountDtoResponse(discountMapper.getAllDiscountDeleted());
        if (discounts.isEmpty()) {
            throw new AppException(ErrorCode.LIST_NOT_FOUND);
        }
        return discounts;
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
