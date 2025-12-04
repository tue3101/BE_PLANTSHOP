package com.example.backendplantshop.mapper;

import com.example.backendplantshop.entity.Deposit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DepositMapper {
    void insert(Deposit deposit);

    void update(Deposit deposit);

    Deposit findById(@Param("depositID") int depositId);

    Deposit findLatestByOrderId(@Param("orderID") int orderId);

    Deposit findByMomoTransId(@Param("transID") String momoTransId);
}


