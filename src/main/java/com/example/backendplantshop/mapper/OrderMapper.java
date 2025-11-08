package com.example.backendplantshop.mapper;

import com.example.backendplantshop.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderMapper {
    int insert(Orders order);
    Orders findById(@Param("orderID") int orderId);
    List<Orders> findByUserId(@Param("userID") int userId);
    void update(Orders order);
    void delete(@Param("orderID") int orderId);
    List<Orders> getAll();
}

