package com.example.backendplantshop.mapper;

import com.example.backendplantshop.entity.Carts;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CartMapper {
    Carts findCartByUserId(@Param("userID") int userId);
    Integer findCartIdByUserId(@Param("userID") int userId);

    void deleteByUserId(@Param("userID") int userId);

    void createCartForUser(@Param("userID") int userId);
    void restoreByUserId(@Param("userID") int userId);

}
