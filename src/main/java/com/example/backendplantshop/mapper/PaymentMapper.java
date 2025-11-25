package com.example.backendplantshop.mapper;

import com.example.backendplantshop.entity.Payment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PaymentMapper {
    void insert(Payment payment);
    Payment findById(@Param("paymentID") int paymentId);
    List<Payment> findByOrderId(@Param("orderID") int orderId);
    List<Payment> getAll();
    void update(Payment payment);
//    void delete(@Param("paymentID") int paymentId);
}

