package com.example.backendplantshop.mapper;

import com.example.backendplantshop.entity.Products;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductMapper {
    public Products findById(@Param("productID") int id);
    public List<Products> getAll();
    public Products findByProductNameAndSize(@Param("productName") String productName, @Param("size") String size, @Param("categoryId") int categoryId);
    public int insert(Products products);
    public int update(Products products);
    public int delete(@Param("productID") int id);

    Products findByIdDeleted(@Param("productID")int id);
    void restoreProduct(@Param("productID")int id);
}
