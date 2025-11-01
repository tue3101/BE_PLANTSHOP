package com.example.backendplantshop.mapper;

import com.example.backendplantshop.entity.Discounts;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DiscountMapper {
    public List<Discounts> getAll();
    public Discounts findById(@Param("discountID") int id);
    public int insert(Discounts discount);
    public int update(Discounts discount);
    public int delete(@Param("discountID") int id);
    public Discounts findByDiscountCodeOrName(@Param("discountCode") String discountCode, @Param("discountName") String discountName);

    Discounts findByIdDeleted(@Param("discountID")int id);
    void restoreDiscount(@Param("discountID")int id);

}
