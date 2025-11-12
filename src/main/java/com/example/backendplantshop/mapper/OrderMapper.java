package com.example.backendplantshop.mapper;

import com.example.backendplantshop.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {
    int insert(Orders order);
    Orders findById(@Param("orderID") int orderId);
    List<Orders> findByUserId(@Param("userID") int userId);
    void update(Orders order);
    void delete(@Param("orderID") int orderId);
    List<Orders> getAll();
    
    // Statistics methods
    Map<String, Object> getStatisticsByDate(@Param("year") int year, @Param("month") int month, @Param("day") int day);
    Map<String, Object> getStatisticsByMonth(@Param("year") int year, @Param("month") int month);
    Map<String, Object> getStatisticsByYear(@Param("year") int year);
}

