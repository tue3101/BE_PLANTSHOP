package com.example.backendplantshop.mapper;

import com.example.backendplantshop.dto.response.OrderStatisticsDtoResponse;
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
//    void updateShippingInfo(@Param("orderId") int orderId,
//                            @Param("shippingName") String shippingName,
//                            @Param("shippingAddress") String shippingAddress,
//                            @Param("shippingPhone") String shippingPhone);
    void delete(@Param("orderID") int orderId);
    List<Orders> getAll();
    
    // Statistics methods
    OrderStatisticsDtoResponse getStatisticsByDate(@Param("year") int year, @Param("month") int month, @Param("day") int day);
    OrderStatisticsDtoResponse getStatisticsByMonth(@Param("year") int year, @Param("month") int month);
    OrderStatisticsDtoResponse getStatisticsByYear(@Param("year") int year);
}

