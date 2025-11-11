package com.example.backendplantshop.mapper;

import com.example.backendplantshop.entity.OrderDetails;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderDetailMapper {
    int insert(OrderDetails orderDetail);
    OrderDetails findById(@Param("orderDetailID") int orderDetailId);
    List<OrderDetails> findByOrderId(@Param("orderID") int orderId);
    void update(OrderDetails orderDetail);
    void delete(@Param("orderDetailID") int orderDetailId);
    void deleteByOrderId(@Param("orderID") int orderId);
}

