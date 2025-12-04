package com.example.backendplantshop.mapper;

import com.example.backendplantshop.entity.Products;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductMapper {
    Products findById(@Param("productID") int id);
     List<Products> getAll();
     Products findByProductName_Size_Category(@Param("productName") String productName, @Param("size") String size, @Param("categoryId") int categoryId);
     int insert(Products products);
    int update(Products products);
     int delete(@Param("productID") int id);


     List<Products> getAllProductDeleted();
    Products findByIdDeleted(@Param("productID")int id);
    void restoreProduct(@Param("productID")int id);
    void updateProductQuantity(@Param("productID") int productId, @Param("quantity") int quantity);
    void restoreProductQuantity(@Param("productID") int productId, @Param("quantity") int quantity);
}
