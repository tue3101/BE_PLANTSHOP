package com.example.backendplantshop.mapper;

import com.example.backendplantshop.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CategoryMapper {
    public List<Category> getAll();
    Category findProductByCategory(@Param("categoryID") int categoryID);

    public Category findById(@Param("categoryID") int id);
    public int insert(Category category);
    public int update(Category category);
    public int delete(@Param("categoryID") int id);
    public Category findByName(@Param("categoryName") String categoryName);

    Category findByIdDeleted(@Param("categoryID")int id);
    void restoreCategory(@Param("categoryID")int id);
    Integer countProductsByCategory(@Param("categoryID") int categoryId);

}
