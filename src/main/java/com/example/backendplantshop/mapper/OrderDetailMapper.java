package com.example.backendplantshop.mapper;

import com.example.backendplantshop.entity.OrderDetails;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface OrderDetailMapper {
    int insert(OrderDetails orderDetail);
    OrderDetails findById(@Param("orderDetailID") int orderDetailId);
    List<OrderDetails> findByOrderId(@Param("orderID") int orderId);
    void update(OrderDetails orderDetail);
    void delete(@Param("orderDetailID") int orderDetailId);
    void deleteByOrderId(@Param("orderID") int orderId);
    
    // Statistics methods - Top products
//    List<Map<String, Object>> getTopProductsByDate(@Param("year") int year, @Param("month") int month, @Param("day") int day, @Param("limit") int limit);
//    List<Map<String, Object>> getTopProductsByMonth(@Param("year") int year, @Param("month") int month, @Param("limit") int limit);
//    List<Map<String, Object>> getTopProductsByYear(@Param("year") int year, @Param("limit") int limit);

    int countActiveOrderDetailsByProductId(@Param("productID") int productId);
    
    // Statistics methods - Total products sold
    Integer getTotalProductsSoldByMonth(@Param("year") int year, @Param("month") int month);
    Integer getTotalProductsSoldByYear(@Param("year") int year);
}

